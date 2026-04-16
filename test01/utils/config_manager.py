"""
配置管理模块
集中管理所有测试框架的配置参数
"""

import os
import json
from typing import Dict, Any


class ConfigManager:
    """配置管理器"""

    _instance = None

    def __new__(cls, *args, **kwargs):
        """单例模式"""
        if not cls._instance:
            cls._instance = super(ConfigManager, cls).__new__(cls)
        return cls._instance

    def __init__(self, config_file: str = None):
        """
        初始化配置管理器

        Args:
            config_file: 配置文件路径
        """
        if not hasattr(self, 'initialized'):
            self.initialized = True
            self.config_file = config_file or os.path.join(
                os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                'config.json'
            )
            self.config = self._load_config()
            self._validate_config()

    def _load_config(self) -> Dict[str, Any]:
        """加载配置文件"""
        default_config = {
            # API配置
            "api": {
                "base_url": "http://127.0.0.1:8000",
                "timeout": 60,
                "max_retries": 3
            },

            # GitHub爬取配置
            "github": {
                "repos": [
                    # {
                    #     "name": "commons-math",
                    #     "url": "https://github.com/apache/commons-math.git",
                    #     "description": "Apache Commons Math库"
                    # },
                    # {
                    #     "name": "guava",
                    #     "url": "https://github.com/google/guava.git",
                    #     "description": "Google Guava库"
                    # },
                    # {
                    #     "name": "junit5-samples",
                    #     "url": "https://github.com/junit-team/junit5-samples.git",
                    #     "description": "JUnit 5示例项目"
                    # }
                    {
                        "name": "commons-lang",
                        "url": "https://github.com/apache/commons-lang.git",
                        "description": "Apache Commons Lang库"
                    }
                ],
                "clone_dir": os.path.join(
                    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    'repos'
                ),
                "max_files_per_repo": 100,
                "max_methods_per_class": 10
            },

            # 测试配置
            "test": {
                "max_test_cases": 20,
                "timeout_per_test": 30,
                "max_fix_attempts": 3
            },

            # LLM配置
            "llm": {
                "api_key": os.getenv("DEEPSEEK_API_KEY", ""),
                "base_url": "https://api.deepseek.com/v1",
                "model_name": "deepseek-chat",
                "temperature": 0.7,
                "max_tokens": 1000
            },

            # 输出配置
            "output": {
                "results_dir": os.path.join(
                    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    'results'
                ),
                "logs_dir": os.path.join(
                    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    'logs'
                ),
                "excel_file": "test_results.xlsx"
            },

            # 编译配置
            "compilation": {
                "java_home": os.getenv("JAVA_HOME", ""),
                "classpath": [
                    os.path.join(
                        os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                        'lib', '*.jar'
                    )
                ]
            }
        }

        if os.path.exists(self.config_file):
            try:
                with open(self.config_file, 'r', encoding='utf-8') as f:
                    user_config = json.load(f)
                # 合并配置
                return self._merge_configs(default_config, user_config)
            except Exception as e:
                print(f"Error loading config file: {e}")
                return default_config
        else:
            return default_config

    def _merge_configs(self, default: Dict[str, Any], user: Dict[str, Any]) -> Dict[str, Any]:
        """合并配置"""
        merged = default.copy()
        for key, value in user.items():
            if key in merged and isinstance(merged[key], dict) and isinstance(value, dict):
                merged[key] = self._merge_configs(merged[key], value)
            else:
                merged[key] = value
        return merged

    def _validate_config(self):
        """验证配置"""
        # 确保目录存在
        output_config = self.config.get('output', {})
        os.makedirs(output_config.get('results_dir', 'results'), exist_ok=True)
        os.makedirs(output_config.get('logs_dir', 'logs'), exist_ok=True)

        github_config = self.config.get('github', {})
        os.makedirs(github_config.get('clone_dir', 'repos'), exist_ok=True)

    def get(self, key: str, default: Any = None) -> Any:
        """
        获取配置值

        Args:
            key: 配置键，支持点号分隔（如 "api.base_url"）
            default: 默认值

        Returns:
            配置值
        """
        keys = key.split('.')
        value = self.config

        for k in keys:
            if isinstance(value, dict) and k in value:
                value = value[k]
            else:
                return default

        return value

    def set(self, key: str, value: Any):
        """
        设置配置值

        Args:
            key: 配置键，支持点号分隔（如 "api.base_url"）
            value: 配置值
        """
        keys = key.split('.')
        config = self.config

        for k in keys[:-1]:
            if k not in config:
                config[k] = {}
            config = config[k]

        config[keys[-1]] = value

    def save(self):
        """保存配置到文件"""
        try:
            with open(self.config_file, 'w', encoding='utf-8') as f:
                json.dump(self.config, f, ensure_ascii=False, indent=2)
            return True
        except Exception as e:
            print(f"Error saving config: {e}")
            return False

    def get_api_config(self) -> Dict[str, Any]:
        """获取API配置"""
        return self.config.get('api', {})

    def get_github_config(self) -> Dict[str, Any]:
        """获取GitHub配置"""
        return self.config.get('github', {})

    def get_test_config(self) -> Dict[str, Any]:
        """获取测试配置"""
        return self.config.get('test', {})

    def get_llm_config(self) -> Dict[str, Any]:
        """获取LLM配置"""
        return self.config.get('llm', {})

    def get_output_config(self) -> Dict[str, Any]:
        """获取输出配置"""
        return self.config.get('output', {})

    def get_compilation_config(self) -> Dict[str, Any]:
        """获取编译配置"""
        return self.config.get('compilation', {})


# 创建全局配置管理器实例
config_manager = ConfigManager()
config = config_manager
