"""
LLM需求描述生成模块
使用大语言模型为方法生成详细的需求描述
"""

from utils.config_manager import config
from utils.log_manager import logger
import os
import sys
import json
from datetime import datetime
from typing import List, Dict, Any
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage

# 添加utils模块路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))



class LLMRequirementGenerator:
    """LLM需求描述生成器"""

    def __init__(self):
        """
        初始化LLM需求描述生成器
        """
        # 获取LLM配置
        llm_config = config.get_llm_config()
        # self.api_key = llm_config.get("api_key", "sk-b1f7e3a3f60648638582f54c7e36de45")
        # self.base_url = llm_config.get("base_url", "https://api.deepseek.com")
        # self.model_name = llm_config.get("model_name", "deepseek-chat")
        # self.temperature = llm_config.get("temperature", 0.7)
        # self.max_tokens = llm_config.get("max_tokens", 1000)
        self.api_key = "sk-b1f7e3a3f60648638582f54c7e36de45"
        self.base_url = "https://api.deepseek.com"
        self.model_name = "deepseek-chat"
        self.temperature =0.7
        self.max_tokens = 1000
        try:
            self.llm = ChatOpenAI(
                api_key=self.api_key,
                base_url=self.base_url,
                model=self.model_name,
                temperature=self.temperature
            )
            logger.info("✅ LLM initialized successfully")
        except Exception as e:
            logger.error(f"Warning: Failed to initialize LLM: {e}")


#     def generate_requirement(self, class_info, method_info):
#         """
#         使用LLM生成需求描述

#         Args:
#             class_info: 类信息字典，包含class_name和original_code
#             method_info: 方法信息字典，包含name、parameters、return_type

#         Returns:
#             需求描述字符串
#         """
#         class_name = class_info['class_name']
#         method_name = method_info['name']
#         parameters = method_info['parameters']
#         return_type = method_info['return_type']
#         original_code = class_info.get('original_code', '')

#         # 如果没有LLM，使用简单的规则生成需求描述
#         if not self.llm:
#             return self._generate_simple_requirement(method_name, parameters, return_type)

#         # 构建提示词
#         prompt = f"""你是一个专业的Java测试工程师。请根据以下Java方法信息，生成详细的测试需求描述。

# 类名：{class_name}
# 方法名：{method_name}
# 参数列表：{parameters}
# 返回类型：{return_type}

# 方法源代码：
# ```
# {original_code[:2000]}
# ```

# 请生成一个详细的测试需求描述，要求如下：

# 1. **方法功能描述**：简要说明该方法的功能和作用
# 2. **参数说明**：列出所有参数及其类型
# 3. **内部逻辑**：根据方法名和代码，推测方法的内部实现逻辑
# 4. **返回值说明**：说明返回值的类型和含义
# 5. **测试场景**：列出需要测试的场景（正常情况、边界情况、异常情况等）

# 要求：
# - 描述要详细且准确
# - 覆盖主要的测试场景
# - 语言简洁明了，使用中文
# - 只返回需求描述，不包含其他任何内容
# - 不要包含代码示例

# 请直接输出需求描述，不要有任何前言或后缀。"""

#         try:
#             # 调用LLM生成需求描述
#             response = self.llm.invoke([HumanMessage(content=prompt)])
#             requirement = response.content.strip()

