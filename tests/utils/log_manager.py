"""
日志管理工具类
用于统一日志记录和输出
"""

import os
import logging
from datetime import datetime
# from typing import Optional


class LogManager:
    """日志管理器"""

    _instance = None

    def __new__(cls, *args, **kwargs):
        """单例模式"""
        if not cls._instance:
            cls._instance = super(LogManager, cls).__new__(cls)
        return cls._instance

    def __init__(self, log_dir: str = None, log_level: int = logging.INFO):
        """
        初始化日志管理器

        Args:
            log_dir: 日志文件目录
            log_level: 日志级别
        """
        if not hasattr(self, 'initialized'):
            self.initialized = True
            self.log_dir = log_dir or os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'logs')
            self.log_level = log_level
            self._setup_logger()

    def _setup_logger(self):
        """设置日志记录器"""
        # 确保日志目录存在
        os.makedirs(self.log_dir, exist_ok=True)

        # 日志文件名
        log_file = os.path.join(self.log_dir, f"test_{datetime.now().strftime('%Y%m%d_%H%M%S')}.log")

        # 创建日志记录器
        self.logger = logging.getLogger('SmartTestGen')
        self.logger.setLevel(self.log_level)

        # 清空现有处理器
        if self.logger.handlers:
            for handler in self.logger.handlers:
                self.logger.removeHandler(handler)

        # 创建文件处理器
        file_handler = logging.FileHandler(log_file, encoding='utf-8')
        file_handler.setLevel(self.log_level)

        # 创建控制台处理器
        console_handler = logging.StreamHandler()
        console_handler.setLevel(self.log_level)

        # 设置日志格式
        formatter = logging.Formatter(
            '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
        )
        file_handler.setFormatter(formatter)
        console_handler.setFormatter(formatter)

        # 添加处理器
        self.logger.addHandler(file_handler)
        self.logger.addHandler(console_handler)

        self.logger.info(f"LogManager initialized. Log file: {log_file}")

    def get_logger(self):
        """获取日志记录器"""
        return self.logger

    def info(self, message: str):
        """记录信息日志"""
        self.logger.info(message)

    def warning(self, message: str):
        """记录警告日志"""
        self.logger.warning(message)

    def error(self, message: str):
        """记录错误日志"""
        self.logger.error(message)

    def debug(self, message: str):
        """记录调试日志"""
        self.logger.debug(message)

    def exception(self, message: str, exc_info=True):
        """记录异常日志"""
        self.logger.exception(message, exc_info=exc_info)


# 创建全局日志管理器实例
log_manager = LogManager()
logger = log_manager.get_logger()
