"""
GitHub测试集爬取模块
从GitHub上爬取测试集，分析代码结构，选择方法并生成需求描述
"""

import os
import subprocess
import re
import json
from datetime import datetime
from typing import List, Dict, Any

# 添加utils模块路径
import sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from utils.log_manager import logger
from utils.config_manager import config

# 存储爬取的测试数据
CRAWLED_TEST_DATA = []


def clone_repository(repo_url, target_dir):
    """克隆GitHub仓库"""
    if not os.path.exists(target_dir):
        os.makedirs(target_dir)

    logger.info(f"Cloning repository: {repo_url}")
    result = subprocess.run(
        ["git", "clone", repo_url, target_dir],
        capture_output=True,
        text=True
    )

    if result.returncode != 0:
        logger.error(f"Failed to clone repository: {result.stderr}")
        return False

    logger.info(f"Successfully cloned repository to: {target_dir}")
    return True


def find_java_files(directory):
    """查找Java文件"""
    java_files = []
    max_files = config.get("github.max_files_per_repo", 100)
    
    for root, _, files in os.walk(directory):
        if len(java_files) >= max_files:
            break
        
        for file in files:
            if len(java_files) >= max_files:
                break
            if file.endswith(".java"):
                java_files.append(os.path.join(root, file))
    
    return java_files


def extract_package_name(java_content):
    """提取包名"""
    package_match = re.search(r'package\s+([\w\.]+);', java_content)
    return package_match.group(1) if package_match else ""


def extract_class_name(java_content):
    """提取类名"""
    # 查找类定义
    class_match = re.search(r'\b(class|interface|enum)\s+(\w+)', java_content)
    return class_match.group(2) if class_match else ""


def extract_class_type(java_content):
    """提取类类型"""
    if 'interface' in java_content:
        return "interface"
    elif 'enum' in java_content:
        return "enum"
    else:
        return "class"


def extract_fields(java_content):
    """提取字段"""
    fields = []
    field_pattern = re.compile(r'\b(private|public|protected)\s+([\w<>\[\]]+)\s+(\w+)(\s*=\s*[^;]+)?;')
    for match in field_pattern.finditer(java_content):
        fields.append({
            "visibility": match.group(1),
            "type": match.group(2),
            "name": match.group(3)
        })
    return fields


def extract_methods(java_content):
    """提取方法"""
    methods = []
    # 匹配方法定义
    method_pattern = re.compile(r'\b(private|public|protected)\s+([\w<>\[\]]+)\s+(\w+)\s*\(([^\)]*)\)')
    
    for match in method_pattern.finditer(java_content):
        visibility = match.group(1)
        return_type = match.group(2)
        method_name = match.group(3)
        parameters = match.group(4).strip()
        
        # 只提取公共方法
        if visibility == "public":
            methods.append({
                "name": method_name,
                "return_type": return_type,
                "parameters": parameters,
                "visibility": visibility
            })
    
    return methods


def extract_dependencies(java_content):
    """提取依赖"""
    dependencies = []
    import_pattern = re.compile(r'import\s+([\w\.]+(\.[\w\*]+)?)\s*;')
    for match in import_pattern.finditer(java_content):
        dependencies.append(match.group(1))
    return dependencies


def generate_requirement(method_info, class_name):
    """生成方法的需求描述（暂时使用简单描述，后续由LLM优化）"""
    method_name = method_info['name']
    return_type = method_info['return_type']
    parameters = method_info['parameters']
    
    # 生成简单的需求描述，后续由LLM进行详细优化
    requirement = f"测试{method_name}方法，参数：{parameters}，返回类型：{return_type}"
    
    return requirement


def process_java_file(java_file):
    """处理Java文件"""
    try:
        with open(java_file, 'r', encoding='utf-8') as f:
            content = f.read()
        
        package_name = extract_package_name(content)
        class_name = extract_class_name(content)
        class_type = extract_class_type(content)
        fields = extract_fields(content)
        methods = extract_methods(content)
        dependencies = extract_dependencies(content)
        
        # 限制方法数量
        max_methods = config.get("github.max_methods_per_class", 10)
        methods = methods[:max_methods]
        
        return {
            "package_name": package_name,
            "class_name": class_name,
            "class_type": class_type,
            "is_interface": class_type == "interface",
            "fields": fields,
            "methods": methods,
            "dependencies": dependencies,
            "file_path": java_file,
            "original_code": content
        }
    except Exception as e:
        logger.error(f"Error processing Java file {java_file}: {e}")
        return None


