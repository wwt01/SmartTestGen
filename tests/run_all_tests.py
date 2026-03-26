"""
性能测试主脚本
按顺序执行所有测试步骤
"""

import os
import sys
import subprocess
from datetime import datetime

VENV_PYTHON = r"d:\MyProjects\GraduationProject\SmartTestGen\tests\venv\Scripts\python.exe"
TESTS_DIR = r"d:\MyProjects\GraduationProject\SmartTestGen\tests"


def run_step(step_name: str, script_name: str):
    """运行单个步骤"""
    print("\n" + "=" * 70)
    print(f"  Running: {step_name}")
    print("=" * 70 + "\n")
    
    script_path = os.path.join(TESTS_DIR, script_name)
    
    result = subprocess.run(
        [VENV_PYTHON, script_path],
        cwd=TESTS_DIR
    )
    
    return result.returncode == 0


def main():
    """主函数"""
    print("=" * 70)
    print("  SmartTestGen Performance Test Framework")
    print(f"  Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 70)
    
    steps = [
        ("Step 1: Prepare Test Data", "step1_prepare_data.py"),
        ("Step 2: Call Parse API and Init Session", "step2_call_parse_api.py"),
        ("Step 3: Call Generate Test Code API", "step3_call_generate_api.py"),
        ("Step 4: Call Fix Compilation Error API", "step4_call_fix_api.py"),
    ]
    
    results = []
    
    for step_name, script_name in steps:
        success = run_step(step_name, script_name)
        results.append((step_name, success))
        
        if not success:
            print(f"\n❌ {step_name} failed. Stopping.")
            break
    
    print("\n" + "=" * 70)
    print("  Test Summary")
    print("=" * 70)
    
    for step_name, success in results:
        status = "✅ PASSED" if success else "❌ FAILED"
        print(f"  {step_name}: {status}")
    
    print("=" * 70)


if __name__ == "__main__":
    main()
