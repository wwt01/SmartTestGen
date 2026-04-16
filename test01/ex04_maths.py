"""
准备测试数据：指定JSON文件 → 生成同名Excel
"""

from utils.excel_manager import ExcelManager
import os
import sys
import json
from datetime import datetime

# 添加项目根目录到路径
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# 需注入的核心字段（仅这些字段从JSON提取并写入Excel）
CORE_FIELDS = [
    "id",
    "description",
    "package_name",
    "class_name",
    "is_interface",
    "method_name",
    "parameters",
    "return_type",
    "fields",
    "methods",
    "dependencies",
    "original_code",
    "file_path"
]

# ==============================================
# 【在这里配置你要处理的 文件夹 + 具体 JSON 文件列表】
# ==============================================
CONFIG = {
    # 存放 json 文件的文件夹
    "json_folder": "llm_data",

    # 只处理这里列出的文件（不带路径，只写文件名）
    "target_json_files": [
        "simple_group_select.json"
    ]
}


def load_single_json(json_path: str) -> list:
    """加载单个JSON文件数据，仅保留核心字段"""
    if not os.path.exists(json_path):
        print(f"[ERROR] 文件不存在: {json_path}")
        return []

    try:
        with open(json_path, 'r', encoding='utf-8') as f:
            raw_data = json.load(f)

        processed_data = []
        for item in raw_data if isinstance(raw_data, list) else [raw_data]:
            filtered_item = {k: item.get(k, "") for k in CORE_FIELDS}
            if "methods" in filtered_item and "class_methods" in item:
                filtered_item["methods"] = item.get("class_methods", "")
            processed_data.append(filtered_item)

        print(f"[SUCCESS] 加载 {json_path} → 有效数据条数: {len(processed_data)}")
        return processed_data
    except Exception as e:
        print(f"[ERROR] 读取JSON失败 {json_path}: {str(e)}")
        return []


def process_specified_json_files():
    """只处理指定的 JSON 文件，生成 Excel"""
    print("=" * 60)
    print("Step 1: 处理指定的 JSON 文件 → 生成 Excel")
    print(f"Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 60)

    current_dir = os.path.dirname(os.path.abspath(__file__))
    llm_data_dir = os.path.join(current_dir, CONFIG["json_folder"])
    results_dir = os.path.join(current_dir, "results")
    os.makedirs(results_dir, exist_ok=True)

    target_files = CONFIG["target_json_files"]
    if not target_files:
        print("[ERROR] 未指定任何 JSON 文件")
        return

    total_files = len(target_files)
    for idx, filename in enumerate(target_files, 1):
        print(f"\n[{idx}/{total_files}] 处理文件: {filename}")

        json_path = os.path.join(llm_data_dir, filename)
        base_name = os.path.splitext(filename)[0]
        excel_path = os.path.join(results_dir, f"{base_name}.xlsx")

        test_cases = load_single_json(json_path)
        if not test_cases:
            print(f"[SKIP] {filename} 无有效数据，跳过")
            continue

        excel = ExcelManager(excel_path)
        excel.create()
        excel.load()

        for case in test_cases:
            excel.add_row(case)

        excel.set_column_width()
        excel.save()
        print(f"[SUCCESS] 生成Excel: {excel_path}")
        print(f"[INFO] 写入用例数: {len(test_cases)}")

    print("\n" + "=" * 60)
    print(f"全部完成！共处理 {total_files} 个指定JSON文件")
    print("=" * 60)


if __name__ == "__main__":
    process_specified_json_files()
