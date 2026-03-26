"""
验证Java编译功能是否可行
测试内容：
1. 创建临时Java文件
2. 写入测试代码（包含编译错误）
3. 使用javac编译并获取错误信息
"""

import os
import subprocess
import tempfile
import shutil
from datetime import datetime


def create_temp_project(base_dir: str) -> dict:
    """创建临时Java项目结构"""
    project_dir = os.path.join(base_dir, "test_compile_project")
    src_dir = os.path.join(project_dir, "src")
    target_dir = os.path.join(project_dir, "target")
    
    os.makedirs(src_dir, exist_ok=True)
    os.makedirs(target_dir, exist_ok=True)
    
    return {
        "project_dir": project_dir,
        "src_dir": src_dir,
        "target_dir": target_dir
    }


def create_main_class(src_dir: str, package: str, class_name: str, method_code: str) -> str:
    """创建主类（包含空方法）"""
    package_dir = os.path.join(src_dir, *package.split('.'))
    os.makedirs(package_dir, exist_ok=True)
    
    file_path = os.path.join(package_dir, f"{class_name}.java")
    
    content = f"""package {package};

public class {class_name} {{
    
    {method_code}
}}
"""
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)
    
    return file_path


def create_test_class(src_dir: str, package: str, class_name: str, test_code: str) -> str:
    """创建测试类"""
    package_dir = os.path.join(src_dir, *package.split('.'))
    os.makedirs(package_dir, exist_ok=True)
    
    file_path = os.path.join(package_dir, f"{class_name}Test.java")
    
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(test_code)
    
    return file_path


def compile_with_javac(project_dir: str, src_dir: str, target_dir: str) -> dict:
    """使用javac编译"""
    try:
        java_files = []
        for root, dirs, files in os.walk(src_dir):
            for file in files:
                if file.endswith('.java'):
                    java_files.append(os.path.join(root, file))
        
        if not java_files:
            return {
                "success": False,
                "stdout": "",
                "stderr": "No Java files found",
                "returncode": -1
            }
        
        result = subprocess.run(
            ["javac", "-d", target_dir, "-encoding", "UTF-8"] + java_files,
            capture_output=True,
            text=True,
            timeout=60
        )
        
        return {
            "success": result.returncode == 0,
            "stdout": result.stdout,
            "stderr": result.stderr,
            "returncode": result.returncode
        }
        
    except subprocess.TimeoutExpired:
        return {
            "success": False,
            "stdout": "",
            "stderr": "Compilation timeout",
            "returncode": -1
        }
    except FileNotFoundError:
        return {
            "success": False,
            "stdout": "",
            "stderr": "javac not found. Please install JDK and add to PATH.",
            "returncode": -1
        }
    except Exception as e:
        return {
            "success": False,
            "stdout": "",
            "stderr": str(e),
            "returncode": -1
        }


def test_compile_success():
    """测试：编译成功的代码"""
    print("\n" + "=" * 60)
    print("Test 1: Compile Success")
    print("=" * 60)
    
    base_dir = tempfile.mkdtemp()
    
    try:
        project = create_temp_project(base_dir)
        
        method_code = """public int add(int a, int b) {
        return a + b;
    }"""
        
        test_code = """package com.example;

public class CalculatorTest {
    
    public void testAdd() {
        Calculator calc = new Calculator();
        int result = calc.add(1, 2);
        System.out.println("Result: " + result);
    }
}
"""
        
        create_main_class(project["src_dir"], "com.example", "Calculator", method_code)
        create_test_class(project["src_dir"], "com.example", "Calculator", test_code)
        
        result = compile_with_javac(
            project["project_dir"],
            project["src_dir"],
            project["target_dir"]
        )
        
        print(f"Success: {result['success']}")
        print(f"Return Code: {result['returncode']}")
        if result['stderr']:
            print(f"Stderr: {result['stderr']}")
        if result['stdout']:
            print(f"Stdout: {result['stdout']}")
        
        return result['success']
        
    finally:
        shutil.rmtree(base_dir, ignore_errors=True)


def test_compile_error():
    """测试：编译失败的代码"""
    print("\n" + "=" * 60)
    print("Test 2: Compile Error (Undefined Class)")
    print("=" * 60)
    
    base_dir = tempfile.mkdtemp()
    
    try:
        project = create_temp_project(base_dir)
        
        method_code = """public int add(int a, int b) {
        return a + b;
    }"""
        
        test_code = """package com.example;

public class CalculatorTest {
    
    public void testAdd() {
        Calculator calc = new Calculator();
        int result = calc.add(1, 2);
        
        // 故意制造编译错误: StringX类不存在
        StringX str = null;
    }
}
"""
        
        create_main_class(project["src_dir"], "com.example", "Calculator", method_code)
        create_test_class(project["src_dir"], "com.example", "Calculator", test_code)
        
        result = compile_with_javac(
            project["project_dir"],
            project["src_dir"],
            project["target_dir"]
        )
        
        print(f"Success: {result['success']}")
        print(f"Return Code: {result['returncode']}")
        if result['stderr']:
            print(f"\n--- Compilation Errors ---\n{result['stderr']}")
        
        return not result['success'] and result['stderr']
        
    finally:
        shutil.rmtree(base_dir, ignore_errors=True)