def crawl_github_repos():
    """爬取GitHub仓库并分析代码"""
    global CRAWLED_TEST_DATA
    CRAWLED_TEST_DATA = []
    
    logger.info("=" * 70)
    logger.info("Crawling GitHub Repositories")
    logger.info(f"Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    logger.info("=" * 70)
    
    # 获取GitHub配置
    github_config = config.get_github_config()
    repos = github_config.get("repos", [])
    clone_dir = github_config.get("clone_dir", "repos")
    
    test_id = 1
    
    for repo in repos:
        repo_name = repo["name"]
        repo_url = repo["url"]
        repo_desc = repo["description"]
        
        logger.info(f"\nProcessing repository: {repo_name}")
        logger.info(f"Description: {repo_desc}")
        
        # 克隆仓库
        repo_path = os.path.join(clone_dir, repo_name)
        if not clone_repository(repo_url, repo_path):
            continue
        
        # 查找Java文件
        java_files = find_java_files(repo_path)
        logger.info(f"Found {len(java_files)} Java files")
        
        # 处理Java文件
        for java_file in java_files:
            class_info = process_java_file(java_file)
            if not class_info:
                continue
            
            # 处理每个方法
            for method in class_info["methods"]:
                requirement = generate_requirement(method, class_info["class_name"])
                
                test_case = {
                    "id": test_id,
                    "requirement": requirement,
                    "package_name": class_info["package_name"],
                    "class_name": class_info["class_name"],
                    "method_name": method["name"],
                    "parameters": method["parameters"],
                    "return_type": method["return_type"],
                    "is_interface": class_info["is_interface"],
                    "class_type": class_info["class_type"],
                    "fields": json.dumps(class_info["fields"], ensure_ascii=False),
                    "dependencies": json.dumps(class_info["dependencies"], ensure_ascii=False),
                    "original_code": class_info["original_code"]
                }
                
                CRAWLED_TEST_DATA.append(test_case)
                test_id += 1
    
    logger.info(f"\n✅ Crawled {len(CRAWLED_TEST_DATA)} test cases from GitHub")


def save_crawled_data():
    """保存爬取的测试数据"""
    if not CRAWLED_TEST_DATA:
        logger.warning("No crawled test data to save")
        return
    
    # 获取输出配置
    output_config = config.get_output_config()
    results_dir = output_config.get("results_dir", "results")
    json_path = os.path.join(results_dir, "crawled_test_data.json")
    
    # 确保输出目录存在
    os.makedirs(results_dir, exist_ok=True)
    
    # 保存为JSON文件
    with open(json_path, 'w', encoding='utf-8') as f:
        json.dump(CRAWLED_TEST_DATA, f, ensure_ascii=False, indent=2)
    
    logger.info(f"✅ Crawled test data saved to: {json_path}")
    
    # 打印爬取结果摘要
    logger.info("\n" + "=" * 70)
    logger.info("Crawled Test Data Summary")
    logger.info("=" * 70)
    
    logger.info(f"\n{'ID':<4} {'Requirement':<40} {'Class':<20} {'Method':<15}")
    logger.info("-" * 80)
    
    for case in CRAWLED_TEST_DATA[:10]:  # 只显示前10个
        req_short = case['requirement'][:37] + "..." if len(case['requirement']) > 40 else case['requirement']
        logger.info(f"{case['id']:<4} {req_short:<40} {case['class_name']:<20} {case['method_name']:<15}")
    
    if len(CRAWLED_TEST_DATA) > 10:
        logger.info(f"... and {len(CRAWLED_TEST_DATA) - 10} more cases")
    
    logger.info("=" * 70)


def main():
    """主函数"""
    crawl_github_repos()
    save_crawled_data()


if __name__ == "__main__":
    main()
