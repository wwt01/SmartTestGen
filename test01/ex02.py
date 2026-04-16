"""
三组json列表随机抽取 100 条数据
"""
import os
import json
import random

# 配置路径
INPUT_DIR = "data"
OUTPUT_DIR = "select_data"

# 自动创建输出目录
os.makedirs(OUTPUT_DIR, exist_ok=True)

# 遍历所有 JSON 文件
for filename in os.listdir(INPUT_DIR):
    if not filename.endswith(".json"):
        continue

    input_path = os.path.join(INPUT_DIR, filename)
    base_name = filename[:-5]  # 去掉 .json
    output_filename = f"{base_name}_select.json"
    output_path = os.path.join(OUTPUT_DIR, output_filename)

    # 读取原数据
    with open(input_path, "r", encoding="utf-8") as f:
        data_list = json.load(f)

    # 随机选 100 条
    sample_count = min(100, len(data_list))
    selected_data = random.sample(data_list, sample_count)

    # ✅ 按 id 从小到大排序
    selected_data.sort(key=lambda x: x["id"])

    # 写入新文件
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(selected_data, f, indent=2, ensure_ascii=False)

    print(f"✅ 处理完成：{filename} → {output_filename} | 抽取 {len(selected_data)} 条 | 已按 id 排序")

print("\n🎉 全部任务完成！文件在 select_data/ 文件夹")
