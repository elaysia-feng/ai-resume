from pydantic import BaseModel, Field


class BootstrapConstraints(BaseModel):
    """Agent 本次运行约束,是否同意agent继续操作。"""

    allow_create_new_section: bool = Field(default=False, alias="allowCreateNewSection", description="是否允许新增简历模块")
    allow_delete_section: bool = Field(default=False, alias="allowDeleteSection", description="是否允许删除简历模块")
    allowed_patch_operation: list[str] = Field(default_factory=list, alias="allowedPatchOperation", description="允许使用的 patch 操作列表")
