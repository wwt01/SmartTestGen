"""
步骤1: 准备测试数据
从GitHub爬取的测试数据或手动构建的测试数据创建测试数据
"""

from utils.excel_manager import ExcelManager
import os
import sys
import json
from datetime import datetime

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))


def load_crawled_test_data():
    """加载从GitHub爬取的测试数据"""
    results_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "results")
    crawled_data_path = os.path.join(results_dir, "test_cases_with_requirements.json")

    if os.path.exists(crawled_data_path):
        try:
            with open(crawled_data_path, 'r', encoding='utf-8') as f:
                crawled_data = json.load(f)
            print(f"Loaded {len(crawled_data)} crawled test cases")
            return crawled_data
        except Exception as e:
            print(f"Error loading crawled test data: {e}")

    # 尝试加载其他可能的数据源
    alternative_paths = [
        os.path.join(results_dir, "selected_test_cases.json"),
        os.path.join(results_dir, "crawled_test_data.json")
    ]

    for path in alternative_paths:
        if os.path.exists(path):
            try:
                with open(path, 'r', encoding='utf-8') as f:
                    crawled_data = json.load(f)
                print(f"Loaded {len(crawled_data)} test cases from {os.path.basename(path)}")
                return crawled_data
            except Exception as e:
                print(f"Error loading test data from {path}: {e}")

    return []


