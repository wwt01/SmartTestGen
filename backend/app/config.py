import os
from dotenv import load_dotenv

load_dotenv()


class Settings:
    HOST: str = os.getenv("HOST", "127.0.0.1")
    PORT: int = int(os.getenv("PORT", 8000))
    DEBUG: bool = os.getenv("DEBUG", "True") == "True"

    LLM_TIMEOUT: int = int(os.getenv("LLM_TIMEOUT", "60"))

    DASHSCOPE_API_KEY: str = os.getenv("DASHSCOPE_API_KEY", "")
    LLM_MODEL: str = os.getenv("LLM_MODEL", "deepseek-chat")
    LLM_API_URL: str = os.getenv("LLM_API_URL", "https://api.deepseek.com/v1")

    RULES_CONFIG_PATH: str = os.getenv("RULES_CONFIG_PATH", "app/config/rules.json")
    EXAMPLES_CONFIG_PATH: str = os.getenv("EXAMPLES_CONFIG_PATH", "app/config/examples.json")


settings = Settings()
