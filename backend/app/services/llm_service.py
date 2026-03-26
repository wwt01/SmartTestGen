import json
import logging
import yaml
import os
from typing import Dict, Any, List
from langchain_openai import ChatOpenAI
from langchain_core.output_parsers import JsonOutputParser
from langchain_core.messages import SystemMessage, HumanMessage, BaseMessage
from langchain_core.runnables.history import RunnableWithMessageHistory
from langchain_core.chat_history import BaseChatMessageHistory
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder, FewShotChatMessagePromptTemplate
from pydantic import BaseModel, Field
from app.config import settings

logging.basicConfig(
    level=logging.INFO,
    format='[SmartTestGen] %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class InMemoryHistory(BaseChatMessageHistory, BaseModel):
    """In memory implementation of chat message history."""
    messages: List[BaseMessage] = Field(default_factory=list)
    static_context: Dict[str, Any] = Field(default_factory=dict)

    def add_messages(self, messages: List[BaseMessage]) -> None:
        """Add a list of messages to the store"""
        self.messages.extend(messages)
        if len(self.messages) > 6:
            self.messages = self.messages[-6:]

    def clear(self) -> None:
        self.messages = []
        self.static_context = {}


class LLMService:
    """LLM调用服务 - 使用LangChain框架"""

    def __init__(self):
        """初始化LLM服务，加载样例文件和提示词配置"""
        self.examples = self._load_examples()
        self.prompts = self._load_prompts()

        # 初始化会话存储
        self.store: Dict[str, InMemoryHistory] = {}

        # 根据配置选择本地模型或云端模型
        if settings.USE_LOCAL_LLM:
            logger.info("Using local LLM (Ollama)")
            logger.info(f"Local LLM URL: {settings.LOCAL_LLM_URL}")
            logger.info(f"Local LLM Model: {settings.LOCAL_LLM_MODEL}")

            # 本地模型不需要真实API Key，但LangChain要求必须提供
            self.llm = ChatOpenAI(
                api_key="ollama",
                base_url=settings.LOCAL_LLM_URL,
                model=settings.LOCAL_LLM_MODEL,
                temperature=0.3,
                max_tokens=2000,
                top_p=0.9,
                timeout=settings.LLM_TIMEOUT
            )
        else:
            logger.info("Using cloud LLM (DeepSeek/Qwen)")
            logger.info(f"Cloud LLM URL: {settings.LLM_API_URL}")
            logger.info(f"Cloud LLM Model: {settings.LLM_MODEL}")

            # 云端模型需要API Key
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

    def get_session_history(self, session_id: str) -> BaseChatMessageHistory:
        if session_id not in self.store:
            self.store[session_id] = InMemoryHistory()
        return self.store[session_id]

    def init_session(self, context_data: Dict[str, Any]) -> str:
        """初始化会话，存储静态上下文信息，返回session_id"""
        import uuid
        session_id = str(uuid.uuid4())
        
        fields = context_data.get("fields", [])
        methods = context_data.get("methods", [])
        dependencies = context_data.get("dependencies", [])
        
        logger.info(f"初始化数据: 字段数: {len(fields)} | 方法数: {len(methods)} | 依赖数: {len(dependencies)}")
        
        fields, methods = self.filter_private_members(fields, methods)
        dependencies = self.filter_relevant_dependencies(dependencies, methods, fields)
        
        logger.info(f"过滤后存储: 字段数: {len(fields)} | 方法数: {len(methods)} | 依赖数: {len(dependencies)}")
        
        history = self.get_session_history(session_id)
        history.static_context = {
            "class_name": context_data.get("class_name", ""),
            "is_interface": context_data.get("is_interface", False),
            "package_name": context_data.get("package_name", ""),
            "class_type": context_data.get("class_type", "Unknown"),
            "fields": fields,
            "methods": methods,
            "dependencies": dependencies
        }
        
        logger.info("=" * 60)
        logger.info("会话初始化成功")
        logger.info(f"Session ID: {session_id}")
        logger.info(f"类名: {context_data.get('class_name')} | 类型: {context_data.get('class_type')}")
        logger.info("=" * 60)
        
        return session_id

    def get_static_context(self, session_id: str) -> Dict[str, Any]:
        """获取会话的静态上下文"""
        if session_id not in self.store:
            return {}
        return self.store[session_id].static_context

    def filter_private_members(self, fields: List[Dict], methods: List[Dict]) -> tuple:
        """过滤private成员，只保留public/protected"""
        filtered_fields = [
            f for f in fields 
            if f.get('visibility', 'public') in ['public', 'protected']
        ]
        filtered_methods = [
            m for m in methods 
            if m.get('visibility', 'public') in ['public', 'protected']
        ]
        return filtered_fields, filtered_methods

    def filter_relevant_methods(self, methods: List[Dict], target_method_name: str, class_name: str = "") -> List[Dict]:
        """只保留相关方法：被测试方法、构造函数、getter/setter、以及少量其他public方法"""
        relevant = []
        for m in methods:
            name = m.get('name', '')
            
            if name == target_method_name:
                relevant.append(m)
                continue
            
            if name in ['<init>', 'constructor'] or name == class_name:
                relevant.append(m)
                continue
            
            if name.startswith('get') or name.startswith('set') or name.startswith('is'):
                relevant.append(m)
                continue
            
            if len(relevant) < 8:
                relevant.append(m)
        
        return relevant

    def filter_relevant_dependencies(self, dependencies: List[str], methods: List[Dict], fields: List[Dict]) -> List[str]:
        """只保留在相关方法和字段中使用的依赖"""
        used_types = set()
        
        for m in methods:
            for p in m.get('parameters', []):
                param_type = p.get('type', '')
                if param_type:
                    used_types.add(param_type)
            return_type = m.get('return_type', '')
            if return_type:
                used_types.add(return_type)
        
        for f in fields:
            field_type = f.get('type', '')
            if field_type:
                used_types.add(field_type)
        
        relevant_deps = []
        for dep in dependencies:
            if any(dep in used_type or used_type.endswith(dep) for used_type in used_types):
                relevant_deps.append(dep)
        
        return relevant_deps

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

    def _prepare_few_shot_examples(self, preprocessing_result: Dict[str, Any], limit: int = 6) -> List[Dict[str, str]]:
        """准备Few-Shot示例，格式为[{input: natural_language, output: structured_result}]"""
        # 直接使用预处理结果中的语言字段
        language = preprocessing_result.get("language", "english")
        examples = self.examples.get(language, [])

        if not examples:
            return []

        # 只取前limit个样例
        examples_to_use = examples[:limit]

        few_shot_examples = []
        for example in examples_to_use:
            natural_lang = example.get("natural_language", "")
            structured = example.get("structured_result", {})

            # 将structured_result转换为JSON字符串
            output_str = json.dumps(structured, ensure_ascii=False, indent=2)

            few_shot_examples.append({
                "input": natural_lang,
                "output": output_str
            })

        return few_shot_examples

    def get_structured_result(self, preprocessing_result: Dict[str, Any]) -> Dict[str, Any]:
        """获取结构化解析结果 - 使用LangChain和FewShotChatMessagePromptTemplate"""
        logger.info("=" * 60)
        logger.info("开始解析需求文本")
        logger.info("=" * 60)

        system_prompt_template = self.prompts.get("llm", {}).get("parse_requirement_system_prompt", "")
        user_prompt_template = self.prompts.get("llm", {}).get("parse_requirement_user_prompt", "")

        if not system_prompt_template:
            system_prompt_template = "You are an expert in Java testing. Parse the following Java requirement into structured test case information."

        if not user_prompt_template:
            user_prompt_template = "# Requirement\n{cleaned_text}\n\n# Output JSON\n{{\n  \"method_name\": \"string\",\n  \"parameters\": [],\n  \"return_type\": \"string\",\n  \"expectations\": []\n}}"

        few_shot_examples = self._prepare_few_shot_examples(preprocessing_result, limit=6)
        logger.info(f"加载 {len(few_shot_examples)} 个示例")

        example_prompt = ChatPromptTemplate.from_messages([
            ("human", "{input}"),
            ("ai", "{output}")
        ])

        few_shot_prompt = FewShotChatMessagePromptTemplate(
            example_prompt=example_prompt,
            examples=few_shot_examples,
        )

        final_prompt = ChatPromptTemplate.from_messages([
            ("system", system_prompt_template),
            few_shot_prompt,
            ("human", user_prompt_template)
        ])

        cleaned_text = preprocessing_result["cleaned_text"]

        try:
            logger.info("调用 LLM 解析需求...")
            chain = final_prompt | self.llm
            response = chain.invoke({"cleaned_text": cleaned_text})

            content = response.content

            if content.startswith("```json"):
                content = content[7:]
            elif content.startswith("```"):
                content = content[3:]

            if content.endswith("```"):
                content = content[:-3]

            content = content.strip()

            structured_data = json.loads(content)

            logger.info("=" * 60)
            logger.info("需求解析成功")
            logger.info(f"方法名: {structured_data.get('method_name')}")
            logger.info(f"参数数量: {len(structured_data.get('parameters', []))}")
            logger.info(f"返回类型: {structured_data.get('return_type')}")
            logger.info("=" * 60)
            return structured_data

        except Exception as e:
            logger.error(f"解析需求失败: {str(e)}")
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
        """根据结构化信息生成 Java 单元测试代码 - 使用LangChain和ChatPromptTemplate"""
        logger.info("=" * 60)
        logger.info("开始生成测试代码")
        logger.info("=" * 60)

        session_id = structured_data.get("session_id")
        if not session_id:
            logger.error("缺少 session_id")
            raise ValueError("session_id is required")

        static_context = self.get_static_context(session_id)
        if not static_context:
            logger.error(f"会话不存在: {session_id}")
            raise ValueError(f"Session not found: {session_id}, please call init-session first")

        method_name = structured_data.get("method_name", "")
        parameters = structured_data.get("parameters", [])
        return_type = structured_data.get("return_type", "")
        expectations = structured_data.get("expectations", [])

        class_name = static_context.get("class_name", "Example")
        is_interface = static_context.get("is_interface", False)
        package_name = static_context.get("package_name", "")
        class_type = static_context.get("class_type", "Unknown")
        fields = static_context.get("fields", [])
        methods = static_context.get("methods", [])
        dependencies = static_context.get("dependencies", [])

        logger.info(f"目标方法: {method_name}")
        logger.info(f"参数数量: {len(parameters)} | 返回类型: {return_type}")
        logger.info(f"类名: {class_name} | 类型: {class_type} | 接口: {is_interface}")
        logger.info(f"上下文数据: 字段数: {len(fields)} | 方法数: {len(methods)} | 依赖数: {len(dependencies)}")

        if is_interface:
            system_template = self.prompts.get("llm", {}).get("generate_test_system_prompt_interface", "")
            user_template = self.prompts.get("llm", {}).get("generate_test_user_prompt_interface", "")
        else:
            system_template = self.prompts.get("llm", {}).get("generate_test_system_prompt", "")
            user_template = self.prompts.get("llm", {}).get("generate_test_user_prompt", "")

        if not system_template:
            system_template = "You are an expert in Java software testing and JUnit 5."

        if not user_template:
            user_template = """# Test Requirements
- Method name: {method_name}
- Parameters: {parameters}
- Return type: {return_type}
- Expectations: {expectations}

Generate the complete Java unit test code."""

        formatted_parameters = json.dumps(parameters, ensure_ascii=False, indent=2)
        formatted_expectations = json.dumps(expectations, ensure_ascii=False, indent=2)
        formatted_fields = json.dumps(fields, ensure_ascii=False, indent=2)
        formatted_methods = json.dumps(methods, ensure_ascii=False, indent=2)
        formatted_dependencies = "\n".join([f"- {dep}" for dep in dependencies]) if dependencies else "No dependencies"

        variables = {
            "class_name": class_name,
            "method_name": method_name,
            "MethodName": method_name.capitalize() if method_name else "",
            "parameters": formatted_parameters,
            "return_type": return_type,
            "expectations": formatted_expectations,
            "package_name": package_name,
            "class_type": class_type,
            "fields": formatted_fields,
            "methods": formatted_methods,
            "dependencies": formatted_dependencies
        }

        try:
            logger.info("调用 LLM 生成测试代码...")

            if session_id:
                prompt_with_history = ChatPromptTemplate.from_messages([
                    ("system", system_template),
                    MessagesPlaceholder(variable_name="history"),
                    ("human", user_template)
                ])

                chain = prompt_with_history | self.llm

                with_message_history = RunnableWithMessageHistory(
                    chain,
                    self.get_session_history,
                    input_messages_key="input",
                    history_messages_key="history",
                )

                response = with_message_history.invoke(
                    variables,
                    config={"configurable": {"session_id": session_id}}
                )
            else:
                prompt = ChatPromptTemplate.from_messages([
                    ("system", system_template),
                    ("human", user_template)
                ])

                chain = prompt | self.llm
                response = chain.invoke(variables)

            test_code = response.content

            if test_code.startswith("```java"):
                test_code = test_code[7:]
            elif test_code.startswith("```"):
                test_code = test_code[3:]

            if test_code.endswith("```"):
                test_code = test_code[:-3]

            test_code = test_code.strip()

            logger.info("=" * 60)
            logger.info("测试代码生成成功")
            logger.info(f"代码长度: {len(test_code)} 字符")
            logger.info("=" * 60)
            return test_code

        except Exception as e:
            logger.error(f"生成测试代码失败: {str(e)}")
            import traceback
            logger.error(f"错误堆栈: {traceback.format_exc()}")
            return "// Failed to generate test code"

    def generate_empty_method(self, structured_data: Dict[str, Any]) -> str:
        """根据结构化信息生成 Java 空方法代码 - 使用字符串拼接"""
        logger.info("Starting generate_empty_method")

        # 提取结构化信息
        method_name = structured_data.get("method_name", "")
        parameters = structured_data.get("parameters", [])
        return_type = structured_data.get("return_type", "void")
        file_content = structured_data.get("file_content", "")

        logger.info(f"Generating empty method code for: {method_name}")
        logger.info(f"Parameters: {parameters}")
        logger.info(f"Return type: {return_type}")

        # 分析文件内容，判断是否为接口
        is_interface = False
        if file_content:
            import re
            interface_match = re.search(r'interface\s+(\w+)\s*[\{]', file_content)
            if interface_match:
                is_interface = True
                logger.info("Detected interface from file content")

        try:
            # 构建参数列表字符串
            param_list = []
            for param in parameters:
                param_name = param.get("name", "param")
                param_type = param.get("type", "Object")
                param_list.append(f"{param_type} {param_name}")

            params_str = ", ".join(param_list)

            # 如果是接口，只生成方法声明
            if is_interface:
                method_code = f"{return_type} {method_name}({params_str});"
                logger.info(f"Generated interface method declaration: {method_code}")
                return method_code

            # 根据返回类型生成默认返回值
            default_value = self._get_default_return_value(return_type)

            # 构建方法体
            if return_type == "void":
                method_body = "    // Implementation to be added"
            else:
                method_body = f"    {default_value}"

            # 拼接完整方法
            method_code = f"public {return_type} {method_name}({params_str}) {{\n{method_body}\n}}"

            logger.info("Empty method code generation completed successfully")
            logger.info(f"Generated method: {method_code}")
            return method_code

        except Exception as e:
            logger.error(f"Error generating empty method code: {e}")
            import traceback
            logger.error(f"Error traceback: {traceback.format_exc()}")
            return "// Failed to generate empty method code"

    def _get_default_return_value(self, return_type: str) -> str:
        """根据返回类型获取默认返回值"""
        default_values = {
            "void": "// Implementation to be added",
            "int": "return 0;",
            "long": "return 0L;",
            "short": "return 0;",
            "byte": "return 0;",
            "float": "return 0.0f;",
            "double": "return 0.0;",
            "boolean": "return false;",
            "char": "return '\\0';",
            "Integer": "return null;",
            "Long": "return null;",
            "Short": "return null;",
            "Byte": "return null;",
            "Float": "return null;",
            "Double": "return null;",
            "Boolean": "return null;",
            "Character": "return null;",
            "String": "return \"\";",
        }

        if return_type in default_values:
            return default_values[return_type]
        else:
            return "throw new UnsupportedOperationException(\"Method not implemented yet\");"

    def fix_compilation_error(self, error_data: Dict[str, Any]) -> str:
        """修复编译错误 - 使用LangChain"""
        logger.info("Starting fix_compilation_error")

        session_id = error_data.get("session_id")
        if not session_id:
            logger.error("session_id is required for fix_compilation_error")
            raise ValueError("session_id is required")

        static_context = self.get_static_context(session_id)
        if not static_context:
            logger.error(f"No static context found for session_id: {session_id}")
            raise ValueError(f"Session not found: {session_id}, please call init-session first")

        code = error_data.get("code", "")
        error_message = error_data.get("error_message", "")

        current_class_name = static_context.get("class_name", "")
        is_interface_file = static_context.get("is_interface", False)
        package_name = static_context.get("package_name", "")
        class_type = static_context.get("class_type", "Unknown")
        fields = static_context.get("fields", [])
        methods = static_context.get("methods", [])
        dependencies = static_context.get("dependencies", [])

        logger.info(f"Fixing compilation error for class: {current_class_name}")
        logger.info(f"Error message: {error_message}")
        logger.info(f"Package: {package_name}, Class type: {class_type}")
        logger.info(f"Is interface file: {is_interface_file}")
        logger.info(f"Code length: {len(code)}")
        logger.info(f"上下文数据: 字段数: {len(fields)} | 方法数: {len(methods)} | 依赖数: {len(dependencies)}")

        formatted_fields = json.dumps(fields, ensure_ascii=False, indent=2)
        formatted_methods = json.dumps(methods, ensure_ascii=False, indent=2)
        formatted_dependencies = "\n".join([f"- {dep}" for dep in dependencies]) if dependencies else "No dependencies"

        system_prompt = self.prompts.get("llm", {}).get("fix_compilation_error_system_prompt", "")
        user_prompt = self.prompts.get("llm", {}).get("fix_compilation_error_user_prompt", "")

        if not system_prompt:
            system_prompt = "You are an expert in Java development, debugging, and JUnit 5. Your task is to fix the compilation errors in the provided test code based on the error message."
        if not user_prompt:
            user_prompt = "# Test Code with Errors\n```java\n{code}\n```\n\n# Compilation Error Message\n{error_message}\n\nGenerate the complete fixed test code with all compilation errors resolved."

        try:
            logger.info("Calling LLM for fixing compilation error with LangChain")
            
            variables = {
                "code": code,
                "error_message": error_message,
                "current_class_name": current_class_name,
                "is_interface_file": is_interface_file,
                "package_name": package_name,
                "class_type": class_type,
                "fields": formatted_fields,
                "methods": formatted_methods,
                "dependencies": formatted_dependencies
            }
            
            if session_id:
                logger.info(f"Using session memory for session_id: {session_id}")

                prompt_template_obj = ChatPromptTemplate.from_messages([
                    ("system", system_prompt),
                    MessagesPlaceholder(variable_name="history"),
                    ("human", user_prompt)
                ])

                chain = prompt_template_obj | self.llm

                with_message_history = RunnableWithMessageHistory(
                    chain,
                    self.get_session_history,
                    input_messages_key="input",
                    history_messages_key="history",
                )

                response = with_message_history.invoke(
                    variables,
                    config={"configurable": {"session_id": session_id}}
                )
            else:
                prompt_template_obj = ChatPromptTemplate.from_messages([
                    ("system", system_prompt),
                    ("human", user_prompt)
                ])

                chain = prompt_template_obj | self.llm

                response = chain.invoke(variables)
            
            fixed_code = response.content

            logger.info(f"LLM response received, length: {len(fixed_code)}")
            logger.info(f"Full LLM response:\n{fixed_code}")

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
