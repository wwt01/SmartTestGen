import re
import json
from typing import Dict, Any
from app.config import settings


class PreprocessingService:
    """文本预处理服务"""

    def __init__(self):
        """初始化预处理服务，加载规则库"""
        self.rules = self._load_rules()

    def _load_rules(self) -> Dict[str, Any]:
        """加载规则库配置文件"""
        try:
            with open(settings.RULES_CONFIG_PATH, 'r', encoding='utf-8') as f:
                return json.load(f)
        except Exception as e:
            print(f"Error loading rules: {e}")
            return {"chinese": {}, "english": {}}

    def _detect_language(self, text: str) -> str:
        """检测文本语言（中文或英文）"""
        chinese_chars = len(re.findall(r'[\u4e00-\u9fff]', text))
        english_chars = len(re.findall(r'[a-zA-Z]', text))

        if chinese_chars > english_chars:
            return "chinese"
        else:
            return "english"

    def clean_text(self, text: str) -> str:
        """清理文本中的乱码、特殊符号等干扰信息"""
        # 移除乱码和特殊符号
        cleaned = re.sub(r'[\x00-\x1f\x7f]', '', text)
        # 移除编程语言注释标记（如 //）
        cleaned = re.sub(r'^//\s*', '', cleaned)
        # 移除多余的空白字符
        cleaned = re.sub(r'\s+', ' ', cleaned).strip()
        return cleaned

    def extract_weak_features(self, text: str) -> Dict[str, Any]:
        """使用规则库提取弱特征"""
        # 检测语言
        language = self._detect_language(text)
        rules = self.rules.get(language, {})

        weak_features = {
            "method_name": None,
            "parameters": [],
            "parameter_types": {},
            "return_type": None,
            "constraints": [],
            "expectations": []
        }

        # 提取方法名
        for pattern in rules.get("method_name", []):
            matches = re.finditer(pattern, text, re.IGNORECASE)
            for match in matches:
                if len(match.groups()) > 0:
                    method_name = match.group(1)
                    weak_features["method_name"] = method_name
                    break

        # 提取参数
        for pattern in rules.get("parameters", []):
            matches = re.finditer(pattern, text, re.IGNORECASE)
            for match in matches:
                if len(match.groups()) > 0:
                    param_name = match.group(1)
                    if param_name not in weak_features["parameters"]:
                        weak_features["parameters"].append(param_name)

        # 提取参数类型
        for pattern in rules.get("parameter_types", []):
            matches = re.finditer(pattern, text, re.IGNORECASE)
            for match in matches:
                if len(match.groups()) >= 2:
                    param_name = match.group(1)
                    param_type = match.group(2)
                    weak_features["parameter_types"][param_name] = param_type

        # 提取返回类型
        for pattern in rules.get("return_type", []):
            matches = re.finditer(pattern, text, re.IGNORECASE)
            for match in matches:
                if len(match.groups()) > 0:
                    return_type = match.group(1)
                    weak_features["return_type"] = return_type
                    break

        # 提取约束条件
        for pattern in rules.get("constraints", []):
            matches = re.finditer(pattern, text, re.IGNORECASE)
            for match in matches:
                if len(match.groups()) >= 1:
                    param_name = match.group(1)
                    constraint_value = ' '.join(match.groups()[1:]) if len(match.groups()) > 1 else match.group(0)
                    weak_features["constraints"].append({
                        "parameter": param_name,
                        "constraint": constraint_value
                    })

        # 提取预期结果
        for pattern in rules.get("expectations", []):
            matches = re.finditer(pattern, text, re.IGNORECASE)
            for match in matches:
                if len(match.groups()) > 0:
                    expectation = match.group(1).strip()
                    weak_features["expectations"].append(expectation)

        return weak_features

    def preprocess(self, text: str) -> Dict[str, Any]:
        """完整的预处理流程"""
        # 1. 清理文本
        cleaned_text = self.clean_text(text)

        # 2. 提取弱特征
        weak_features = self.extract_weak_features(cleaned_text)

        return {
            "original_text": text,
            "cleaned_text": cleaned_text,
            "weak_features": weak_features
        }


# 实例化服务
preprocessing_service = PreprocessingService()
