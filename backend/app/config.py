import os
from dotenv import load_dotenv

# 加载 .env 环境变量
load_dotenv()


# 项目配置
class Settings:
    # 服务器配置
    HOST: str = os.getenv("HOST", "127.0.0.1")  # 允许外部访问（IDEA 插件可调用）
    PORT: int = int(os.getenv("PORT", 8000))
    DEBUG: bool = os.getenv("DEBUG", "True") == "True"
    
    # DeepSeek LLM配置
    DASHSCOPE_API_KEY: str = os.getenv("DASHSCOPE_API_KEY", "")
    LLM_MODEL: str = os.getenv("LLM_MODEL", "deepseek-chat")
    LLM_API_URL: str = os.getenv("LLM_API_URL", "https://api.deepseek.com/v1")
    LLM_TIMEOUT: int = int(os.getenv("LLM_TIMEOUT", "60"))
    
    # 规则库配置文件路径
    RULES_CONFIG_PATH: str = os.getenv("RULES_CONFIG_PATH", "app/config/rules.json")
    
    # 样例文件路径
    EXAMPLES_CONFIG_PATH: str = os.getenv("EXAMPLES_CONFIG_PATH", "app/config/examples.json")


# 实例化配置，供其他模块调用
settings = Settings()