MANUAL_TEST_CASES = [
    {
        "id": 1,
        "requirement": "测试add方法，输入两个整数1和2，期望返回3",
        "package_name": "com.example.calculator",
        "class_name": "Calculator",
        "method_name": "add",
        "parameters": "int a, int b",
        "return_type": "int",
        "is_interface": False,
        "class_type": "Service",
        "fields": "[]",
        "dependencies": "[]"
    },
    {
        "id": 2,
        "requirement": "测试subtract方法，输入5和3，期望返回2",
        "package_name": "com.example.calculator",
        "class_name": "Calculator",
        "method_name": "subtract",
        "parameters": "int a, int b",
        "return_type": "int",
        "is_interface": False,
        "class_type": "Service",
        "fields": "[]",
        "dependencies": "[]"
    },
    {
        "id": 3,
        "requirement": "测试multiply方法，输入4和5，期望返回20",
        "package_name": "com.example.calculator",
        "class_name": "Calculator",
        "method_name": "multiply",
        "parameters": "int a, int b",
        "return_type": "int",
        "is_interface": False,
        "class_type": "Service",
        "fields": "[]",
        "dependencies": "[]"
    },
    {
        "id": 4,
        "requirement": "测试divide方法，输入10和2，期望返回5",
        "package_name": "com.example.calculator",
        "class_name": "Calculator",
        "method_name": "divide",
        "parameters": "int a, int b",
        "return_type": "int",
        "is_interface": False,
        "class_type": "Service",
        "fields": "[]",
        "dependencies": "[]"
    },
    {
        "id": 5,
        "requirement": "测试isEven方法，输入4，期望返回true",
        "package_name": "com.example.math",
        "class_name": "MathUtils",
        "method_name": "isEven",
        "parameters": "int number",
        "return_type": "boolean",
        "is_interface": False,
        "class_type": "Service",
        "fields": "[]",
        "dependencies": "[]"
    },
    {
        "id": 6,
        "requirement": "测试getMax方法，输入数组[3,1,4,1,5]，期望返回5",
        "package_name": "com.example.array",
        "class_name": "ArrayUtils",
        "method_name": "getMax",
        "parameters": "int[] arr",
        "return_type": "int",
        "is_interface": False,
        "class_type": "Service",
        "fields": "[]",
        "dependencies": "[]"
    },
    {
        "id": 7,
        "requirement": "测试reverse方法，输入字符串hello，期望返回olleh",
        "package_name": "com.example.string",
        "class_name": "StringUtils",
        "method_name": "reverse",
        "parameters": "String str",
        "return_type": "String",
        "is_interface": False,
        "class_type": "Service",
        "fields": "[]",
        "dependencies": "[]"
    },
    {
        "id": 8,
        "requirement": "测试isEmpty方法，输入空字符串，期望返回true",
        "package_name": "com.example.string",
        "class_name": "StringUtils",
        "method_name": "isEmpty",
        "parameters": "String str",
        "return_type": "boolean",
        "is_interface": False,
        "class_type": "Service",
        "fields": "[]",
        "dependencies": "[]"
    },
    {
        "id": 9,
        "requirement": "测试calculateArea方法，输入半径5，期望返回78.54",
        "package_name": "com.example.geometry",
        "class_name": "Circle",
        "method_name": "calculateArea",
        "parameters": "double radius",
        "return_type": "double",
        "is_interface": False,
        "class_type": "DTO",
        "fields": "[{\"name\": \"radius\", \"type\": \"double\"}]",
        "dependencies": "[]"
    },
    {
        "id": 10,
        "requirement": "测试validateEmail方法，输入test@example.com，期望返回true",
        "package_name": "com.example.validation",
        "class_name": "EmailValidator",
        "method_name": "validateEmail",
        "parameters": "String email",
        "return_type": "boolean",
        "is_interface": False,
        "class_type": "Service",
        "fields": "[]",
        "dependencies": "[]"
    },
    {
        "id": 11,
        "requirement": "测试formatDate方法，输入日期2024-01-15，期望返回字符串2024年01月15日",
        "package_name": "com.example.date",
        "class_name": "DateFormatter",
        "method_name": "formatDate",
        "parameters": "LocalDate date",
        "return_type": "String",
        "is_interface": False,
        "class_type": "Service",
        "fields": "[]",
        "dependencies": "[\"java.time.LocalDate\"]"
    },
    {
        "id": 12,
        "requirement": "测试calculateDiscount方法，输入价格100和折扣率0.2，期望返回80.0",
        "package_name": "com.example.shop",
        "class_name": "PriceCalculator",
        "method_name": "calculateDiscount",
        "parameters": "double price, double discountRate",
        "return_type": "double",
        "is_interface": False,
        "class_type": "Service",
        "fields": "[]",
        "dependencies": "[]"
    },
    {
        "id": 13,
        "requirement": "测试contains方法，输入列表[1,2,3]和元素2，期望返回true",
        "package_name": "com.example.collection",
        "class_name": "ListUtils",
        "method_name": "contains",
        "parameters": "List<Integer> list, Integer element",
        "return_type": "boolean",
        "is_interface": False,
        "class_type": "Service",
        "fields": "[]",
        "dependencies": "[\"java.util.List\"]"
    },
    {
        "id": 14,
        "requirement": "测试concat方法，输入字符串数组[\"Hello\", \"World\"]，期望返回\"Hello World\"",
        "package_name": "com.example.string",
        "class_name": "StringUtils",
        "method_name": "concat",
        "parameters": "String[] strings",
        "return_type": "String",
        "is_interface": False,
        "class_type": "Service",
        "fields": "[]",
        "dependencies": "[]"
    },
    {
        "id": 15,
        "requirement": "测试sqrt方法，输入16，期望返回4.0",
        "package_name": "com.example.math",
        "class_name": "MathUtils",
        "method_name": "sqrt",
        "parameters": "double number",
        "return_type": "double",
        "is_interface": False,
        "class_type": "Service",
        "fields": "[]",
        "dependencies": "[]"
    },
    {
        "id": 16,
        "requirement": "测试静态方法parseInt，输入字符串\"123\"，期望返回整数123",
        "package_name": "com.example.parser",
        "class_name": "NumberParser",
        "method_name": "parseInt",
        "parameters": "String str",
        "return_type": "int",
        "is_interface": False,
        "class_type": "Service",
        "fields": "[]",
        "dependencies": "[]"
    },
    {
        "id": 17,
        "requirement": "测试findById方法，输入用户ID 1，期望返回用户对象",
        "package_name": "com.example.user",
        "class_name": "UserService",
        "method_name": "findById",
        "parameters": "Long id",
        "return_type": "User",
        "is_interface": False,
        "class_type": "Service",
        "fields": "[]",
        "dependencies": "[\"com.example.user.User\", \"com.example.user.UserRepository\"]"
    },
    {
        "id": 18,
        "requirement": "测试save方法，输入用户对象，期望返回保存后的用户",
        "package_name": "com.example.user",
        "class_name": "UserRepository",
        "method_name": "save",
        "parameters": "User user",
        "return_type": "User",
        "is_interface": True,
        "class_type": "Repository",
        "fields": "[]",
        "dependencies": "[\"com.example.user.User\"]"
    },
    {
        "id": 19,
        "requirement": "测试countWords方法，输入字符串\"Hello World Java\"，期望返回3",
        "package_name": "com.example.text",
        "class_name": "TextAnalyzer",
        "method_name": "countWords",
        "parameters": "String text",
        "return_type": "int",
        "is_interface": False,
        "class_type": "Service",
        "fields": "[]",
        "dependencies": "[]"
    },
    {
        "id": 20,
        "requirement": "测试generateId方法，期望返回非空且唯一的字符串ID",
        "package_name": "com.example.util",
        "class_name": "IdGenerator",
        "method_name": "generateId",
        "parameters": "",
        "return_type": "String",
        "is_interface": False,
        "class_type": "Service",
        "fields": "[]",
        "dependencies": "[]"
    }
]


