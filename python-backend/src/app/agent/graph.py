import logging
from collections.abc import AsyncIterator
from typing import Any

from langgraph.graph import END, StateGraph
from langgraph.types import Command

from src.app.agent.constants import AgentEventType, AgentRoute, AgentStage, AgentStatus
from src.app.agent.events import AgentEvent, format_sse_event
from src.app.agent.nodes.approval_packager_node import approval_packager_node
from src.app.agent.nodes.bootstrap_node import bootstrap_node
from src.app.agent.nodes.clarifier_node import clarifier_node
from src.app.agent.nodes.gap_analyzer_node import gap_analyzer_node
from src.app.agent.nodes.jd_analyst_node import jd_analyst_node
from src.app.agent.nodes.retriever_node import retriever_node
from src.app.agent.nodes.reviewer_node import reviewer_node
from src.app.agent.nodes.rewriter_node import rewriter_node
from src.app.agent.nodes.summarize_conversation_node import summarize_conversation_node
from src.app.agent.nodes.supervisor_node import supervisor_node
from src.app.agent.routing import decide_memory_route, decide_review_route, decide_supervisor_route
from src.app.agent.state import ResumeAgentInput, ResumeAgentOutput, ResumeAgentState
from src.app.config.settings import get_settings
from src.app.internal_dto.graph_continue_request import GraphContinueRequest
from src.app.internal_dto.graph_run_stream_request import GraphRunStreamRequest
from src.app.internal_dto.run_status_update_request import RunStatusUpdateRequest
from src.app.service.agent_event_service import agent_event_service
from src.app.service.java_gateway_service import java_gateway_service

logger = logging.getLogger("uvicorn.error")


# LangGraph 节点名 -> 业务阶段码。
# 这里主要给 SSE 事件和 Java 落库用，方便前端显示“当前执行到哪一步”。
NODE_STAGE_MAP = {
    "bootstrap": AgentStage.BOOTSTRAP,
    "summarize_conversation": AgentStage.MEMORY_SUMMARY,
    "supervisor": AgentStage.SUPERVISOR,
    "jd_analyst": AgentStage.JD_ANALYST,
    "gap_analyzer": AgentStage.GAP_ANALYZER,
    "retriever": AgentStage.RETRIEVER,
    "rewriter": AgentStage.REWRITER,
    "reviewer": AgentStage.REVIEWER,
    "clarifier": AgentStage.CLARIFIER,
    "approval_packager": AgentStage.APPROVAL_PACKAGER,
}

# LangGraph 节点名 -> 面向用户/调试界面的阶段提示。
NODE_MESSAGE_MAP = {
    "bootstrap": "加载简历上下文",
    "summarize_conversation": "整理长期记忆摘要",
    "supervisor": "判断下一步任务",
    "jd_analyst": "分析岗位 JD",
    "gap_analyzer": "分析简历差距",
    "retriever": "检索参考表达",
    "rewriter": "生成简历修改提案",
    "reviewer": "审查修改提案",
    "clarifier": "等待用户补充信息",
    "approval_packager": "封装待确认修改",
}

WORKFLOW_NODE_NAMES = tuple(NODE_STAGE_MAP.keys())


_CHECKPOINTER: Any | None = None
_CHECKPOINTER_CONTEXT: Any | None = None


