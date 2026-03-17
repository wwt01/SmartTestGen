from fastapi import APIRouter, HTTPException
from app.schemas.text_schema import (TextRequest, StructuredRequest, FixCompilationErrorRequest)  # noqa: E501
from app.services.text_service import text_service
from app.utils.response import success_response, fail_response

# 创建路由实例（前缀 /api/text，方便区分接口模块）
router = APIRouter(prefix="/api/text", tags=["文本处理"])


@router.post("/parse", summary="解析 IDEA 插件选中的文本为结构化信息")
async def parse_text(request: TextRequest):
    try:
        # 调用业务逻辑处理文本
        result = text_service.process_selected_text(request.content)
        return success_response(data=result)
    except Exception as e:
        # 异常处理
        raise HTTPException(
            status_code=500, detail=fail_response(msg=str(e)).model_dump())


@router.post("/generate-test", summary="根据结构化信息生成 Java 单元测试代码")
async def generate_test_code(request: StructuredRequest):
    try:
        # 调用业务逻辑生成测试代码
        result = text_service.generate_test_case({
            "method_name": request.method_name,
            "parameters": request.parameters,
            "return_type": request.return_type,
            "expectations": request.expectations,
            "class_name": request.class_name,
            "is_interface": request.is_interface,
            "code_structure": request.code_structure,
            "file_content": request.file_content
        })
        # 检查是否有错误
        if "error" in result:
            raise HTTPException(status_code=500, detail=fail_response(
                msg=result["message"]).model_dump())
        return success_response(data=result)
    except Exception as e:
        # 异常处理
        raise HTTPException(
            status_code=500, detail=fail_response(msg=str(e)).model_dump())


@router.post("/fix-compilation-error", summary="修复测试代码的编译错误")
async def fix_compilation_error(request: FixCompilationErrorRequest):
    try:
        print("错误信息是"+request.error_message)
        # 调用业务逻辑修复编译错误
        result = text_service.fix_compilation_error({
            "code": request.code,
            "error_message": request.error_message,
            "code_structure": request.code_structure,
            "current_class_name": request.current_class_name,
            "is_interface_file": request.is_interface_file
        })
        # 检查是否有错误
        if "error" in result:
            raise HTTPException(status_code=500, detail=fail_response(
                msg=result["message"]).model_dump())
        return success_response(data=result)
    except Exception as e:
        # 异常处理
        raise HTTPException(
            status_code=500, detail=fail_response(msg=str(e)).model_dump())