def test_type_mismatch():
    """测试：类型不匹配"""
    print("\n" + "=" * 60)
    print("Test 3: Type Mismatch Error")
    print("=" * 60)
    
    base_dir = tempfile.mkdtemp()
    
    try:
        project = create_temp_project(base_dir)
        
        method_code = """public int add(int a, int b) {
        return a + b;
    }"""
        
        test_code = """package com.example;

public class CalculatorTest {
    
    public void testAdd() {
        Calculator calc = new Calculator();
        // 故意制造编译错误: 参数类型不匹配
        int result = calc.add("string", 1);
    }
}
"""
        
        create_main_class(project["src_dir"], "com.example", "Calculator", method_code)
        create_test_class(project["src_dir"], "com.example", "Calculator", test_code)
        
        result = compile_with_javac(
            project["project_dir"],
            project["src_dir"],
            project["target_dir"]
        )
        
        print(f"Success: {result['success']}")
        print(f"Return Code: {result['returncode']}")
        if result['stderr']:
            print(f"\n--- Compilation Errors ---\n{result['stderr']}")
        
        return not result['success'] and result['stderr']
        
    finally:
        shutil.rmtree(base_dir, ignore_errors=True)


def test_missing_semicolon():
    """测试：缺少分号"""
    print("\n" + "=" * 60)
    print("Test 4: Missing Semicolon")
    print("=" * 60)
    
    base_dir = tempfile.mkdtemp()
    
    try:
        project = create_temp_project(base_dir)
        
        method_code = """public int add(int a, int b) {
        return a + b;
    }"""
        
        test_code = """package com.example;

public class CalculatorTest {
    
    public void testAdd() {
        Calculator calc = new Calculator()
        int result = calc.add(1, 2);
    }
}
"""
        
        create_main_class(project["src_dir"], "com.example", "Calculator", method_code)
        create_test_class(project["src_dir"], "com.example", "Calculator", test_code)
        
        result = compile_with_javac(
            project["project_dir"],
            project["src_dir"],
            project["target_dir"]
        )
        
        print(f"Success: {result['success']}")
        print(f"Return Code: {result['returncode']}")
        if result['stderr']:
            print(f"\n--- Compilation Errors ---\n{result['stderr']}")
        
        return not result['success'] and result['stderr']
        
    finally:
        shutil.rmtree(base_dir, ignore_errors=True)


def check_java_installed():
    """检查Java是否安装"""
    print("\n" + "=" * 60)
    print("Checking Java Installation")
    print("=" * 60)
    
    try:
        result = subprocess.run(
            ["java", "-version"],
            capture_output=True,
            text=True,
            timeout=10
        )
        
        if result.returncode == 0:
            print("✅ Java is installed")
            version_line = result.stderr.split('\n')[0] if result.stderr else result.stdout.split('\n')[0]
            print(f"   {version_line}")
            return True
        else:
            print("❌ Java is not working properly")
            return False
            
    except FileNotFoundError:
        print("❌ Java not found. Please install JDK and add to PATH.")
        print("   Download: https://adoptium.net/")
        return False
    except subprocess.TimeoutExpired:
        print("❌ Java command timeout")
        return False


def check_javac_installed():
    """检查javac是否可用"""
    print("\n" + "=" * 60)
    print("Checking javac (Java Compiler)")
    print("=" * 60)
    
    try:
        result = subprocess.run(
            ["javac", "-version"],
            capture_output=True,
            text=True,
            timeout=10
        )
        
        if result.returncode == 0:
            print("✅ javac is available")
            version_line = result.stderr.split('\n')[0] if result.stderr else result.stdout.split('\n')[0]
            print(f"   {version_line}")
            return True
        else:
            print("❌ javac is not working properly")
            return False
            
    except FileNotFoundError:
        print("❌ javac not found. Please install JDK (not JRE) and add to PATH.")
        print("   Download: https://adoptium.net/")
        return False
    except subprocess.TimeoutExpired:
        print("❌ javac command timeout")
        return False


if __name__ == "__main__":
    print("=" * 60)
    print("Java Compilation Verification")
    print(f"Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 60)
    
    java_ok = check_java_installed()
    javac_ok = check_javac_installed()
    
    if not java_ok or not javac_ok:
        print("\n❌ Environment check failed. Please install JDK.")
        exit(1)
    
    print("\n" + "=" * 60)
    print("Running Compilation Tests")
    print("=" * 60)
    
    test1_passed = test_compile_success()
    test2_passed = test_compile_error()
    test3_passed = test_type_mismatch()
    test4_passed = test_missing_semicolon()
    
    print("\n" + "=" * 60)
    print("Test Results Summary")
    print("=" * 60)
    print(f"Test 1 (Compile Success):      {'✅ PASSED' if test1_passed else '❌ FAILED'}")
    print(f"Test 2 (Undefined Class):      {'✅ PASSED' if test2_passed else '❌ FAILED'}")
    print(f"Test 3 (Type Mismatch):        {'✅ PASSED' if test3_passed else '❌ FAILED'}")
    print(f"Test 4 (Missing Semicolon):    {'✅ PASSED' if test4_passed else '❌ FAILED'}")
    
    all_passed = test1_passed and test2_passed and test3_passed and test4_passed
    
    print("\n" + "=" * 60)
    if all_passed:
        print("✅ All tests passed!")
        print("   Compilation verification successful.")
        print("   You can proceed with the performance testing framework.")
    else:
        print("❌ Some tests failed. Please check the environment.")
    print("=" * 60)
