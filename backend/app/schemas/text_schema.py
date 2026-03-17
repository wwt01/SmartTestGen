from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any

# 接收 IDEA 插件发送的文本请求模型


class TextRequest(BaseModel):
    content: str = Field(..., description="IDEA 插件选中的自然语言需求内容")
    # 可选：扩展字段，比如用户标识（毕业设计可留空）
    user_id: Optional[str] = Field(None, description="可选：用户标识")

# 接收结构化信息的请求模型，用于生成 Java 单元测试代码


class StructuredRequest(BaseModel):
    method_name: str = Field(..., description="方法名")
    parameters: List[Dict[str, Any]] = Field(default_factory=list, description="参数信息列表，每个参数包含 name、type 和 constraints")
    return_type: str = Field(..., description="返回值类型")
    expectations: List[str] = Field(default_factory=list, description="断言逻辑列表")
    class_name: Optional[str] = Field(None, description="类名，用于生成测试类")
    is_interface: Optional[bool] = Field(False, description="是否为接口类，用于选择不同的提示词模板")
    code_structure: Optional[str] = Field(None, description="代码结构信息，字符串形式")
    file_content: Optional[str] = Field(None, description="需求注释所在文件的代码内容")

# 统一响应模型


class TextResponse(BaseModel):
    code: int = Field(200, description="响应码（200 成功，500 失败）")
    msg: str = Field("success", description="响应信息")
    data: Optional[Dict[str, Any]] = Field(None, description="返回数据")

# 修复编译错误的请求模型


class FixCompilationErrorRequest(BaseModel):
    code: str = Field(..., description="测试代码")
    error_message: str = Field(..., description="编译错误信息")
    code_structure: Optional[str] = Field(None, description="代码结构信息")
    current_class_name: Optional[str] = Field(None, description="当前类名")
    is_interface_file: Optional[bool] = Field(False, description="是否是接口文件")
