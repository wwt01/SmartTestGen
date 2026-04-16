"""
准备测试数据：遍历llm_data文件夹JSON文件，生成同名Excel
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
    "methods",  # 新增：方法信息
    "dependencies",
    "original_code",
    "file_path"  # 新增：文件绝对路径
]


def load_single_json(json_path: str) -> list:
    """加载单个JSON文件数据，仅保留核心字段"""
    if not os.path.exists(json_path):
        print(f"[ERROR] 文件不存在: {json_path}")
        return []

    try:
        with open(json_path, 'r', encoding='utf-8') as f:
            raw_data = json.load(f)

        # 确保数据是列表格式，且仅保留核心字段
        processed_data = []
        for item in raw_data if isinstance(raw_data, list) else [raw_data]:
            filtered_item = {k: item.get(k, "") for k in CORE_FIELDS}
            # 处理methods字段：从class_methods获取值
            if "methods" in filtered_item and "class_methods" in item:
                filtered_item["methods"] = item.get("class_methods", "")
            processed_data.append(filtered_item)

        print(f"[SUCCESS] 加载 {json_path} → 有效数据条数: {len(processed_data)}")
        return processed_data
    except Exception as e:
        print(f"[ERROR] 读取JSON失败 {json_path}: {str(e)}")
        return []


def process_llm_data_folder():
    """遍历llm_data文件夹，为每个JSON生成同名Excel"""
    print("=" * 60)
    print("Step 1: 遍历llm_data文件夹，处理所有JSON文件")
    print(f"Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 60)

    # 定义路径
    current_dir = os.path.dirname(os.path.abspath(__file__))
    llm_data_dir = os.path.join(current_dir, "llm_data")  # JSON源文件夹
    results_dir = os.path.join(current_dir, "results")     # Excel输出文件夹
    os.makedirs(llm_data_dir, exist_ok=True)
    os.makedirs(results_dir, exist_ok=True)

    # 获取llm_data下所有JSON文件
    json_files = [f for f in os.listdir(llm_data_dir) if f.endswith(".json")]
    if not json_files:
        print("[ERROR] llm_data文件夹下无JSON文件，退出")
        return

    # 逐个处理JSON文件
    total_files = len(json_files)
    for idx, json_filename in enumerate(json_files, 1):
        print(f"\n[{idx}/{total_files}] 处理文件: {json_filename}")

        # 构建路径
        json_basename = os.path.splitext(json_filename)[0]  # 去除后缀的文件名
        json_path = os.path.join(llm_data_dir, json_filename)
        excel_path = os.path.join(results_dir, f"{json_basename}.xlsx")

        # 加载JSON数据（仅核心字段）
        test_cases = load_single_json(json_path)
        if not test_cases:
            print(f"[SKIP] {json_filename} 无有效数据，跳过")
            continue

        # 创建并写入Excel
        excel = ExcelManager(excel_path)
        excel.create()  # 创建带完整HEADERS的Excel
        excel.load()

        # 写入过滤后的测试用例
        for case in test_cases:
            excel.add_row(case)  # ExcelManager会自动处理列表/字典转JSON字符串

        # 设置列宽并保存
        excel.set_column_width()
        excel.save()

        print(f"[SUCCESS] Excel生成完成: {excel_path}")
        print(f"[INFO] 写入用例数: {len(test_cases)}")

    # 输出汇总
    print("\n" + "=" * 60)
    print(f"处理完成！共处理 {total_files} 个JSON文件，输出到 {results_dir}")
    print("=" * 60)


if __name__ == "__main__":
    process_llm_data_folder()