def create_test_data():
    """创建测试数据Excel文件"""
    print("=" * 60)
    print("Step 1: Preparing Test Data")
    print(f"Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 60)

    results_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "results")
    os.makedirs(results_dir, exist_ok=True)

    excel_path = os.path.join(results_dir, "test_results.xlsx")

    excel = ExcelManager(excel_path)
    excel.create()
    excel.load()

    # 尝试加载爬取的测试数据
    crawled_test_cases = load_crawled_test_data()

    if crawled_test_cases:
        print(f"\nAdding {len(crawled_test_cases)} crawled test cases...")

        for case in crawled_test_cases:
            # 确保所有必要字段都存在
            required_fields = ["id", "requirement", "class_name", "method_name", "parameters", "return_type"]
            for field in required_fields:
                if field not in case:
                    case[field] = ""

            # 添加其他必要字段
            case.setdefault("package_name", "")
            case.setdefault("is_interface", False)
            case.setdefault("class_type", "Service")
            case.setdefault("fields", "[]")
            case.setdefault("dependencies", "[]")

            excel.add_row(case)
    else:
        print(f"\nAdding {len(MANUAL_TEST_CASES)} manual test cases...")

        for case in MANUAL_TEST_CASES:
            excel.add_row(case)

    excel.set_column_width()
    excel.save()

    total_cases = len(crawled_test_cases) if crawled_test_cases else len(MANUAL_TEST_CASES)

    print(f"\nTest data saved to: {excel_path}")
    print(f"   Total test cases: {total_cases}")

    print("\n" + "=" * 60)
    print("Test Data Summary")
    print("=" * 60)

    test_cases = crawled_test_cases if crawled_test_cases else MANUAL_TEST_CASES

    print(f"\n{'ID':<4} {'Requirement':<40} {'Class':<20} {'Method':<15}")
    print("-" * 80)

    for case in test_cases[:10]:  # 只显示前10个
        req_short = case['requirement'][:37] + "..." if len(case['requirement']) > 40 else case['requirement']
        print(f"{case['id']:<4} {req_short:<40} {case['class_name']:<20} {case['method_name']:<15}")

    if len(test_cases) > 10:
        print(f"... and {len(test_cases) - 10} more cases")

    print("\n" + "=" * 60)
    print("Step 1 completed successfully!")
    print("=" * 60)

    return excel_path


if __name__ == "__main__":
    create_test_data()
