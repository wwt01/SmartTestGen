import os
import json
from langchain_openai import ChatOpenAI

# ===================== 你的配置 =====================
API_KEY = "sk-afe08b93d34246628907cfcd9fab7401"  # 填写 DeepSeek API Key
BASE_URL = "https://api.deepseek.com"
MODEL = "deepseek-chat"
# ====================================================

INPUT_DIR = "select_data"
OUTPUT_DIR = "llm_data"
os.makedirs(OUTPUT_DIR, exist_ok=True)
os.makedirs(INPUT_DIR, exist_ok=True)

# 初始化 LLM（LangChain + ChatOpenAI 格式对接 DeepSeek）
llm = ChatOpenAI(
    api_key=API_KEY,
    base_url=BASE_URL,
    model=MODEL,
    temperature=0.1,
    max_tokens=1024
)


def generate_description(code):
    prompt = f"""
你是专业测试工程师。根据下面的Java方法生成【方法需求描述】。

必须包含：
1. 方法名称
2. 参数名称类型
3. 返回值类型
4. 核心逻辑
5. 是否是静态方法

代码：
{code}

警告：只返回需求描述，无需多余符号，注意信息要全，不要多余内容。
""".strip()

    try:
        print("开始调用大模型（流式输出）")

        # 流式接收
        full_response = ""
        for chunk in llm.stream(prompt):
            content = chunk.content
            if content:
                print(content, end="", flush=True)  # 实时打印流
                full_response += content

        print()  # 换行
        return full_response.strip()

    except Exception as e:
        error_msg = f"LLM调用失败：{str(e)}"
        print(error_msg)
        return error_msg

# 处理文件（不删减任何字段！只追加 description）


def process_file(filename):
    input_path = os.path.join(INPUT_DIR, filename)
    output_path = os.path.join(OUTPUT_DIR, filename)

    with open(input_path, "r", encoding="utf-8") as f:
        items = json.load(f)

    print(f"处理：{filename} 数量：{len(items)}")

    # ✅ 只追加字段，不修改任何原有内容
    for item in items:
        id = item.get("id")
        code = item.get("original_code", "")
        desc = generate_description(code)
        item["description"] = desc  # 只加这一句
        print(f"处理id:{id},{filename}成功")

    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(items, f, indent=2, ensure_ascii=False)

    print(f"✅ 完成：{filename}\n")


if __name__ == "__main__":
    # 要处理的文件名称列表，可以根据需要修改
    files_to_process = [
        # "simple_group_select.json",
        # "generic_group_select.json",
        # "complex_group_select.json"
        "maths_group_select.json"
    ]

    for f in files_to_process:
        if f.endswith(".json"):
            process_file(f)

    print("🎉 全部完成！结果在 llm_data/")