def build_checkpointer() -> Any | None:
    """构建 LangGraph Redis checkpointer。

    依赖 `langgraph-checkpoint-redis`。如果本地尚未同步依赖，返回 None，
    这样 Studio 仍可启动；安装依赖后会自动启用 RedisSaver。

    执行逻辑：
    1. 优先复用模块级单例，避免每次 run 都新建 Redis 连接。
    2. 如果缺少 RedisSaver 依赖，返回 None，让本地 Studio 仍可调试。
    3. 如果 Redis 连接失败，也返回 None，避免服务启动阶段直接崩溃。
    4. 真正需要用户继续输入的 run，生产环境应确保 Redis checkpointer 可用。
    """
    global _CHECKPOINTER, _CHECKPOINTER_CONTEXT
    if _CHECKPOINTER is not None:
        return _CHECKPOINTER

    try:
        from langgraph.checkpoint.redis import RedisSaver as RedisSaverClass
    except ImportError:
        return None

    settings = get_settings()
    try:
        _CHECKPOINTER_CONTEXT = RedisSaverClass.from_conn_string(
            settings.redis_url,
            connection_args={
                "socket_connect_timeout": 1,
                "socket_timeout": 1,
            },
            ttl={
                "default_ttl": max(1, settings.agent_checkpoint_ttl_seconds // 60),
                "refresh_on_read": True,
            },
        )
        _CHECKPOINTER = _CHECKPOINTER_CONTEXT.__enter__()
        _CHECKPOINTER.setup()
        return _CHECKPOINTER
    except Exception:
        _CHECKPOINTER = None
        _CHECKPOINTER_CONTEXT = None
        return None


async def log_node_run(node_name: str, node_func: Any, state: ResumeAgentState) -> ResumeAgentState:
    """给节点执行加终端日志，方便本地调试 graph 路径。"""
    run_id = state.get("run_id", 0)
    logger.info("[resume-agent] node start runId=%s node=%s", run_id, node_name)
    try:
        result = await node_func(state)
    except Exception:
        logger.exception("[resume-agent] node failed runId=%s node=%s", run_id, node_name)
        raise

    if isinstance(result, dict):
        executed_nodes = [*state.get("executed_nodes", []), node_name]
        result["executed_nodes"] = executed_nodes
        logger.info(
            "[resume-agent] node done runId=%s node=%s status=%s errors=%s patches=%s",
            run_id,
            node_name,
            result.get("status") or state.get("status"),
            len(result.get("errors") or state.get("errors") or []),
            len(result.get("candidate_patches") or state.get("candidate_patches") or []),
        )
    else:
        logger.info("[resume-agent] node done runId=%s node=%s", run_id, node_name)
    return result


def logged_node(node_name: str, node_func: Any):
    """把业务节点包装成 LangGraph 可 await 的节点函数。"""

    async def run(state: ResumeAgentState) -> ResumeAgentState:
        return await log_node_run(node_name, node_func, state)

    return run


def build_graph(checkpointer: Any | None = None):
    """构建给 LangGraph Studio 和 FastAPI 共用的工作流。

    这里是整个 Agent 的“施工图”：
    1. Studio 通过 langgraph.json 读取下面导出的 graph。
    2. FastAPI 通过 agent_graph_service 调用同一个 graph。
    3. 所以图结构只在这里维护一份，避免调试图和真实接口不一致。

    主链路：
    1. bootstrap：加载 Java 权威上下文。
    2. summarize_conversation：必要时压缩历史消息。
    3. supervisor：判断信息是否足够。
    4. jd_analyst / gap_analyzer：抽取 JD 要求并分析简历差距。
    5. retriever：执行 Agentic RAG，召回参考知识。
    6. rewriter：生成候选 patch。
    7. reviewer：审查 patch 是否安全。
    8. approval_packager：交给用户确认。

    两个挂起点：
    1. clarifier：缺信息，等待用户补充。
    2. approval_packager：有修改建议，等待用户确认应用。
    """
    workflow = StateGraph(
        state_schema=ResumeAgentState,  # 内部完整状态，节点之间都通过它传递数据。
        input_schema=ResumeAgentInput,  # Studio/FastAPI 入口只展示最小输入。
        output_schema=ResumeAgentOutput,  # Studio 输出只展示关键结果。
    )

    # TODO 优化这个图, 因为 这个graph的state过于多了, 很有可能传给模型就会爆满
    # 注册节点。节点名会直接显示在 LangGraph Studio 的图里。
    workflow.add_node("bootstrap", logged_node("bootstrap", bootstrap_node))
    workflow.add_node(
        "summarize_conversation",
        logged_node("summarize_conversation", summarize_conversation_node),
    )
    workflow.add_node("supervisor", logged_node("supervisor", supervisor_node))
    workflow.add_node("jd_analyst", logged_node("jd_analyst", jd_analyst_node))
    workflow.add_node("gap_analyzer", logged_node("gap_analyzer", gap_analyzer_node))
    workflow.add_node("retriever", logged_node("retriever", retriever_node))
    workflow.add_node("rewriter", logged_node("rewriter", rewriter_node))
    workflow.add_node("reviewer", logged_node("reviewer", reviewer_node))
    workflow.add_node("clarifier", logged_node("clarifier", clarifier_node))
    workflow.add_node(
        "approval_packager",
        logged_node("approval_packager", approval_packager_node),
    )

    # 固定入口：任何 run 都先加载简历、schema、历史消息等上下文。
    workflow.set_entry_point("bootstrap")
    # 入口摘要只处理“刚进图时历史已经很长”的情况。
    # 节点执行中真正的上下文守卫在 compact_memory_for_model()，发生在每次调用模型之前。
    workflow.add_conditional_edges(
        "bootstrap",
        decide_memory_route,
        {
            "summarize_conversation": "summarize_conversation",
            "supervisor": "supervisor",
        },
    )
    workflow.add_edge("summarize_conversation", "supervisor")

    # supervisor 只负责判断路由：
    # - 信息不足：进入 clarifier，挂起等待用户补充
    # - 信息足够：进入 JD 分析链路
    # - 异常或无法判断：结束为 failed
    workflow.add_conditional_edges(
        "supervisor",
        decide_supervisor_route,
        {
            AgentRoute.JD_ANALYST: "jd_analyst",
            AgentRoute.CLARIFIER: "clarifier",
            AgentRoute.FAILED: END,
        },
    )

    # 主业务链路：JD 分析 -> 差距分析 -> 检索 -> 改写 -> 审查。
    workflow.add_edge("jd_analyst", "gap_analyzer")
    workflow.add_edge("gap_analyzer", "retriever")
    workflow.add_edge("retriever", "rewriter")
    workflow.add_edge("rewriter", "reviewer")

    # reviewer 决定 patch 能不能给用户确认：
    # - 通过：进入 approval_packager
    # - 不通过但可重试：退回 rewriter
    # - 超过重试次数：结束为 failed
    workflow.add_conditional_edges(
        "reviewer",
        decide_review_route,
        {
            AgentRoute.REWRITER: "rewriter",
            AgentRoute.APPROVAL_PACKAGER: "approval_packager",
            AgentRoute.FAILED: END,
        },
    )

    # 两个正常挂起点：
    # clarifier 等用户补充信息，approval_packager 等用户确认 patch。
    workflow.add_edge("clarifier", "supervisor")
    workflow.add_edge("approval_packager", END)
    # 评测脚本可传入 MemorySaver 等 checkpointer；默认走 Redis 生产 checkpointer。
    return workflow.compile(checkpointer=checkpointer if checkpointer is not None else build_checkpointer())


# 官方 LangGraph CLI/Studio 会读取这个变量。
# langgraph.json 里配置的是：./src/app/agent/graph.py:graph
graph = build_graph()


class AgentGraphService:
    """LangGraph 多 Agent 运行服务。

    这个类不是给 Studio 用的，而是给 FastAPI controller 用的。
    它把 Java 请求 DTO 转成 LangGraph state，并把 graph 输出转成 SSE。
    """

    def __init__(self, compiled_graph: Any | None = None) -> None:
        # 测试时可以传入自定义 graph；正常运行使用上面导出的 graph。
        self.graph = compiled_graph or graph

    def build_graph(self):
        """构建 LangGraph 工作流。"""
        return build_graph()

    def create_initial_state(self, request: GraphRunStreamRequest) -> ResumeAgentState:
        """根据启动请求创建初始 state。

        Java 调 /internal/graph/runs/stream 时会走这里。
        注意这里只放启动时已知的信息，简历详情由 bootstrap_node 再去 Java 加载。

        字段来源：
        1. run/session/resume/scene/targetSectionId 来自 Java。
        2. user_input/job_description 来自本次用户请求或 Java session。
        3. status/current_stage/retry/event/errors 是 Python 运行控制字段。
        4. resume_snapshot、schema、history 不在这里放，由 bootstrap_node 加载。
        """
        return {
            "run_id": request.run_id,
            "session_id": request.session_id,
            "resume_id": request.resume_id,
            "scene_code": request.scene_code,
            "status": AgentStatus.RUNNING,
            "current_stage": AgentStage.BOOTSTRAP,
            "user_input": request.user_input,
            "job_description": request.job_description,
            "target_section_id": request.target_section_id,
            "clarification_answers": [],
            "review_retry_count": 0,
            "event_seq": 0,
            "errors": [],
        }

    def create_continue_state(self, request: GraphContinueRequest) -> ResumeAgentState:
        """根据继续请求创建恢复 state。

        正常情况下 continue 会先从 checkpoint 取回旧 state。
        只有 checkpoint 不存在时，才用这个最小 state 兜底。
        注意：这个兜底 state 不包含 session_id / resume_id / scene_code，
        不能完整恢复业务上下文，生产链路应尽量依赖 checkpoint。

        恢复策略：
        1. 首选 Redis checkpoint 里的完整旧 state。
        2. 用户本次回答会通过 Command(resume=...) 注入 clarifier。
        3. checkpoint 缺失时只构造最小 state，避免接口直接报空指针。
        4. 最小 state 不能完整跑主链路，只作为调试和异常兜底。
        """
        answers = [answer.model_dump(by_alias=True) for answer in request.answers]
        return {
            "run_id": request.run_id or 0,
            "status": AgentStatus.RUNNING,
            "current_stage": AgentStage.SUPERVISOR,
            "clarification_answers": answers,
            "review_retry_count": 0,
            "event_seq": 0,
            "errors": [],
        }

    async def stream_run(self, request: GraphRunStreamRequest) -> AsyncIterator[str]:
        """启动新 run 并输出 SSE 字符串。

        这是 FastAPI start_run_stream() 的直接调用入口。
        """
        async for event in self.stream_graph_events(self.create_initial_state(request)):
            yield event

    async def continue_run(self, request: GraphContinueRequest) -> AsyncIterator[str]:
        """从 checkpoint 恢复 run 并继续输出 SSE 字符串。

        用户回答 clarifier 的追问后，Java 会调用这个入口继续执行。
        LangGraph Redis checkpointer 保存的是上次 WAITING_USER 时的 StateSnapshot。
        这里把用户新回答写入同一个 thread，再从 checkpoint 继续执行。

        执行顺序：
        1. 用 run_id 构造同一个 LangGraph thread_id。
        2. 尝试读取上次 interrupt 时保存的完整 state。
        3. 将用户回答包装成 Command(resume=...)。
        4. 继续 stream_graph_events，让 graph 从 interrupt 点恢复。
        """
        run_id = request.run_id or 0
        config = self._graph_config(run_id)
        state = await self._load_state_snapshot(config) or self.create_continue_state(request)
        resume_payload = {"answers": [answer.model_dump(by_alias=True) for answer in request.answers]}
        async for event in self.stream_graph_events(
            Command(resume=resume_payload),
            config=config,
            run_id=run_id,
            event_state=state,
        ):
            yield event

    async def cancel_run(self, run_id: int) -> None:
        """取消 run 并清理 checkpoint。

        当前是轻量实现：先清理 Python 本地 checkpoint，再尽量通知 Java。
        通知失败不阻断取消动作。
        """
        try:
            await java_gateway_service.notify_cancelled(run_id)
        except Exception:
            pass

    async def stream_graph_events(
        self,
        state: ResumeAgentState | None,
        *,
        config: dict[str, Any] | None = None,
        run_id: int | None = None,
        event_state: dict[str, Any] | None = None,
    ) -> AsyncIterator[str]:
        """执行 graph 并输出 SSE 字符串。

        LangGraph 的 astream(stream_mode="updates") 会按节点返回局部 state 更新。
        这里把每个节点更新转换成 Java/前端能理解的 SSE 事件。

        SSE 转换流程：
        1. 先发 run.started。
        2. 按 LangGraph updates 逐个处理节点输出。
        3. 普通节点输出 stage.changed。
        4. interrupt 输出 clarification.required。
        5. approval_packager 输出 approval.required。
        6. 异常时标记 failed，并尽量同步 Java。
        """
        # 拷贝一份 state，后续会不断合并节点返回值并递增 event_seq。
        # 这里不直接改入参，避免调用方继续持有的对象被流式过程污染。
        graph_input = state
        state = dict(event_state or state or {})
        if run_id is not None:
            state.setdefault("run_id", run_id)
        graph_config = config or self._graph_config(state.get("run_id", 0))
        state["status"] = AgentStatus.RUNNING
        state.setdefault("current_stage", AgentStage.BOOTSTRAP)
        # 生产路径下 Java 不再解析 Python SSE 字符串。
        # 所以 run 状态必须由 Python 主动回写 Java，否则前端只能看到 QUEUED。
        await self._safe_update_status(
            state,
            RunStatusUpdateRequest(
                status=AgentStatus.RUNNING,
                currentStage=state.get("current_stage", AgentStage.BOOTSTRAP),
            ),
        )

        yield await self._emit_event(
            state,
            AgentEventType.RUN_STARTED,
            message="Agent run 已启动",
        )

        try:
            # updates 模式返回的是“节点名 -> 该节点返回的局部 state”。
            # 例如 {"supervisor": {"route_decision": {...}}}。
            async for update in self.graph.astream(graph_input or None, config=graph_config, stream_mode="updates"):
                async for event_text in self._handle_graph_update(state, update):
                    yield event_text
            log_graph_summary(state)
        except Exception as exc:
            await self.mark_failed(state, exc)
            yield await self._emit_event(
                state,
                AgentEventType.RUN_FAILED,
                message=str(exc),
            )

    async def mark_failed(self, state: ResumeAgentState, exc: Exception) -> None:
        """将 run 标记为失败。"""
        state["status"] = AgentStatus.FAILED
        state.setdefault("errors", []).append(str(exc))
        try:
            # Java 是 run 状态的最终记录方，Python 失败后尽量同步给 Java。
            await java_gateway_service.update_run_status(
                state["run_id"],
                RunStatusUpdateRequest(status=AgentStatus.FAILED, errorMessage=str(exc)),
            )
        except Exception:
            pass

    def decide_supervisor_route(self, state: dict) -> str:
        """根据 Supervisor Agent 决策选择下一节点。"""
        return decide_supervisor_route(state)

    def decide_review_route(self, state: dict) -> str:
        """根据 Reviewer Agent 结果选择通过、重写或失败。"""
        return decide_review_route(state)

    async def _handle_graph_update(self, state: dict, update: dict[str, Any]) -> AsyncIterator[str]:
        """处理 LangGraph 单次节点更新。

        update 的形态通常是 {"节点名": {"该节点返回的局部 state"}}。
        我们先合并 state，再按节点名补发阶段事件和挂起事件。

        处理顺序：
        1. 如果是 __interrupt__，提取 payload 并发 clarification.required。
        2. 如果是普通节点，先把节点返回字段合并到本地事件视角 state。
        3. 根据节点名发 stage.changed。
        4. 对 clarifier / approval_packager 额外发前端需要的挂起事件。
        5. 对 summarize_conversation，把 summary 尽量同步回 Java。
        """
        for node_name, node_state in update.items():
            if node_name == "__interrupt__":
                payload = self._extract_interrupt_payload(node_state)
                state["clarification_payload"] = payload
                state["status"] = AgentStatus.WAITING_USER
                state["current_stage"] = AgentStage.CLARIFIER
                # 顺序很重要：
                # 1. 先落 clarification.required 事件。
                # 2. 再把状态改成 WAITING_USER。
                # Java SSE 看到终态后会准备关闭连接，先写事件可避免前端漏掉追问 payload。
                event_text = await self._emit_event(
                    state,
                    AgentEventType.CLARIFICATION_REQUIRED,
                    stage_code=AgentStage.CLARIFIER,
                    message="需要补充信息",
                    payload=payload,
                )
                await self._safe_update_status(
                    state,
                    RunStatusUpdateRequest(
                        status=AgentStatus.WAITING_USER,
                        currentStage=AgentStage.CLARIFIER,
                        clarificationPayload=payload,
                    ),
                )
                yield event_text
                continue

            if isinstance(node_state, dict):
                # 普通字段在这里覆盖合并，保证后续 SSE 能拿到最新 status / payload。
                # messages 的 reducer 已在 LangGraph 内部处理完成，这里只维护本地事件视角。
                state.update(node_state)

            stage_code = NODE_STAGE_MAP.get(node_name)
            if not stage_code:
                continue

            yield await self._emit_event(
                state,
                AgentEventType.STAGE_CHANGED,
                stage_code=stage_code,
                message=NODE_MESSAGE_MAP[node_name],
            )

            if node_name == "clarifier" and state.get("status") == AgentStatus.WAITING_USER:
                # clarifier 是等待用户输入的挂起点，前端收到后展示追问表单。
                # 这里同样先发事件再回写 WAITING_USER，保证 Java 事件流不会提前结束。
                event_text = await self._emit_event(
                    state,
                    AgentEventType.CLARIFICATION_REQUIRED,
                    stage_code=stage_code,
                    message="需要补充信息",
                    payload=state.get("clarification_payload", {}),
                )
                await self._safe_update_status(
                    state,
                    RunStatusUpdateRequest(
                        status=AgentStatus.WAITING_USER,
                        currentStage=stage_code,
                        clarificationPayload=state.get("clarification_payload", {}),
                    ),
                )
                yield event_text

            if node_name == "approval_packager":
                # approval_packager 是等待用户确认 patch 的挂起点，前端收到后展示 diff/审批包。
                # approvalPayload 会同时写到事件 payload 和 Java run 表，approveRun 以后读 Java 表为准。
                event_text = await self._emit_event(
                    state,
                    AgentEventType.APPROVAL_REQUIRED,
                    stage_code=stage_code,
                    message="已生成待确认修改建议",
                    payload=state.get("approval_payload", {}),
                )
                await self._safe_update_status(
                    state,
                    RunStatusUpdateRequest(
                        status=AgentStatus.WAITING_CONFIRM,
                        currentStage=stage_code,
                        approvalPayload=state.get("approval_payload", {}),
                    ),
                )
                yield event_text

            if node_name == "summarize_conversation" and state.get("summary"):
                await self._safe_update_status(
                    state,
                    RunStatusUpdateRequest(
                        status=state.get("status", AgentStatus.RUNNING),
                        currentStage=AgentStage.MEMORY_SUMMARY,
                        resultSummary=state["summary"],
                    ),
                )

    async def _emit_event(
        self,
        state: dict,
        event_type: str,
        *,
        stage_code: str | None = None,
        message: str | None = None,
        payload: dict[str, Any] | None = None,
    ) -> str:
        """构造并输出一条 SSE 事件。

        event_seq 是 run 内递增序号，前端可以用它保证事件顺序。

        事件落地顺序：
        1. 本地 event_seq 自增。
        2. 构造 AgentEvent。
        3. 尝试写回 Java 事件表。
        4. 转成 SSE 文本 yield 给 Java 代理层。
        """
        state["event_seq"] = state.get("event_seq", 0) + 1
        event = AgentEvent(
            eventSeq=state["event_seq"],
            eventType=event_type,
            runId=state.get("run_id", 0),
            sessionId=state.get("session_id"),
            stageCode=stage_code,
            message=message,
            payload=payload or {},
        )
        await self._safe_persist_event(event)
        # 即使当前是 MQ worker 路径，也继续返回 SSE 文本：
        # 1. FastAPI 调试接口还能直接流式查看。
        # 2. worker 会 drain 这些文本，只依赖上面的 Java 事件落库。
        return format_sse_event(event)

    async def _safe_persist_event(self, event: AgentEvent) -> None:
        """尽量把事件写回 Java。

        本地 Studio 调试时 Java 可能没启动，所以这里吞掉异常，避免影响看图。
        """
        try:
            await agent_event_service.persist_events(event.run_id, [event])
        except Exception:
            pass

    async def _safe_update_status(self, state: dict, request: RunStatusUpdateRequest) -> None:
        """尽量把状态写回 Java，失败不影响本地 Studio 调试。"""
        try:
            await java_gateway_service.update_run_status(state.get("run_id", 0), request)
        except Exception:
            pass

    def _extract_interrupt_payload(self, node_state: Any) -> dict[str, Any]:
        """兼容不同 LangGraph 版本的 interrupt update 结构。"""
        if isinstance(node_state, (list, tuple)) and node_state:
            value = getattr(node_state[0], "value", None)
            return value if isinstance(value, dict) else {}
        value = getattr(node_state, "value", None)
        return value if isinstance(value, dict) else {}

    def _apply_clarification_answers(self, state: dict, request: GraphContinueRequest) -> None:
        """把用户追问答案合并回 state。

        如果用户回答的是 JD，就直接写入 job_description，让 supervisor 下次走主链路。
        """
        values = []
        for answer in request.answers:
            values.append(answer.value)
            if answer.field_key in {"jobDescription", "job_description", "jd"}:
                state["job_description"] = answer.value
        # 兜底：如果原始 user_input 为空，就把所有回答拼成一段文本，
        # 让后续 JD 分析节点至少有可读取的用户输入。
        if values and not state.get("user_input"):
            state["user_input"] = "\n".join(values)

    def _graph_config(self, run_id: int) -> dict[str, Any]:
        """LangGraph checkpointer 使用 thread_id 关联同一个 run 的状态历史。"""
        return {
            "configurable": {
                "thread_id": str(run_id),
                "checkpoint_ns": "resume",
            }
        }

    def _has_checkpointer(self) -> bool:
        """判断当前 compiled graph 是否启用了 LangGraph checkpointer。"""
        return getattr(self.graph, "checkpointer", None) is not None

    async def _load_state_snapshot(self, config: dict[str, Any]) -> dict[str, Any] | None:
        """从 LangGraph StateSnapshot 中取出上次保存的 state。"""
        if not self._has_checkpointer() or not hasattr(self.graph, "aget_state"):
            return None
        snapshot = await self.graph.aget_state(config)
        values = getattr(snapshot, "values", None)
        return dict(values) if values else None


agent_graph_service = AgentGraphService()


def get_skipped_nodes(executed_nodes: list[str] | None) -> list[str]:
    """根据已执行节点推导本轮未执行节点。"""
    executed = set(executed_nodes or [])
    return [node for node in WORKFLOW_NODE_NAMES if node not in executed]


def log_graph_summary(state: dict[str, Any]) -> None:
    """打印本轮 graph 最终执行/跳过节点。"""
    executed_nodes = state.get("executed_nodes", [])
    logger.info(
        "[resume-agent] graph summary runId=%s executed=%s skipped=%s",
        state.get("run_id", 0),
        executed_nodes,
        get_skipped_nodes(executed_nodes),
    )
