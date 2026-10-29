"""批量质量审计脚本。

解决的问题：简历优化结果多时人看不过来。脚本用评测系统先给每个优化 patch 打分，
标出 PASS/FLAG，人只需要复核被 FLAG 的少数条目。

用法（在 backend/python 目录下运行）：
  python scripts/quality_audit.py                                  # 全量，LLM 打分
  python scripts/quality_audit.py --no-llm-score --limit 1          # 快速免费验证链路
  python scripts/quality_audit.py --out eval_reports/qa

参数：
  --cases           测试用例 JSON 路径（默认 eval_cases/cases.json）
  --limit           最多跑前 N 个用例（默认全量）
  --no-llm-score    关闭 LLM rubric 打分，只跑确定性规则（免费）
  --out             报告输出前缀（默认 eval_reports/quality_audit_report）

输出：{--out}.html（自包含可分享）+ {--out}.json（程序可读）。
"""
from __future__ import annotations

import argparse
import asyncio
import json
import sys
from datetime import datetime
from pathlib import Path

import dotenv
from langgraph.checkpoint.memory import MemorySaver

# 保证 scripts/ 下运行时也能导入应用包（cwd 不一定是 backend/python）。
ROOT = Path(__file__).resolve().parents[1]
SOURCE_ROOT = ROOT / "src"
if str(SOURCE_ROOT) not in sys.path:
    sys.path.insert(0, str(SOURCE_ROOT))

from app.evaluation.rubric import DIMENSION_LABELS, DIMENSIONS  # noqa: E402
from app.evaluation.scoring_service import score_run  # noqa: E402
from app.evaluation.types import AuditReport, RunResult  # noqa: E402
from app.graphs.resume_agent.graph import build_graph  # noqa: E402
from app.graphs.resume_agent.types import ResumeSectionPatch  # noqa: E402

DEFAULT_CASES = ROOT / "eval_cases" / "cases.json"
DEFAULT_OUT = ROOT / "eval_reports" / "quality_audit_report"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="简历优化质量审计")
    parser.add_argument("--cases", default=str(DEFAULT_CASES), help="测试用例 JSON 路径")
    parser.add_argument("--limit", type=int, default=0, help="最多跑前 N 个用例")
    parser.add_argument("--no-llm-score", action="store_true", help="关闭 LLM 打分")
    parser.add_argument("--out", default=str(DEFAULT_OUT), help="报告输出前缀")
    return parser.parse_args()


def load_cases(path: str) -> list[dict]:
    with open(path, encoding="utf-8") as f:
        return json.load(f)["cases"]


def build_state(case: dict, run_id: int) -> dict:
    """构造一次性 run 的初始 state（照抄 dev_resume_controller._build_state）。"""
    target_section_id = case["target_section_id"]
    return {
        "run_id": run_id,
        "session_id": case.get("session_id", 1),
        "resume_id": case.get("resume_id", 1),
        "scene_code": case.get("scene_code", "JD_CUSTOMIZE"),
        "user_input": case.get("user_input"),
        "job_description": case.get("job_description"),
        "target_section_id": target_section_id,
        "resume_snapshot": case["resume_snapshot"],
        "section_schemas": case.get("section_schemas", {}),
        "history_messages": case.get("history_messages", []),
        "summary": case.get("summary", ""),
        "clarification_answers": [],
        "editable_section_ids": [target_section_id],
        "constraints": {},
        "review_retry_count": 0,
        "event_seq": 0,
        "errors": [],
    }


async def run_case(graph, case: dict, run_id: int) -> RunResult:
    """跑一次完整优化图，收集 patch 与中间产物。"""
    state = build_state(case, run_id)
    config = {"configurable": {"thread_id": f"qa-{run_id}", "checkpoint_ns": "eval"}}
    patches: list[dict] = []
    gap_report: dict = {}
    jd_analysis: dict = {}
    skipped: str | None = None
    try:
        async for update in graph.astream(state, config=config, stream_mode="updates"):
            for node_name, node_state in update.items():
                if node_name == "__interrupt__":
                    skipped = "命中 clarifier 追问，评测用例需提供完整 JD"
                    continue
                if isinstance(node_state, dict):
                    if node_state.get("candidate_patches") is not None:
                        patches = node_state["candidate_patches"]
                    if node_state.get("gap_report"):
                        gap_report = node_state["gap_report"]
                    if node_state.get("jd_analysis"):
                        jd_analysis = node_state["jd_analysis"]
    except Exception as exc:  # 单用例异常不中断整批
        skipped = f"运行异常: {exc}"

    patch_objs: list[ResumeSectionPatch] = []
    for item in patches:
        if isinstance(item, ResumeSectionPatch):
            patch_objs.append(item)
        elif isinstance(item, dict):
            try:
                patch_objs.append(ResumeSectionPatch.model_validate(item))
            except Exception:
                continue
    return RunResult(
        case_id=case["id"],
        title=case["title"],
        patches=patch_objs,
        gap_report=gap_report,
        jd_analysis=jd_analysis,
        skipped=skipped,
    )


def _mean(values: list[float]) -> float:
    return round(sum(values) / len(values), 2) if values else 0.0


def build_flagged(results: list[RunResult]) -> list[dict]:
    flagged = []
    for run in results:
        if run.skipped:
            continue
        for q in run.quality:
            if q.verdict == "FLAG":
                flagged.append(
                    {
                        "case_id": run.case_id,
                        "title": run.title,
                        "patch_id": q.patch_id,
                        "score": q.score,
                        "reason": " | ".join(q.flag_reasons) or "低分需复核",
                    }
                )
    return flagged


