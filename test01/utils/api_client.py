"""
API调用工具类
用于调用后端API接口
"""

import requests
import time
import json
from typing import Dict, Any


class APIClient:
    """API客户端"""

    def __init__(self, base_url: str = "http://127.0.0.1:8000"):
        self.base_url = base_url
        self.session = requests.Session()
        self.session.headers.update({"Content-Type": "application/json"})

    def _make_request(self, url: str, data: Dict[str, Any], timeout: int = 60) -> Dict[str, Any]:
        """
        通用请求方法

        Args:
            url: 请求URL
            data: 请求数据
            timeout: 超时时间

        Returns:
            请求结果
        """
        start_time = time.time()

        try:
            response = self.session.post(
                url,
                json=data,
                timeout=timeout
            )

            elapsed_ms = int((time.time() - start_time) * 1000)

            if response.status_code == 200:
                try:
                    result = response.json()
                    return {
                        "success": True,
                        "data": result.get("data", {}),
                        "time_ms": elapsed_ms,
                        "error": None
                    }
                except json.JSONDecodeError:
                    return {
                        "success": False,
                        "data": {},
                        "time_ms": elapsed_ms,
                        "error": "Invalid JSON response"
                    }
            else:
                return {
                    "success": False,
                    "data": {},
                    "time_ms": elapsed_ms,
                    "error": f"HTTP {response.status_code}: {response.text}"
                }

        except requests.exceptions.Timeout:
            elapsed_ms = int((time.time() - start_time) * 1000)
            return {
                "success": False,
                "data": {},
                "time_ms": elapsed_ms,
                "error": "Request timeout"
            }
        except requests.exceptions.ConnectionError:
            elapsed_ms = int((time.time() - start_time) * 1000)
            return {
                "success": False,
                "data": {},
                "time_ms": elapsed_ms,
                "error": "Connection error. Please check if the API server is running."
            }
        except Exception as e:
            elapsed_ms = int((time.time() - start_time) * 1000)
            return {
                "success": False,
                "data": {},
                "time_ms": elapsed_ms,
                "error": str(e)
            }

    def parse_text(self, text: str) -> Dict[str, Any]:
        """
        调用接口1: 解析需求文本

        Args:
            text: 需求文本

        Returns:
            {
                "success": bool,
                "data": dict,
                "time_ms": int,
                "error": str
            }
        """
        url = f"{self.base_url}/api/text/parse"
        return self._make_request(url, {"content": text}, timeout=120)

    def init_session(self, session_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        调用初始化会话接口

        Args:
            session_data: 会话初始化数据

        Returns:
            {
                "success": bool,
                "session_id": str,
                "time_ms": int,
                "error": str
            }
        """
        url = f"{self.base_url}/api/text/init-session"
        result = self._make_request(url, session_data, timeout=60)

        if result["success"]:
            return {
                "success": True,
                "session_id": result["data"].get("session_id", ""),
                "time_ms": result["time_ms"],
                "error": None
            }
        else:
            return {
                "success": False,
                "session_id": "",
                "time_ms": result["time_ms"],
                "error": result["error"]
            }

    def generate_test_code(self, request_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        调用接口2: 生成测试代码

        Args:
            request_data: 请求数据

        Returns:
            {
                "success": bool,
                "test_code": str,
                "empty_method": str,
                "time_ms": int,
                "error": str
            }
        """
        url = f"{self.base_url}/api/text/generate-test"
        result = self._make_request(url, request_data, timeout=180)

        if result["success"]:
            data = result["data"]
            return {
                "success": True,
                "test_code": data.get("test_code", ""),
                "empty_method": data.get("empty_method", ""),
                "time_ms": result["time_ms"],
                "error": None
            }
        else:
            return {
                "success": False,
                "test_code": "",
                "empty_method": "",
                "time_ms": result["time_ms"],
                "error": result["error"]
            }

    def fix_compilation_error(self, request_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        调用接口3: 修复编译错误

        Args:
            request_data: 请求数据

        Returns:
            {
                "success": bool,
                "fixed_code": str,
                "time_ms": int,
                "error": str
            }
        """
        url = f"{self.base_url}/api/text/fix-compilation-error"

        # 简化请求数据，只传入必要的字段
        fix_data = {
            "code": request_data.get("test_code", ""),
            "error_message": request_data.get("compilation_error", ""),
            "session_id": request_data.get("session_id", ""),
            "method_source": request_data.get("method_source", "No information available yet.")
        }

        result = self._make_request(url, fix_data, timeout=180)

        if result["success"]:
            data = result["data"]
            return {
                "success": True,
                "fixed_code": data.get("test_code", ""),
                "time_ms": result["time_ms"],
                "error": None
            }
        else:
            return {
                "success": False,
                "fixed_code": "",
                "time_ms": result["time_ms"],
                "error": result["error"]
            }

    def health_check(self) -> bool:
        """检查API服务是否可用"""
        try:
            response = self.session.get(
                f"{self.base_url}/docs",
                timeout=5
            )
            return response.status_code == 200
        except Exception:
            return False

    def wait_for_service(self, max_retries: int = 30, interval: float = 2.0) -> bool:
        """
        等待API服务可用

        Args:
            max_retries: 最大重试次数
            interval: 重试间隔（秒）

        Returns:
            服务是否可用
        """
        print("Waiting for API service to be available...")

        for i in range(max_retries):
            if self.health_check():
                print("✅ API service is available!")
                return True
            print(f"Attempt {i + 1}/{max_retries}: Service not available yet...")
            time.sleep(interval)

        print("❌ API service is not available after maximum retries")
        return False
