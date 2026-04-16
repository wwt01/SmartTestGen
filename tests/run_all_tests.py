"""
性能测试主脚本
按顺序执行所有测试步骤
"""

import os
import sys
import subprocess
from datetime import datetime

# 脚本目录
TESTS_DIR = os.path.dirname(os.path.abspath(__file__))

# 检查Python环境


def get_python_executable():
    """获取Python可执行文件路径"""
    # 优先使用虚拟环境
    venv_python = os.path.join(TESTS_DIR, "venv", "Scripts", "python.exe")
    if os.path.exists(venv_python):
        return venv_python
    # 否则使用系统Python
    return sys.executable


PYTHON_EXECUTABLE = get_python_executable()


def run_step(step_name: str, script_name: str, *args):
    """运行单个步骤"""
    print("\n" + "=" * 70)
    print(f"  Running: {step_name}")
    print("=" * 70 + "\n")

    script_path = os.path.join(TESTS_DIR, script_name)

    command = [PYTHON_EXECUTABLE, script_path]
    command.extend(args)

    result = subprocess.run(
        command,
        cwd=TESTS_DIR,
        capture_output=True,
        text=True
    )

    # 打印输出
    if result.stdout:
        print(result.stdout)
    if result.stderr:
        print("\n--- Error Output ---")
        print(result.stderr)

    return result.returncode == 0


def main():
    """主函数"""
    print("=" * 70)
    print("  SmartTestGen Performance Test Framework")
    print(f"  Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print(f"  Python: {PYTHON_EXECUTABLE}")
    print("=" * 70)

    steps = [
        # ("Step 1: Crawl Test Data from GitHub", "github_crawler.py"),
        # ("Step 2: Analyze and Select Test Cases", "test_analyzer.py"),
        # ("Step 3: Generate Requirements with LLM", "llm_requirement_generator.py"),
        ("Step 4: Prepare Test Data Excel", "step1_prepare_data.py"),
        ("Step 5: Call Parse API and Init Session", "api_caller.py", "--step", "parse"),
        ("Step 6: Call Generate Test Code API", "api_caller.py", "--step", "generate"),
        ("Step 7: Call Fix Compilation Error API", "api_caller.py", "--step", "fix"),
        ("Step 8: Analyze Coverage and Run Results", "coverage_analyzer.py"),
    ]

    results = []

    for step_name, script_name, *args in steps:
        success = run_step(step_name, script_name, *args)
        results.append((step_name, success))

        if not success:
            print(f"\n[FAIL] {step_name} failed. Stopping.")
            break

    print("\n" + "=" * 70)
    print("  Test Summary")
    print("=" * 70)

    for step_name, success in results:
        status = "✅ PASSED" if success else "❌ FAILED"
        print(f"  {step_name}: {status}")

    print("\n" + "=" * 70)
    print("  Test Framework Complete")
    print("  All steps have been executed according to the test plan.")
    print("  Check the results directory for detailed test results.")
    print("=" * 70)


if __name__ == "__main__":
    main()
