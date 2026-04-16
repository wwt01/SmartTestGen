"""
爬取项目并解析json至data目录
"""
import os
import json
import subprocess
import re

# ===================== 配置 =====================
REPO_URL = "https://github.com/TheAlgorithms/Java.git"
PROJECT_DIR = "TheAlgorithms-Java"
SRC_DIR = os.path.join(PROJECT_DIR, "src/main/java/com/thealgorithms")
OUTPUT_DIR = "data"  # 输出目录统一在这里配置

# 你指定的分组
GROUP_CONFIG = {
    # "simple_group": ["maths", "strings", "searches"],
    # "generic_group": ["sorts", "datastructures"],
    # "complex_group": ["graph", "dynamicprogramming", "backtracking"]
    "maths_group": ["maths"]
}

# ===================== 正则 =====================
PACKAGE_PATTERN = re.compile(r'package\s+([\w.]+);')
IMPORT_PATTERN = re.compile(r'^import\s+([\w.]+);', re.MULTILINE)
# 改进的正则表达式，只匹配类定义中的类名，避免匹配到implements等关键字
CLASS_PATTERN = re.compile(r'\bclass\s+(\w+)(?:\s+extends\s+\w+)?(?:\s+implements\s+[\w,\s]+)?\s*\{')
# 改进的正则表达式，只匹配接口定义中的接口名
INTERFACE_PATTERN = re.compile(r'\binterface\s+(\w+)(?:\s+extends\s+[\w,\s]+)?\s*\{')  # 识别接口
FIELD_PATTERN = re.compile(
    r'^\s*(public|private|protected)?\s*([\w<>]+)\s+(\w+)\s*[=;]', re.MULTILINE)

METHOD_PATTERN = re.compile(
    r'''
    (public)?\s*
    (static|final)?\s*
    ([\w<>]+)\s+
    (\w+)
    \s*\(([^)]*)\)\s*\{
    ''',
    re.DOTALL | re.VERBOSE
)

# ===================== 克隆项目 =====================


def clone_repo():
    if not os.path.exists(PROJECT_DIR):
        print(f"[+] 克隆项目 {REPO_URL}")
        subprocess.run(["git", "clone", "--depth", "1", REPO_URL, PROJECT_DIR])
    else:
        print(f"[+] 项目已存在，跳过克隆")

# ===================== 创建输出目录 =====================


def prepare_output_dir():
    if not os.path.exists(OUTPUT_DIR):
        os.makedirs(OUTPUT_DIR)

# ===================== 获取该组所有 Java 文件 =====================


def get_java_files(folders):
    files = []
    for folder in folders:
        fp = os.path.join(SRC_DIR, folder)
        if not os.path.isdir(fp):
            continue
        for root, _, filenames in os.walk(fp):
            for f in filenames:
                if f.endswith(".java"):
                    files.append(os.path.join(root, f))
    return files

# ===================== 解析 Java 文件 =====================


def parse_java(filepath):
    try:
        with open(filepath, "r", encoding="utf-8") as f:
            content = f.read()
    except:
        return None

    pkg = PACKAGE_PATTERN.search(content)
    package_name = pkg.group(1) if pkg else ""

    # 判断是否为接口
    is_interface = False
    class_name = None

    interface_match = INTERFACE_PATTERN.search(content)
    if interface_match:
        is_interface = True
        class_name = interface_match.group(1)
    else:
        class_match = CLASS_PATTERN.search(content)
        if not class_match:
            return None
        class_name = class_match.group(1)

    dependencies = list(set(IMPORT_PATTERN.findall(content)))

    fields = []
    for m in FIELD_PATTERN.finditer(content):
        vis = m.group(1) or "default"
        type_name = m.group(2)
        name = m.group(3)
        fields.append({
            "visibility": vis.strip(),
            "type": type_name.strip(),
            "name": name.strip()
        })

    # 提取所有方法（包括私有、保护和公有方法）
    all_class_methods = []
    for match in METHOD_PATTERN.finditer(content):
        start = match.start()
        body_end = content.find("}", start) + 1
        if body_end <= start:
            continue

        modifier = match.group(1)
        static_modifier = match.group(2)
        ret_type = match.group(3)
        method_name = match.group(4)
        params = match.group(5) or ""
        method_code = content[start:body_end].strip()

        # 跳过构造函数
        if method_name == class_name:
            continue

        # 记录方法的可见性
        visibility = modifier.strip() if modifier else "default"
        is_static = static_modifier == "static"

        all_class_methods.append({
            "method_name": method_name,
            "return_type": ret_type,
            "parameters": params,
            "visibility": visibility,
            "is_static": is_static,
            "original_code": method_code
        })

    # 获取文件的绝对路径
    absolute_path = os.path.abspath(filepath)

    return {
        "package_name": package_name,
        "class_name": class_name,
        "is_interface": is_interface,  # 新增
        "dependencies": dependencies,
        "fields": fields,
        "all_class_methods": all_class_methods,
        "file_path": absolute_path  # 新增：文件绝对路径
    }

# ===================== 处理分组 =====================


def process_group(name, folders):
    print(f"\n===== 处理 {name} =====")
    files = get_java_files(folders)
    result = []
    idx = 1

    for f in files:
        data = parse_java(f)
        if not data:
            continue

        # 只对公有方法创建json
        for m in data["all_class_methods"]:
            # 只处理公有方法
            if m["visibility"] != "public":
                continue

            # 其他方法（包括私有、保护和公有方法）
            other_methods = [
                meth for meth in data["all_class_methods"]
                if meth["method_name"] != m["method_name"]
            ]

            result.append({
                "id": idx,
                "package_name": data["package_name"],
                "class_name": data["class_name"],
                "is_interface": data["is_interface"],  # 写入JSON
                "method_name": m["method_name"],
                "parameters": m["parameters"],
                "return_type": m["return_type"],
                "fields": json.dumps(data["fields"], ensure_ascii=False),
                "dependencies": data["dependencies"],
                "original_code": m["original_code"],
                "class_methods": other_methods,  # 同类其他方法（包括私有、保护和公有方法）
                "file_path": data["file_path"]  # 新增：文件绝对路径
            })
            idx += 1

    # 保存到 data/ 目录
    output_path = os.path.join(OUTPUT_DIR, f"{name}.json")
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(result, f, indent=2, ensure_ascii=False)

    print(f"✅ {name} 完成 | 方法总数：{len(result)} | 保存到：{output_path}")


# ===================== 主程序 =====================
if __name__ == "__main__":
    clone_repo()
    prepare_output_dir()  # 自动创建data目录
    # process_group("simple_group", GROUP_CONFIG["simple_group"])
    # process_group("generic_group", GROUP_CONFIG["generic_group"])
    # process_group("complex_group", GROUP_CONFIG["complex_group"])

    process_group("maths_group", GROUP_CONFIG["maths_group"])
    print("\n🎉 全部完成！文件在 data/ 目录下")
