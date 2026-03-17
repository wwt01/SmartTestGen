from app.schemas.text_schema import TextResponse

# 成功响应


def success_response(data: dict = {}, msg: str = "success") -> TextResponse:
    return TextResponse(code=200, msg=msg, data=data)

# 失败响应


def fail_response(msg: str = "failed", code: int = 500) -> TextResponse:
    return TextResponse(code=code, msg=msg, data={})