async def main() -> None:
    args = parse_args()
    dotenv.load_dotenv(ROOT / ".env")

    cases = load_cases(args.cases)
    if args.limit:
        cases = cases[: args.limit]

    graph = build_graph(checkpointer=MemorySaver())

    results: list[RunResult] = []
    run_id = 1
    for case in cases:
        print(f"[qa] {case['id']} ...")
        run = await run_case(graph, case, run_id)
        run_id += 1
        if run.skipped:
            print(f"  skip: {run.skipped}")
            results.append(run)
            continue
        if not run.patches:
            print(f"  {len(run.patches)} patches（未生成优化建议）")
            results.append(run)
            continue
        print(f"  {len(run.patches)} patches，评分中 ...")
        await score_run(
            run,
            missing_keywords=run.gap_report.get("missingKeywords", []),
            job_description=case.get("job_description", ""),
            gap_report=run.gap_report,
            use_llm=not args.no_llm_score,
        )
        results.append(run)
        print(f"  overall={run.overall_score}  "
              f"PASS={sum(1 for q in run.quality if q.verdict == 'PASS')}/"
              f"FLAG={sum(1 for q in run.quality if q.verdict == 'FLAG')}")

    report = AuditReport(
        cases=results,
        flagged=build_flagged(results),
    )
    write_report(report, args.out, args)
    print(f"\n[done] report -> {args.out}.html / {args.out}.json")


def write_report(report: AuditReport, out_prefix: str, args: argparse.Namespace) -> None:
    out_path = Path(out_prefix)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    with open(out_path.with_suffix(".json"), "w", encoding="utf-8") as f:
        json.dump(report.model_dump(), f, ensure_ascii=False, indent=2)
    with open(out_path.with_suffix(".html"), "w", encoding="utf-8") as f:
        f.write(render_html(report, args))


def render_html(report: AuditReport, args: argparse.Namespace) -> str:
    meta_line = (
        f"生成时间 {datetime.now().strftime('%Y-%m-%d %H:%M:%S')} ｜ "
        f"参数：LLM 打分={not args.no_llm_score} ｜ 用例数 {len(report.cases)}"
    )
    dim_rows = "\n".join(
        f"<tr><td>{DIMENSION_LABELS[dim]}</td><td>{_mean_dim(report, dim)}</td></tr>"
        for dim in DIMENSIONS
    )
    case_rows = "\n".join(_render_case_row(run) for run in report.cases)
    flag_rows = "\n".join(
        f"<tr><td>{f['case_id']}</td><td>{f['title']}</td>"
        f"<td>{f['patch_id']}</td><td class='score'>{f['score']}</td><td>{f['reason']}</td></tr>"
        for f in report.flagged
    ) or "<tr><td colspan='5'>无 FLAG 项，全部通过 ✓</td></tr>"

    return f"""<!DOCTYPE html>
<html lang="zh-CN"><head><meta charset="utf-8">
<title>简历优化质量审计报告</title>
<style>
  body {{ font-family: system-ui, "Microsoft YaHei", sans-serif; margin: 24px; color: #222; background: #fafafa; }}
  h1 {{ font-size: 20px; }}
  h2 {{ font-size: 16px; margin-top: 28px; border-left: 4px solid #4a6cf7; padding-left: 8px; }}
  table {{ border-collapse: collapse; width: 100%; background: #fff; font-size: 13px; }}
  th, td {{ border: 1px solid #e2e2e2; padding: 6px 10px; text-align: left; }}
  th {{ background: #f0f3ff; }}
  .score {{ font-weight: 700; }}
  .verdict-flag {{ color: #c0392b; font-weight: 700; }}
  .verdict-pass {{ color: #27ae60; font-weight: 700; }}
  .meta {{ color: #666; font-size: 12px; }}
</style></head><body>
<h1>简历优化质量审计报告</h1>
<p class="meta">{meta_line}</p>

<h2>各维度平均分</h2>
<table>
  <tr><th>维度</th><th>平均分</th></tr>
  {dim_rows}
</table>

<h2>各用例结果</h2>
<table>
  <tr><th>用例</th><th>标题</th><th>patch 数</th><th>总分</th><th>PASS/FLAG</th><th>备注</th></tr>
  {case_rows}
</table>

<h2>需人工复核的 FLAG 项（只复核这些即可）</h2>
<table>
  <tr><th>用例</th><th>标题</th><th>patch</th><th>分数</th><th>理由</th></tr>
  {flag_rows}
</table>
</body></html>"""


def _mean_dim(report: AuditReport, dim: str) -> float:
    values = [q.dimension_scores.get(dim, 3) for run in report.cases for q in run.quality]
    return _mean(values)


def _render_case_row(run: RunResult) -> str:
    if run.skipped:
        return (
            f"<tr><td>{run.case_id}</td><td>{run.title}</td>"
            f"<td>-</td><td>-</td><td>-</td><td class='verdict-flag'>跳过：{run.skipped}</td></tr>"
        )
    pass_n = sum(1 for q in run.quality if q.verdict == "PASS")
    flag_n = sum(1 for q in run.quality if q.verdict == "FLAG")
    verdict_html = f"<span class='verdict-pass'>{pass_n} PASS</span> / <span class='verdict-flag'>{flag_n} FLAG</span>"
    note = "未生成 patch" if not run.quality else ""
    return (
        f"<tr><td>{run.case_id}</td><td>{run.title}</td>"
        f"<td>{len(run.patches)}</td><td class='score'>{run.overall_score}</td>"
        f"<td>{verdict_html}</td><td>{note}</td></tr>"
    )


if __name__ == "__main__":
    asyncio.run(main())
