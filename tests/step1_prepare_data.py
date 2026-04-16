"""
步骤1: 准备测试数据
仅从 test_cases_with_requirements.json 加载数据，不手动创建任何用例
"""

from utils.excel_manager import ExcelManager
import os
import sys
import json
from datetime import datetime

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


def load_test_data_from_json():
    """仅从 test_cases_with_requirements.json 加载数据"""
    results_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "results")
    json_path = os.path.join(results_dir, "test_cases_with_requirements.json")

    if not os.path.exists(json_path):
        print(f"[ERROR] 错误：文件不存在 {json_path}")
        return []

    try:
        with open(json_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
        print(f"[SUCCESS] 成功加载 {len(data)} 条测试用例 from test_cases_with_requirements.json")
        return data
    except Exception as e:
        print(f"[ERROR] 读取JSON失败: {e}")
        return []


def create_test_data():
    """从JSON文件创建测试数据Excel，不手动添加任何数据"""
    print("=" * 60)
    print("Step 1: 从 test_cases_with_requirements.json 加载测试数据")
    print(f"Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 60)

    results_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "results")
    os.makedirs(results_dir, exist_ok=True)
    excel_path = os.path.join(results_dir, "test_results.xlsx")

    # 加载数据（仅从JSON）
    test_cases = load_test_data_from_json()

    if not test_cases:
        print("[ERROR] 没有测试数据，退出")
        return None

    # 创建Excel
    excel = ExcelManager(excel_path)
    excel.create()
    excel.load()

    # 写入所有用例（不修改、不增强、不补充）
    for case in test_cases:
        excel.add_row(case)

    excel.save()

    # 输出结果
    print(f"\n[SUCCESS] 测试数据已保存到: {excel_path}")
    print(f"[INFO] 总用例数: {len(test_cases)}")
    print("=" * 60)
    print("Step1 完成！仅使用JSON文件数据 [SUCCESS]")
    print("=" * 60)

    return excel_path


if __name__ == "__main__":
    create_test_data()
