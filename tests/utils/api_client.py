"""
API调用工具类
用于调用后端API接口
"""

import requests
import time
from typing import Dict, Any, Optional


class APIClient:
    """API客户端"""
    
    def __init__(self, base_url: str = "http://127.0.0.1:8000"):
        self.base_url = base_url
        self.session = requests.Session()
        
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
        
        start_time = time.time()
        
        try:
            response = self.session.post(
                url,
                json={"content": text},
                timeout=120
            )
            
            elapsed_ms = int((time.time() - start_time) * 1000)
            
            if response.status_code == 200:
                result = response.json()
                return {
                    "success": True,
                    "data": result.get("data", {}),
                    "time_ms": elapsed_ms,
                    "error": None
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
        except Exception as e:
            elapsed_ms = int((time.time() - start_time) * 1000)
            return {
                "success": False,
                "data": {},
                "time_ms": elapsed_ms,
                "error": str(e)
            }
    
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
        
        start_time = time.time()
        
        try:
            response = self.session.post(
                url,
                json=session_data,
                timeout=60
            )
            
            elapsed_ms = int((time.time() - start_time) * 1000)
            
            if response.status_code == 200:
                result = response.json()
                return {
                    "success": True,
                    "session_id": result.get("data", {}).get("session_id", ""),
                    "time_ms": elapsed_ms,
                    "error": None
                }
            else:
                return {
                    "success": False,
                    "session_id": "",
                    "time_ms": elapsed_ms,
                    "error": f"HTTP {response.status_code}: {response.text}"
                }
                
        except Exception as e:
            elapsed_ms = int((time.time() - start_time) * 1000)
            return {
                "success": False,
                "session_id": "",
                "time_ms": elapsed_ms,
                "error": str(e)
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
        
        start_time = time.time()
        
        try:
            response = self.session.post(
                url,
                json=request_data,
                timeout=180
            )
            
            elapsed_ms = int((time.time() - start_time) * 1000)
            
            if response.status_code == 200:
                result = response.json()
                data = result.get("data", {})
                return {
                    "success": True,
                    "test_code": data.get("test_code", ""),
                    "empty_method": data.get("empty_method", ""),
                    "time_ms": elapsed_ms,
                    "error": None
                }
            else:
                return {
                    "success": False,
                    "test_code": "",
                    "empty_method": "",
                    "time_ms": elapsed_ms,
                    "error": f"HTTP {response.status_code}: {response.text}"
                }
                
        except requests.exceptions.Timeout:
            elapsed_ms = int((time.time() - start_time) * 1000)
            return {
                "success": False,
                "test_code": "",
                "empty_method": "",
                "time_ms": elapsed_ms,
                "error": "Request timeout"
            }
        except Exception as e:
            elapsed_ms = int((time.time() - start_time) * 1000)
            return {
                "success": False,
                "test_code": "",
                "empty_method": "",
                "time_ms": elapsed_ms,
                "error": str(e)
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
        
        start_time = time.time()
        
        try:
            response = self.session.post(
                url,
                json={
                    "code": request_data.get("test_code", ""),
                    "error_message": request_data.get("compilation_error", ""),
                    "session_id": request_data.get("session_id", "")
                },
                timeout=180
            )
            
            elapsed_ms = int((time.time() - start_time) * 1000)
            
            if response.status_code == 200:
                result = response.json()
                data = result.get("data", {})
                return {
                    "success": True,
                    "fixed_code": data.get("test_code", ""),
                    "time_ms": elapsed_ms,
                    "error": None
                }
            else:
                return {
                    "success": False,
                    "fixed_code": "",
                    "time_ms": elapsed_ms,
                    "error": f"HTTP {response.status_code}: {response.text}"
                }
                
        except requests.exceptions.Timeout:
            elapsed_ms = int((time.time() - start_time) * 1000)
            return {
                "success": False,
                "fixed_code": "",
                "time_ms": elapsed_ms,
                "error": "Request timeout"
            }
        except Exception as e:
            elapsed_ms = int((time.time() - start_time) * 1000)
            return {
                "success": False,
                "fixed_code": "",
                "time_ms": elapsed_ms,
                "error": str(e)
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
