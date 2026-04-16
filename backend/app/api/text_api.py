from fastapi import APIRouter, Depends
from fastapi.responses import JSONResponse
from app.schemas.text_schema import (TextRequest, StructuredRequest, FixCompilationErrorRequest, InitSessionRequest, PreCompileRequest)  # noqa: E501
from app.services.text_service import TextService
from app.dependencies import get_text_service
from app.utils.response import success_response, fail_response
from app.utils.compilation_util import CompilationUtil
import logging
import time

router = APIRouter(prefix="/api/text", tags=["文本处理"])
logger = logging.getLogger(__name__)


@router.post("/init-session", summary="初始化会话，存储静态上下文信息")
async def init_session(
    request: InitSessionRequest,
    text_service: TextService = Depends(get_text_service)
):
    start_time = time.time()
    try:
        logger.info(">>> 接收初始化会话请求")
        session_id = text_service.init_session({
            "class_name": request.class_name,
            "is_interface": request.is_interface,
            "package_name": request.package_name,
            "class_type": request.class_type,
            "fields": request.fields,
            "methods": request.methods,
            "dependencies": request.dependencies
        })
        elapsed = (time.time() - start_time) * 1000
        logger.info(f"<<< 初始化成功 [{elapsed:.0f}ms]: {session_id}")
        return success_response(data={"session_id": session_id})
    except Exception as e:
        elapsed = (time.time() - start_time) * 1000
        logger.error(f"<<< 初始化失败 [{elapsed:.0f}ms]: {str(e)}")
        return JSONResponse(
            status_code=500,
            content=fail_response(msg=str(e)).model_dump()
        )


@router.post("/parse", summary="解析 IDEA 插件选中的文本为结构化信息")
async def parse_text(
    request: TextRequest,
    text_service: TextService = Depends(get_text_service)
):
    start_time = time.time()
    try:
        logger.info(">>> 接收需求解析请求")
        result = text_service.process_selected_text(
            content=request.content
        )
        elapsed = (time.time() - start_time) * 1000
        logger.info(f"<<< 解析成功 [{elapsed:.0f}ms]")
        return success_response(data=result)
    except Exception as e:
        elapsed = (time.time() - start_time) * 1000
        logger.error(f"<<< 解析失败 [{elapsed:.0f}ms]: {str(e)}")
        return JSONResponse(
            status_code=500,
            content=fail_response(msg=str(e)).model_dump()
        )


@router.post("/generate-test", summary="根据结构化信息生成 Java 单元测试代码和空方法")
async def generate_test_code(
    request: StructuredRequest,
    text_service: TextService = Depends(get_text_service)
):
    start_time = time.time()
    try:
        logger.info(f">>> 接收生成测试请求: {request.method_name}")
        result = text_service.generate_test_case({
            "method_name": request.method_name,
            "parameters": request.parameters,
            "return_type": request.return_type,
            "expectations": request.expectations,
            "session_id": request.session_id,
            "is_static": request.is_static
        })
        if "error" in result:
            elapsed = (time.time() - start_time) * 1000
            logger.error(f"<<< 生成失败 [{elapsed:.0f}ms]: {result['message']}")
            return JSONResponse(
                status_code=500,
                content=fail_response(msg=result["message"]).model_dump()
            )
        elapsed = (time.time() - start_time) * 1000
        logger.info(f"<<< 生成成功 [{elapsed:.0f}ms]: {len(result['test_code'])} 字符")
        return success_response(data=result)
    except Exception as e:
        elapsed = (time.time() - start_time) * 1000
        logger.error(f"<<< 生成失败 [{elapsed:.0f}ms]: {str(e)}")
        return JSONResponse(
            status_code=500,
            content=fail_response(msg=str(e)).model_dump()
        )


@router.post("/fix-compilation-error", summary="修复测试代码的编译错误")
async def fix_compilation_error(
    request: FixCompilationErrorRequest,
    text_service: TextService = Depends(get_text_service)
):
    start_time = time.time()
    try:
        logger.info(">>> 接收修复编译错误请求")
        result = text_service.fix_compilation_error({
            "code": request.code,
            "error_message": request.error_message,
            "session_id": request.session_id,
            "method_source": request.method_source
        })
        if "error" in result:
            elapsed = (time.time() - start_time) * 1000
            logger.error(f"<<< 修复失败 [{elapsed:.0f}ms]: {result['message']}")
            return JSONResponse(
                status_code=500,
                content=fail_response(msg=result["message"]).model_dump()
            )
        elapsed = (time.time() - start_time) * 1000
        logger.info(f"<<< 修复成功 [{elapsed:.0f}ms]: {len(result['test_code'])} 字符")
        return success_response(data=result)
    except Exception as e:
        elapsed = (time.time() - start_time) * 1000
        logger.error(f"<<< 修复失败 [{elapsed:.0f}ms]: {str(e)}")
        return JSONResponse(
            status_code=500,
            content=fail_response(msg=str(e)).model_dump()
        )


@router.post("/pre-compile", summary="预编译测试代码，返回编译结果")
async def pre_compile(
    request: PreCompileRequest
):
    start_time = time.time()
    try:
        logger.info(">>> 接收预编译请求")
        
        if not CompilationUtil.check_javac_available():
            elapsed = (time.time() - start_time) * 1000
            logger.error(f"<<< 预编译失败 [{elapsed:.0f}ms]: javac不可用")
            return JSONResponse(
                status_code=500,
                content=fail_response(msg="javac not available on server").model_dump()
            )
        
        result = CompilationUtil.compile_test_code(
            package_name=request.package_name,
            class_name=request.class_name,
            empty_method=request.empty_method,
            test_code=request.test_code
        )
        
        elapsed = (time.time() - start_time) * 1000
        
        if result["success"]:
            logger.info(f"<<< 预编译成功 [{elapsed:.0f}ms]")
            return success_response(data={
                "success": True,
                "error_message": ""
            })
        else:
            logger.info(f"<<< 预编译失败 [{elapsed:.0f}ms]: 编译错误")
            return success_response(data={
                "success": False,
                "error_message": result["error_message"]
            })
            
    except Exception as e:
        elapsed = (time.time() - start_time) * 1000
        logger.error(f"<<< 预编译失败 [{elapsed:.0f}ms]: {str(e)}")
        return JSONResponse(
            status_code=500,
            content=fail_response(msg=str(e)).model_dump()
        )
