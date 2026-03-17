import json
import logging
import yaml
import os
from typing import Dict, Any
from langchain_openai import ChatOpenAI
from langchain_core.output_parsers import JsonOutputParser
from langchain_core.messages import SystemMessage, HumanMessage
from app.config import settings

# 配置日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class LLMService:
    """LLM调用服务 - 使用LangChain框架"""

    def __init__(self):
        """初始化LLM服务，加载样例文件和提示词配置"""
        self.examples = self._load_examples()
        self.prompts = self._load_prompts()

        # 初始化LangChain的ChatOpenAI实例
        self.llm = ChatOpenAI(
            api_key=settings.DASHSCOPE_API_KEY,
            base_url=settings.LLM_API_URL,
            model=settings.LLM_MODEL,
            temperature=0.3,
            max_tokens=2000,
            top_p=0.9,
            timeout=settings.LLM_TIMEOUT
        )

        # 创建JSON输出解析器
        self.json_parser = JsonOutputParser()

    def _load_examples(self) -> Dict[str, Any]:
        """加载样例配置文件"""
        try:
            with open(settings.EXAMPLES_CONFIG_PATH, 'r', encoding='utf-8') as f:
                return json.load(f)
        except Exception as e:
            logger.error(f"Error loading examples: {e}")
            return {"chinese": [], "english": []}

    def _load_prompts(self) -> Dict[str, Any]:
        """加载提示词配置文件"""
        try:
            prompts_path = os.path.join(os.path.dirname(__file__), "..", "config", "prompts.yml")
            with open(prompts_path, 'r', encoding='utf-8') as f:
                return yaml.safe_load(f)
        except Exception as e:
            logger.error(f"Error loading prompts: {e}")
            # 返回默认提示词，确保服务仍能运行
            return {
                "llm": {
                    "parse_requirement_prompt": "You are an expert in Java testing. Parse the following Java requirement into structured test case information.\n\n# Requirement\n{cleaned_text}\n\n# Output JSON\n{{\n  \"method_name\": \"string\",\n  \"parameters\": [],\n  \"return_type\": \"string\",\n  \"expectations\": []\n}}",
                    "generate_test_prompt": "You are an expert in Java testing. Generate JUnit 5 test code for method {method_name} with parameters {parameters} and return type {return_type}."
                }
            }

    def _detect_language(self, text: str) -> str:
        """检测文本语言（中文或英文）"""
        import re
        chinese_chars = len(re.findall(r'[\u4e00-\u9fff]', text))
        english_chars = len(re.findall(r'[a-zA-Z]', text))

        if chinese_chars > english_chars:
            return "chinese"
        else:
            return "english"

    def construct_parse_prompt(self, preprocessing_result: Dict[str, Any]) -> str:
        """构造简洁的提示词"""
        cleaned_text = preprocessing_result["cleaned_text"]

        # 检测语言
        language = self._detect_language(preprocessing_result["original_text"])
        examples = self.examples.get(language, [])

        # 从配置文件中获取提示词模板
        prompt_template = self.prompts.get("llm", {}).get("parse_requirement_prompt", "")

        # 如果模板为空，使用默认提示词
        if not prompt_template:
            prompt_template = "You are an expert in Java testing. Parse the following Java requirement into structured test case information.\n\n# Requirement\n{cleaned_text}\n\n# Output JSON\n{\n  \"method_name\": \"string\",\n  \"parameters\": [],\n  \"return_type\": \"string\",\n  \"expectations\": []\n}"

        # 使用Python的string.Template进行变量替换（模板中已经使用${}格式）
        import string
        template = string.Template(prompt_template)

        prompt = template.safe_substitute(
            cleaned_text=cleaned_text,
            examples=self._format_examples(examples, limit=6)
        )

        return prompt

    def _format_examples(self, examples: list, limit: int = 10) -> str:
        """格式化样例为字符串，限制数量避免混淆"""
        if not examples:
            return "No examples available."

        # 只取前limit个样例，提供足够的样例给LLM参考
        examples_to_show = examples[:limit]

        formatted = []
        for i, example in enumerate(examples_to_show, 1):
            natural_lang = example.get("natural_language", "")
            structured = example.get("structured_result", {})
            formatted.append(f"""
Example {i}:
Natural Language: {natural_lang}
Structured Result: {json.dumps(structured, ensure_ascii=False, indent=2)}
""")

        return '\n'.join(formatted)

    def get_structured_result(self, preprocessing_result: Dict[str, Any]) -> Dict[str, Any]:
        """获取结构化解析结果 - 使用LangChain"""
        logger.info("Starting get_structured_result")

        # 构造提示词
        logger.info("Constructing prompt")
        prompt = self.construct_parse_prompt(preprocessing_result)
        logger.info(f"Prompt constructed, length: {len(prompt)}")

        # 打印完整提示词用于调试
        logger.info("=" * 80)
        logger.info("FULL PROMPT SENT TO LLM:")
        logger.info("=" * 80)
        logger.info(prompt)
        logger.info("=" * 80)

        # 使用LangChain调用LLM
        logger.info("Calling LLM with LangChain")

        try:
            # 创建消息列表
            messages = [
                SystemMessage(content="You are an expert in Java software testing and natural language processing."),
                HumanMessage(content=prompt)
            ]

            # 调用LLM
            response = self.llm.invoke(messages)
            content = response.content

            logger.info(f"LLM call completed, raw response: {content}")

            # 移除markdown代码块标记
            if content.startswith("```json"):
                content = content[7:]  # 移除 ```json
            elif content.startswith("```"):
                content = content[3:]  # 移除 ```

            if content.endswith("```"):
                content = content[:-3]  # 移除 ```

            content = content.strip()

            # 解析JSON响应
            structured_data = json.loads(content)

            logger.info(f"Parsed structured data: {structured_data}")
            return structured_data

        except Exception as e:
            logger.error(f"Error calling LLM: {e}")
            logger.error(f"Error type: {type(e).__name__}")
            # 返回默认结构化结果
            default_result = {
                "method_name": "",
                "parameters": [],
                "return_type": "",
                "expectations": [],
                "is_constructed": {
                    "method_name": False,
                    "parameters": False,
                    "return_type": False
                }
            }
            return default_result

    def generate_test_code(self, structured_data: Dict[str, Any]) -> str:
        """根据结构化信息生成 Java 单元测试代码 - 使用LangChain"""
        logger.info("Starting generate_test_code")

        # 提取结构化信息
        method_name = structured_data.get("method_name", "")
        parameters = structured_data.get("parameters", [])
        return_type = structured_data.get("return_type", "")
        expectations = structured_data.get("expectations", [])
        class_name = structured_data.get("class_name", "Example")
        is_interface = structured_data.get("is_interface", False)
        code_structure = structured_data.get("code_structure", "")
        file_content = structured_data.get("file_content", "")
        empty_method_code = structured_data.get("empty_method_code", "")

        logger.info(f"Generating test code for method: {method_name}")
        logger.info(f"Parameters: {parameters}")
        logger.info(f"Return type: {return_type}")
        logger.info(f"Expectations: {expectations}")
        logger.info(f"Class name: {class_name}")
        logger.info(f"Is interface: {is_interface}")
        logger.info(f"Code structure provided: {code_structure}")
        logger.info(f"File content provided: {file_content[:200]}..." if file_content else "No file content provided")
        logger.info(f"Empty method code provided: {empty_method_code[:200]}..." if empty_method_code else "No empty method code provided")

        # 分析文件内容，提取类信息和package信息
        package_name = ""
        if file_content:
            # 尝试从文件内容中提取package信息
            import re
            # 查找package定义
            package_match = re.search(r'package\s+([\w\.]+);', file_content)
            if package_match:
                package_name = package_match.group(1)
                logger.info(f"Extracted package information: {package_name}")

            # 尝试从文件内容中提取类名
            if not class_name:
                # 查找类定义
                class_match = re.search(r'class\s+(\w+)\s*[\{\(]', file_content)
                if class_match:
                    class_name = class_match.group(1)
                # 查找接口定义
                interface_match = re.search(r'interface\s+(\w+)\s*[\{]', file_content)
                if interface_match:
                    class_name = interface_match.group(1)
                logger.info(f"Extracted class information: name={class_name}")

        # 根据is_interface参数选择不同的提示词模板
        if is_interface:
            prompt_template = self.prompts.get("llm", {}).get("generate_test_prompt_interface", "")
            logger.info("Using interface test prompt template")
        else:
            prompt_template = self.prompts.get("llm", {}).get("generate_test_prompt", "")
            logger.info("Using regular class test prompt template")

        # 如果模板为空，使用默认提示词
        if not prompt_template:
            prompt_template = "You are an expert in Java testing. Generate JUnit 5 test code based on the following information.\n\n"
            prompt_template += "# Method Information\n"
            prompt_template += "Method Name: ${method_name}\n"
            prompt_template += "Parameters: ${parameters}\n"
            prompt_template += "Return Type: ${return_type}\n"
            prompt_template += "Class Name: ${class_name}\n"
            prompt_template += "Expectations: ${expectations}\n\n"
            prompt_template += "# File Context (if available)\n"
            prompt_template += "${file_content}...\n\n"
            prompt_template += "# Empty Method Code (if available)\n"
            prompt_template += "${empty_method_code}\n\n"
            prompt_template += "# Requirements\n"
            prompt_template += "1. Generate complete JUnit 5 test class\n"
            prompt_template += "2. Use proper Java syntax and JUnit 5 annotations\n"
            prompt_template += "3. Include necessary import statements for all classes used in the test code\n"
            prompt_template += "4. Create test methods for different scenarios based on expectations\n"
            prompt_template += "5. Use meaningful test method names\n"
            prompt_template += "6. Include setup and teardown methods if needed\n"
            prompt_template += "7. Mock dependencies if necessary\n"
            prompt_template += "8. Consider the file context and empty method code to ensure the test code is compatible with the existing class structure\n"
            prompt_template += "9. Ensure the test code can compile and run successfully with the provided class and method\n"
            prompt_template += "10. IMPORTANT: For any class used in the test code that is not part of the standard Java library or JUnit 5, include the appropriate import statement\n"
            prompt_template += "11. IMPORTANT: For example, if the class being tested is in the package 'com.example.calculator', include 'import com.example.calculator.Calculator;'\n"
            prompt_template += "12. IMPORTANT: For any other classes used in the test code (e.g., custom data types, utilities), include their import statements as well\n\n"
            prompt_template += "# Example Output Format\n"
            prompt_template += "```java\n"
            prompt_template += "# If package information is provided:\n"
            prompt_template += "package ${package_name};\n\n"
            prompt_template += "import org.junit.jupiter.api.BeforeEach;\n"
            prompt_template += "import org.junit.jupiter.api.Test;\n"
            prompt_template += "import static org.junit.jupiter.api.Assertions.*;\n"
            prompt_template += "// Add any other necessary imports for the class being tested\n"
            prompt_template += "import com.example.calculator.Calculator; // Example import for the class being tested\n"
            prompt_template += "import com.example.utils.MathUtils; // Example import for other classes used\n\n"
            prompt_template += "public class ${class_name}Test {\n"
            prompt_template += "    private ${class_name} instance;\n\n"
            prompt_template += "    @BeforeEach\n"
            prompt_template += "    void setUp() {\n"
            prompt_template += "        instance = new ${class_name}();\n"
            prompt_template += "    }\n\n"
            prompt_template += "    @Test\n"
            prompt_template += "    void test${MethodName}() {\n"
            prompt_template += "        // Test setup\n"
            prompt_template += "        // Method call\n"
            prompt_template += "        // Assertions\n"
            prompt_template += "    }\n"
            prompt_template += "}\n"
            prompt_template += "```\n\n"
            prompt_template += "# Important Note\n"
            prompt_template += "Always use the actual class name extracted from the file context for creating instances and calling methods.\n"
            prompt_template += "Do not create non-existent classes like 'TestClass'. Use the real class name from the file context.\n"
            prompt_template += "For example, if the class name is 'Calculator', create an instance like 'private Calculator calculator = new Calculator();'\n"
            prompt_template += "and call methods like 'calculator.calculateSum(a, b);'\n\n"
            prompt_template += "# Your Output\n"
            prompt_template += "Generate the complete JUnit 5 test class based on the provided information, including all necessary import statements."

        # 格式化参数信息
        formatted_parameters = json.dumps(parameters, ensure_ascii=False, indent=2)
        formatted_expectations = json.dumps(expectations, ensure_ascii=False, indent=2)

        # 将 method_name 转换为首字母大写的形式，用于生成测试方法名
        method_name_capitalized = method_name.capitalize() if method_name else ""

        # 使用Python的string.Template进行变量替换（模板中已经使用${}格式）
        import string
        template = string.Template(prompt_template)

        # 准备文件内容和空方法代码
        file_context = file_content[:500]

        prompt = template.safe_substitute(
            class_name=class_name,
            method_name=method_name,
            MethodName=method_name_capitalized,
            parameters=formatted_parameters,
            return_type=return_type,
            expectations=formatted_expectations,
            code_structure=code_structure,
            file_content=file_context,
            empty_method_code=empty_method_code,
            package_name=package_name
        )

        logger.info(f"Prompt constructed for test generation, length: {len(prompt)}")

        # 打印完整提示词用于调试
        logger.info("=" * 80)
        logger.info("FULL PROMPT SENT TO LLM (TEST GENERATION):")
        logger.info("=" * 80)
        logger.info(prompt)
        logger.info("=" * 80)

        # 使用LangChain调用LLM生成测试代码
        try:
            # 创建消息列表
            messages = [
                SystemMessage(content="You are an expert in Java software testing and JUnit 5."),
                HumanMessage(content=prompt)
            ]

            # 调用LLM
            logger.info("Calling LLM for test code generation with LangChain")
            response = self.llm.invoke(messages)
            test_code = response.content

            logger.info(f"LLM response received, length: {len(test_code)}")
            logger.info(f"Full LLM response:\n{test_code}")

            # 提取代码块
            if test_code.startswith("```java"):
                test_code = test_code[7:]
            elif test_code.startswith("```"):
                test_code = test_code[3:]

            if test_code.endswith("```"):
                test_code = test_code[:-3]

            test_code = test_code.strip()

            logger.info("Test code generation completed successfully")
            return test_code

        except Exception as e:
            logger.error(f"Error generating test code: {e}")
            logger.error(f"Error type: {type(e).__name__}")
            import traceback
            logger.error(f"Error traceback: {traceback.format_exc()}")
            return "// Failed to generate test code"

    def generate_empty_method(self, structured_data: Dict[str, Any]) -> str:
        """根据结构化信息生成 Java 空方法代码 - 使用LangChain"""
        logger.info("Starting generate_empty_method")

        # 提取结构化信息
        method_name = structured_data.get("method_name", "")
        parameters = structured_data.get("parameters", [])
        return_type = structured_data.get("return_type", "void")
        file_content = structured_data.get("file_content", "")

        logger.info(f"Generating empty method code for: {method_name}")
        logger.info(f"Parameters: {parameters}")
        logger.info(f"Return type: {return_type}")
        logger.info(f"File content provided: {file_content[:200]}..." if file_content else "No file content provided")

        # 分析文件内容，提取类信息
        class_name = ""
        is_interface = False
        if file_content:
            # 尝试从文件内容中提取类名
            import re
            # 查找类定义
            class_match = re.search(r'class\s+(\w+)\s*[\{\(]', file_content)
            if class_match:
                class_name = class_match.group(1)
            # 查找接口定义
            interface_match = re.search(r'interface\s+(\w+)\s*[\{]', file_content)
            if interface_match:
                class_name = interface_match.group(1)
                is_interface = True
            logger.info(f"Extracted class information: name={class_name}, is_interface={is_interface}")

        # 构建提示词
        # 从配置文件中获取提示词模板
        prompt_template = self.prompts.get("llm", {}).get("generate_empty_method_prompt", "")

        # 如果模板为空，使用默认提示词
        if not prompt_template:
            prompt_template = "You are an expert in Java development. Generate an empty method implementation based on the following information.\n\n"
            prompt_template += "# Method Information\n"
            prompt_template += "Method Name: ${method_name}\n"
            prompt_template += "Parameters: ${parameters}\n"
            prompt_template += "Return Type: ${return_type}\n"
            prompt_template += "Class Name: ${class_name}\n"
            prompt_template += "Is Interface: ${is_interface}\n\n"
            prompt_template += "# File Context (if available)\n"
            prompt_template += "${file_content}...\n\n"
            prompt_template += "# Requirements\n"
            prompt_template += "1. Generate only the method implementation, no class definition\n"
            prompt_template += "2. Use proper Java syntax\n"
            prompt_template += "3. For methods with return type, return a suitable default value or throw UnsupportedOperationException\n"
            prompt_template += "4. For void methods, leave the body empty\n"
            prompt_template += "5. Include proper parameter names and types\n"
            prompt_template += "6. The method signature must match exactly what would be called by test code\n"
            prompt_template += "7. If the class is an interface, generate only the method declaration without body\n"
            prompt_template += "8. Consider the file context to ensure the method is compatible with the existing class structure\n\n"
            prompt_template += "# Example\n"
            prompt_template += "Input:\n"
            prompt_template += "Method Name: calculateSum\n"
            prompt_template += "Parameters: [{\"name\": \"a\", \"type\": \"int\"}, {\"name\": \"b\", \"type\": \"int\"}]\n"
            prompt_template += "Return Type: int\n"
            prompt_template += "Class Name: Calculator\n"
            prompt_template += "Is Interface: false\n\n"
            prompt_template += "Output:\n"
            prompt_template += "public int calculateSum(int a, int b) {\n"
            prompt_template += "    throw new UnsupportedOperationException(\"Method not implemented yet\");\n"
            prompt_template += "}\n\n"
            prompt_template += "Input:\n"
            prompt_template += "Method Name: printMessage\n"
            prompt_template += "Parameters: [{\"name\": \"message\", \"type\": \"String\"}]\n"
            prompt_template += "Return Type: void\n"
            prompt_template += "Class Name: MessageService\n"
            prompt_template += "Is Interface: true\n\n"
            prompt_template += "Output:\n"
            prompt_template += "void printMessage(String message);\n\n"
            prompt_template += "# Your Output\n"
            prompt_template += "Generate the empty method implementation based on the provided information."

        # 格式化参数信息
        formatted_parameters = json.dumps(parameters, ensure_ascii=False)
        file_context = file_content[:500]

        # 使用Python的string.Template进行变量替换
        import string
        template = string.Template(prompt_template)

        prompt = template.safe_substitute(
            method_name=method_name,
            parameters=formatted_parameters,
            return_type=return_type,
            class_name=class_name,
            is_interface=is_interface,
            file_content=file_context
        )

        logger.info(f"Prompt constructed for empty method generation, length: {len(prompt)}")

        # 打印完整提示词用于调试
        logger.info("=" * 80)
        logger.info("FULL PROMPT SENT TO LLM (EMPTY METHOD GENERATION):")
        logger.info("=" * 80)
        logger.info(prompt)
        logger.info("=" * 80)

        # 使用LangChain调用LLM生成空方法代码
        try:
            # 创建消息列表
            messages = [
                SystemMessage(content="You are an expert in Java development and code generation."),
                HumanMessage(content=prompt)
            ]

            # 调用LLM
            logger.info("Calling LLM for empty method generation with LangChain")
            response = self.llm.invoke(messages)
            empty_method_code = response.content

            logger.info(f"LLM response received, length: {len(empty_method_code)}")
            logger.info(f"Full LLM response:\n{empty_method_code}")

            # 提取代码块
            if empty_method_code.startswith("```java"):
                empty_method_code = empty_method_code[7:]
            elif empty_method_code.startswith("```"):
                empty_method_code = empty_method_code[3:]

            if empty_method_code.endswith("```"):
                empty_method_code = empty_method_code[:-3]

            empty_method_code = empty_method_code.strip()

            logger.info("Empty method code generation completed successfully")
            return empty_method_code

        except Exception as e:
            logger.error(f"Error generating empty method code: {e}")
            logger.error(f"Error type: {type(e).__name__}")
            import traceback
            logger.error(f"Error traceback: {traceback.format_exc()}")
            return "// Failed to generate empty method code"

    def fix_compilation_error(self, error_data: Dict[str, Any]) -> str:
        """修复编译错误 - 使用LangChain"""
        logger.info("Starting fix_compilation_error")

        # 提取错误数据
        code = error_data.get("code", "")
        error_message = error_data.get("error_message", "")
        code_structure = error_data.get("code_structure", "")
        current_class_name = error_data.get("current_class_name", "")
        is_interface_file = error_data.get("is_interface_file", False)

        logger.info(f"Fixing compilation error for class: {current_class_name}")
        logger.info(f"Error message: {error_message}")
        logger.info(f"Code structure provided: {code_structure}")
        logger.info(f"Is interface file: {is_interface_file}")
        logger.info(f"Code length: {len(code)}")

        # 构建提示词
        prompt_template = self.prompts.get("llm", {}).get("fix_compilation_error_prompt", "")

        # 如果模板为空，使用默认提示词
        if not prompt_template:
            prompt_template = "You are an expert in Java development and debugging. Fix the compilation errors in the following test code based on the error message.\n\n"
            prompt_template += "# Test Code with Errors\n"
            prompt_template += "```java\n"
            prompt_template += "${code}\n"
            prompt_template += "```\n\n"
            prompt_template += "# Compilation Error Message\n"
            prompt_template += "${error_message}\n\n"
            prompt_template += "# Code Structure (if available)\n"
            prompt_template += "${code_structure}\n\n"
            prompt_template += "# Class Information\n"
            prompt_template += "Class Name: ${current_class_name}\n"
            prompt_template += "Is Interface: ${is_interface_file}\n\n"
            prompt_template += "# Requirements\n"
            prompt_template += "1. Analyze the compilation error message carefully\n"
            prompt_template += "2. Fix all compilation errors in the test code\n"
            prompt_template += "3. Ensure the fixed code compiles successfully\n"
            prompt_template += "4. Maintain the original functionality of the test code\n"
            prompt_template += "5. Include all necessary import statements\n"
            prompt_template += "6. Return the complete fixed test code\n"
            prompt_template += "7. Do not include any explanations, just the fixed code\n\n"
            prompt_template += "# Your Output\n"
            prompt_template += "Generate the complete fixed test code with all compilation errors resolved."

        # 使用Python的string.Template进行变量替换
        import string
        template = string.Template(prompt_template)

        prompt = template.safe_substitute(
            code=code,
            error_message=error_message,
            code_structure=code_structure,
            current_class_name=current_class_name,
            is_interface_file=is_interface_file
        )

        logger.info(f"Prompt constructed for fixing compilation error, length: {len(prompt)}")

        # 打印完整提示词用于调试
        logger.info("=" * 80)
        logger.info("FULL PROMPT SENT TO LLM (FIX COMPILATION ERROR):")
        logger.info("=" * 80)
        logger.info(prompt)
        logger.info("=" * 80)

        # 使用LangChain调用LLM修复编译错误
        try:
            # 创建消息列表
            messages = [
                SystemMessage(content="You are an expert in Java development, debugging, and JUnit 5."),
                HumanMessage(content=prompt)
            ]

            # 调用LLM
            logger.info("Calling LLM for fixing compilation error with LangChain")
            response = self.llm.invoke(messages)
            fixed_code = response.content

            logger.info(f"LLM response received, length: {len(fixed_code)}")
            logger.info(f"Full LLM response:\n{fixed_code}")

            # 提取代码块
            if fixed_code.startswith("```java"):
                fixed_code = fixed_code[7:]
            elif fixed_code.startswith("```"):
                fixed_code = fixed_code[3:]

            if fixed_code.endswith("```"):
                fixed_code = fixed_code[:-3]

            fixed_code = fixed_code.strip()

            logger.info("Compilation error fixed successfully")
            return fixed_code

        except Exception as e:
            logger.error(f"Error fixing compilation error: {e}")
            logger.error(f"Error type: {type(e).__name__}")
            import traceback
            logger.error(f"Error traceback: {traceback.format_exc()}")
            return "// Failed to fix compilation error"


# 实例化服务
llm_service = LLMService()
