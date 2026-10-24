from pydantic import BaseModel, Field

# analyst_node 的分析结果
class InterviewContext(BaseModel):
    target_position: str | None = Field(default=None, description="目标岗位名称")
    core_skills: list[str] = Field(default_factory=list, description="核心技术栈或能力要求")
    focus_areas: list[str] = Field(default_factory=list, description="面试重点考察方向")
    resume_highlights: list[str] = Field(default_factory=list, description="简历中与岗位匹配的亮点经历")


# 问题选项
class QuestionOption(BaseModel):
    option_key: str = Field("A,B,C,D")


# interrupt() 挂起时的payload
class InterviewPayload(BaseModel):
    # interrupt() 纯粹用来暂停，不塞题目
    run_id: int | str    # 前端通过 run_id 从 state 取 current_question 渲染

# answer_node 拿到的用户回答
class InterviewAnswer(BaseModel):
    question_id: int | str
    select_options: str = Field(description="用户选了哪个")
    additional_note: str | None = Field(description="用户补充说明")

# summary_node 的最终输出
class InterviewSummary(BaseModel):
    summary: str
    highlight: list[str] | None = Field(description="亮点")
    improvement: list[str] = Field(description="待提升")
    score : int | None = Field(description="可选评分")



# analyst_answer 的分析结果
class InterviewEvaluation(BaseModel):
    evaluation: str = Field(description="本轮回答的分析评价")
    score: int | None = Field(default=None, description="本轮评分 0-100")
    has_next_question: bool = Field(description="是否还有下一题")
    reason: str | None = Field(default=None, description="判断依据，为什么继续或结束")


class InterviewOption(BaseModel):
    key: str = Field(..., description="选项标识，例如 A / B / C / D")
    text: str = Field(..., description="选项内容")

# 面试题目 --node 输出给 interrupt() 的结构
class InterviewQuestion(BaseModel):
    question_text: str = Field(..., alias="questionText")
    options: list[InterviewOption] = Field(default_factory=list)
