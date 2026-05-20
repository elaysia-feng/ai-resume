from typing import Any

from langgraph.constants import END
from langgraph.graph import StateGraph

from src.app.agent.graph import build_checkpointer
from src.app.internal_dto.interview_graph_run_request import InterviewGraphRunRequest
from src.app.internal_dto.interview_run_response import InterviewStartResponse, InterviewContinueResponse
from src.app.interview.nodes.analyst_answer_node import analyst_answer_node
from src.app.interview.nodes.analyst_node import analyst_node
from src.app.interview.nodes.answer_node import answer_node
from src.app.interview.nodes.bootstrap_node import bootstrap_node
from src.app.interview.nodes.question_node import question_node
from src.app.interview.nodes.summary_node import summary_node
from src.app.interview.routing import route_after_analysis
from src.app.interview.state import InterviewAgentState
from langgraph.types import Command

# 构建checkpoint
checkpointer = build_checkpointer()

# 定义图
def build_graph():
    #TODO 定义input的结构, 定义output的结构

    workflow = StateGraph(
        # 定义的内部完整状态, 节点之间都通过它来传递数据
        state_schema=InterviewAgentState
    )

    # 注册节点
    workflow.add_node("bootstrap", bootstrap_node)
    workflow.add_node("analyst", analyst_node)
    workflow.add_node("question", question_node)
    workflow.add_node("answer", answer_node)
    workflow.add_node("analyst_answer", analyst_answer_node)
    workflow.add_node("summary", summary_node)

    # 固定入口
    workflow.set_entry_point("bootstrap")

    workflow.add_edge("bootstrap", "analyst")
    workflow.add_edge("analyst", "question")
    workflow.add_edge("question", "answer")
    workflow.add_edge("answer", "analyst_answer")
    workflow.add_conditional_edges(
        source="analyst_answer",
        path=route_after_analysis,
        # route_after_analysis 这个函数返回的就是 key
        path_map={
            "summary": "summary",
            "question": "question",
        }
    )

    workflow.add_edge("summary", END)


    return workflow.compile(checkpointer=checkpointer)
# #
# # 官方 LangGraph CLI/Studio 会读取这个变量。
# # langgraph.json 里配置的是：./src/app/agent/graph.py:graph
# interview_graph = build_graph()

interview_graph = build_graph()

class InterviewGraphService:
    def __init__(self, compiled_graph: Any | None = None) -> None:
        # 测试时可以传入自定义 graph；正常运行使用上面导出的 graph。
        self.graph = compiled_graph or interview_graph


    def _interview_graph_config(self, run_id) -> dict[str, Any]:
        """ 使用thread_id去标记同一个run的状态历史"""

        return {
            "configurable": {
                "thread_id": str(run_id),
                "checkpoint_ns": "interview",
            }
        }

    # 创建初始请求
    def create_initial_state(self, request: InterviewGraphRunRequest) -> InterviewAgentState:
        """传入初始化参数, 传入message"""
        return {
            "run_id": request.run_id,
            "session_id": request.session_id,
            "resume_id": request.resume_id,
            "scene_code": request.scene_code,
            "job_description": request.job_description,
            "job_type": request.job_type,
            "answers": request.answers,
        }

    async def start_run(self, request: InterviewGraphRunRequest) -> InterviewStartResponse:
        init_state = self.create_initial_state(request)
        config = self._interview_graph_config(request.run_id)
        # 封装后的形式
        # {
        #     "__interrupt__": [
        #         Interrupt(
        #             value={
        #                 "round_id": 1,
        #                 "round_no": 1,
        #                 "question": {...}
        #             }
        #         )
        #     ]
        # }

        # 这个update返回的是state delta(也就是 增量更新的state)
        async for update in self.graph.astream(init_state, config, stream_mode="updates"):
            for node_name, node_state in update.items():
                if node_name == "__interrupt__":
                    payload = self._extract_interrupt_payload(node_state)

                    # 如果payload不为空的话, 就返回对应的resp 去更新状态
                    return InterviewStartResponse(
                        status="WAITING_ANSWER",
                        round_id=payload.get("round_id", 0),
                        round_no=payload.get("round_no", 0),
                        question=payload.get("question", {}),
                    )

        # 没有 interrupt，直接结束
        return InterviewStartResponse(
            status="SUCCESS",
            round_id=0,
            round_no=0,
            question={},
        )

    async def continue_run(self, request: InterviewGraphRunRequest) -> InterviewContinueResponse:
        """继续执行 graph图"""
        if request.status != "READY_ANSWER":
            return InterviewContinueResponse(status="IGNORED")

        config = self._interview_graph_config(request.run_id)

        # 这个Command参数就会让 langgraph自动把checkpoint里面的数据传回给graph, 以达到恢复的目的
        final_state: dict[str, Any] = {}
        async for update in self.graph.astream(Command(resume={"answer": True})
                , config
                , stream_mode="updates"):

            # 如果继续执行还遇到了暂停节点的话(就是 题目还没出到 5 题, 就不会到Summary节点, 就肯定会一直到interrupt节点)
            for node_name, node_state in update.items():
                if node_name == "__interrupt__":
                    payload = self._extract_interrupt_payload(node_state)
                    return InterviewContinueResponse(
                        status="WAITING_ANSWER",
                        round_id=payload.get("round_id"),
                        round_no=payload.get("round_no"),
                        question=payload.get("question", {}),
                    )
                if isinstance(node_state, dict):
                    final_state.update(node_state)

        return InterviewContinueResponse(
            status="SUCCESS",
            summary={
                "summary": final_state.get("summary"),
                "highlight": final_state.get("highlight"),
                "improvement": final_state.get("improvement"),
                "score": final_state.get("score"),
            },
        )

    # 取interrupt 的节点状态里面的值, 就是得到对象里面的value属性
    def _extract_interrupt_payload(self, interrupt_state: Any) -> dict[str, Any]:
        if isinstance(interrupt_state, (list, tuple)) and interrupt_state:
            payload = getattr(interrupt_state[0], "value", None)
        else:
            payload = getattr(interrupt_state, "value", None)

        return payload if isinstance(payload, dict) else {}