#             return requirement
#         except Exception as e:
#             logger.error(f"Error generating requirement with LLM: {e}")
#             # 如果LLM调用失败，使用简单的规则生成
#             return self._generate_simple_requirement(method_name, parameters, return_type)

    def generate_requirement(self, original_code: str) -> str:
        """
        只需要传入 Java 源代码，自动生成测试需求描述
        兼容原有代码逻辑
        """
        # 如果没有LLM，返回简单提示
        if not self.llm:
            return "无法调用大模型，请配置API密钥"

        # 极简提示词：只看代码 → 生成标准测试需求
        prompt = f"""你是专业Java测试工程师。
    根据下面的Java代码，生成一段标准测试需求描述，必须包含：
    1. 方法功能
    2. 输入参数说明
    3. 返回值说明
    4. 测试场景（正常场景、边界场景、异常场景）

    要求：
    - 纯中文，专业简洁
    - 只输出需求描述，不要多余内容
    - 不要代码，不要格式符号

    Java代码：
    {original_code}
    """

        try:
            response = self.llm.invoke(prompt)
            return response.content.strip()
        except Exception as e:
            logger.error(f"LLM调用失败: {e}")
            return "LLM调用失败，无法生成需求"
        
    def _generate_simple_requirement(self, method_name: str, parameters: str, return_type: str) -> str:
        """
        使用简单规则生成需求描述（当LLM不可用时）

        Args:
            method_name: 方法名
            parameters: 参数列表
            return_type: 返回类型

        Returns:
            需求描述字符串
        """
        # 解析参数
        param_list = [p.strip() for p in parameters.split(',') if p.strip()] if parameters else []
        param_count = len(param_list)
        param_desc = "、".join(param_list) if param_list else "无参数"

        # 根据方法名推测功能
        if 'add' in method_name.lower():
            logic = "将两个或多个数值相加"
        elif 'subtract' in method_name.lower():
            logic = "从一个数值中减去另一个数值"
        elif 'multiply' in method_name.lower():
            logic = "将两个或多个数值相乘"
        elif 'divide' in method_name.lower():
            logic = "将一个数值除以另一个数值"
        elif 'is' in method_name.lower() or 'has' in method_name.lower():
            logic = "判断某个条件是否成立"
        elif 'get' in method_name.lower():
            logic = "获取某个属性或值"
        elif 'set' in method_name.lower():
            logic = "设置某个属性或值"
        elif 'calculate' in method_name.lower():
            logic = "执行某种计算操作"
        elif 'validate' in method_name.lower() or 'check' in method_name.lower():
            logic = "验证输入是否符合要求"
        else:
            logic = "执行特定的业务逻辑"

        # 构建需求描述
        requirement = f"""测试{method_name}方法：
- 方法功能：{logic}
- 参数数量：{param_count}个
- 参数类型：{param_desc}
- 内部逻辑：{logic}
- 返回类型：{return_type}
- 测试场景：需要测试正常输入、边界值和异常情况"""

        return requirement

    def process_test_cases(self, test_cases: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """处理测试用例，为每个方法生成需求描述"""
        if not test_cases:
            logger.warning("No test cases to process")
            return []

        logger.info("=" * 70)
        logger.info("Generating Requirements with LLM")
        logger.info(f"Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        logger.info("=" * 70)

        processed_cases = []
        total_cases = len(test_cases)

        for i, case in enumerate(test_cases):
            logger.info(f"Processing case {i + 1}/{total_cases}: {case['class_name']}.{case['method_name']}")

            # 构建类信息
            class_info = {
                'class_name': case['class_name'],
                'original_code': case.get('original_code', '')
            }

            # 构建方法信息
            method_info = {
                'name': case['method_name'],
                'parameters': case['parameters'],
                'return_type': case['return_type']
            }

            # 生成需求描述
            requirement = self.generate_requirement(case["original_code"])

            # 更新测试用例
            updated_case = case.copy()
            updated_case['requirement'] = requirement
            processed_cases.append(updated_case)

        logger.info(f"\n✅ Generated requirements for {len(processed_cases)} test cases")
        logger.info("=" * 70)

        return processed_cases

    def load_test_cases(self, input_path: str) -> List[Dict[str, Any]]:
        """加载测试用例"""
        if not os.path.exists(input_path):
            logger.error(f"File not found: {input_path}")
            return []

        try:
            with open(input_path, 'r', encoding='utf-8') as f:
                test_cases = json.load(f)
            logger.info(f"Loaded {len(test_cases)} test cases from {input_path}")
            return test_cases
        except Exception as e:
            logger.error(f"Error loading test cases: {e}")
            return []

    def save_test_cases(self, test_cases: List[Dict[str, Any]], output_path: str) -> bool:
        """保存测试用例"""
        if not test_cases:
            logger.warning("No test cases to save")
            return False

        try:
            # 确保输出目录存在
            output_dir = os.path.dirname(output_path)
            if output_dir:
                os.makedirs(output_dir, exist_ok=True)

            # 保存为JSON文件
            with open(output_path, 'w', encoding='utf-8') as f:
                json.dump(test_cases, f, ensure_ascii=False, indent=2)

            logger.info(f"Saved {len(test_cases)} test cases to {output_path}")
            return True
        except Exception as e:
            logger.error(f"Error saving test cases: {e}")
            return False


def main():
    """主函数"""
    logger.info("=" * 70)
    logger.info("LLM Requirement Generator")
    logger.info("=" * 70)

    # 检查API密钥
    llm_config = config.get_llm_config()
    api_key = llm_config.get("api_key", "")

    if not api_key:
        logger.warning("⚠️  No DeepSeek API key found.")
        logger.warning("   Please set api_key in config.json or DEEPSEEK_API_KEY environment variable.")
        logger.warning("   Example: set DEEPSEEK_API_KEY=your_api_key_here")
        logger.warning("   Without API key, the generator will use simple rule-based requirements.")
        logger.info("=" * 70)

    generator = LLMRequirementGenerator()

    # 获取输出配置
    output_config = config.get_output_config()
    results_dir = output_config.get("results_dir", "results")

    # 加载选择的测试用例
    input_path = os.path.join(results_dir, "selected_test_cases.json")
    test_cases = generator.load_test_cases(input_path)

    if not test_cases:
        # 如果没有选择的测试用例，加载爬取的测试数据
        input_path = os.path.join(results_dir, "crawled_test_data.json")
        test_cases = generator.load_test_cases(input_path)

    # 处理测试用例，生成需求描述
    processed_cases = generator.process_test_cases(test_cases[:10])  # 只处理前10个测试用例

    # 保存处理后的测试用例
    output_path = os.path.join(results_dir, "test_cases_with_requirements.json")
    generator.save_test_cases(processed_cases, output_path)


if __name__ == "__main__":
    main()
