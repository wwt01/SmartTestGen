import logging
from app.services.preprocessing import preprocessing_service
from app.services.llm_service import llm_service

# 配置日志
logger = logging.getLogger(__name__)


class TextService:
    """处理文本的业务逻辑"""

    @staticmethod
    def process_selected_text(content: str, session_id: str = None) -> dict:
        """
        处理 IDEA 插件发送的自然语言需求，生成结构化的信息
        :param content: 选中的自然语言需求内容
        :param session_id: 会话标识
        :return: 处理后的结果，包含原文本、清洗后的文本、弱特征和结构化信息
        """
        try:
            logger.info(f"Starting to process text: {content}")

            # 1. 文本预处理：清理和弱特征提取
            logger.info("Step 1: Preprocessing text")
            preprocessing_result = preprocessing_service.preprocess(content)
            logger.info(f"Preprocessing result: {preprocessing_result}")

            # 2. LLM结构化解析：构造提示词并获取结构化解析结果
            logger.info("Step 2: Calling LLM service")
            structured_result = llm_service.get_structured_result(
                preprocessing_result,
                session_id=session_id
            )
            logger.info(f"LLM result: {structured_result}")

            # 3. 返回处理结果
            logger.info("Step 3: Returning result")
            return {
                "original_text": preprocessing_result["original_text"],
                "cleaned_text": preprocessing_result["cleaned_text"],
                "weak_features": preprocessing_result["weak_features"],
                "structured_result": structured_result
            }
        except Exception as e:
            logger.error(f"Error processing text: {e}", exc_info=True)
            return {
                "original_text": content,
                "error": str(e),
                "message": "文本处理失败"
            }

    @staticmethod
    def generate_test_case(structured_data: dict) -> dict:
        """
        根据结构化信息生成 Java 单元测试代码
        :param structured_data: 包含方法名、参数、返回值类型和断言逻辑的结构化信息
        :return: 包含生成的测试代码的结果
        """
        try:
            logger.info(f"Starting to generate test case: {structured_data}")

            # 1. 调用 LLM 生成测试代码
            logger.info("Step 1: Calling LLM service to generate test code")
            test_code = llm_service.generate_test_code(structured_data)
            logger.info("Test code generated successfully")

            # 2. 调用 LLM 生成空方法代码
            logger.info("Step 2: Calling LLM service to generate empty method code")
            empty_method = llm_service.generate_empty_method(structured_data)
            logger.info("Empty method code generated successfully")

            # 3. 返回生成的测试代码和空方法代码
            logger.info("Step 3: Returning generated code")
            return {
                "test_code": test_code,
                "empty_method": empty_method,
                "structured_data": structured_data
            }
        except Exception as e:
            logger.error(f"Error generating test case: {e}", exc_info=True)
            return {
                "error": str(e),
                "message": "测试代码生成失败"
            }

    @staticmethod
    def fix_compilation_error(error_data: dict) -> dict:
        """
        修复编译错误
        :param error_data: 包含测试代码、编译错误信息、代码结构等信息
        :return: 包含修复后的测试代码的结果
        """
        try:
            logger.info(f"Starting to fix compilation error: {error_data}")

            # 1. 调用 LLM 修复编译错误
            logger.info("Step 1: Calling LLM service to fix compilation error")
            fixed_code = llm_service.fix_compilation_error(error_data)
            logger.info("Compilation error fixed successfully")

            # 2. 返回修复后的测试代码
            logger.info("Step 2: Returning fixed code")
            return {
                "test_code": fixed_code,
                "error_data": error_data
            }
        except Exception as e:
            logger.error(f"Error fixing compilation error: {e}", exc_info=True)
            return {
                "error": str(e),
                "message": "修复编译错误失败"
            }


# 实例化服务，供路由调用
text_service = TextService()
