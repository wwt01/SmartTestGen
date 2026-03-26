"""
步骤3: 调用接口2生成测试代码
读取Excel中的测试数据，调用生成测试代码接口，并进行模拟编译
"""

import os
import sys
import json
from datetime import datetime

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from utils.excel_manager import ExcelManager
from utils.api_client import APIClient
from utils.compilation_util import CompilationUtil


def call_generate_test_code():
    """调用生成测试代码接口"""
    print("=" * 60)
    print("Step 3: Calling Generate Test Code API")
    print(f"Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 60)
    
    results_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "results")
    excel_path = os.path.join(results_dir, "test_results.xlsx")
    
    if not os.path.exists(excel_path):
        print("❌ Error: test_results.xlsx not found. Please run step1 and step2 first.")
        return
    
    excel = ExcelManager(excel_path)
    excel.load()
    
    api = APIClient()
    
    print("\nChecking API connection...")
    if not api.health_check():
        print("❌ Error: API server is not running.")
        return
    
    print("✅ API server is running\n")
    
    print("Checking javac availability...")
    if not CompilationUtil.check_javac_available():
        print("❌ Error: javac is not available. Please install JDK.")
        return
    
    print("✅ javac is available\n")
    
    total_rows = excel.get_row_count()
    print(f"Processing {total_rows} test cases...\n")
    
    success_count = 0
    compile_success_count = 0
    compile_fail_count = 0
    api_fail_count = 0
    
    for i in range(1, total_rows + 1):
        row_data = excel.get_row(i)
        test_id = row_data.get("id", i)
        requirement = row_data.get("requirement", "")
        session_id = row_data.get("session_id", "")
        
        print(f"[{i}/{total_rows}] Processing ID {test_id}: {requirement[:40]}...")
        
        if not session_id:
            print(f"   ⚠️ Skipped: No session_id found")
            continue
        
        structured_result = row_data.get("structured_result", "")
        if not structured_result:
            print(f"   ⚠️ Skipped: No structured_result found")
            continue
        
        try:
            structured_data = json.loads(structured_result)
        except json.JSONDecodeError:
            print(f"   ⚠️ Skipped: Invalid structured_result JSON")
            continue
        
        request_data = {
            "session_id": session_id,
            "method_name": structured_data.get("method_name", row_data.get("method_name", "")),
            "parameters": structured_data.get("parameters", []),
            "return_type": structured_data.get("return_type", row_data.get("return_type", "")),
            "expectations": structured_data.get("expectations", [])
        }
        
        gen_result = api.generate_test_code(request_data)
        
        if gen_result["success"]:
            test_code = gen_result["test_code"]
            empty_method = gen_result["empty_method"]
            
            excel.update_cell(i, "test_code", test_code)
            excel.update_cell(i, "empty_method", empty_method)
            excel.update_cell(i, "generate_time_ms", gen_result["time_ms"])
            
            print(f"   ✅ Generate success: {gen_result['time_ms']}ms")
            success_count += 1
            
            compile_result = CompilationUtil.compile_test_code(
                package_name=row_data.get("package_name", "com.example"),
                class_name=row_data.get("class_name", "TestClass"),
                empty_method=empty_method,
                test_code=test_code
            )
            
            if compile_result["success"]:
                excel.update_cell(i, "compile_success", True)
                excel.update_cell(i, "compile_error", "")
                print(f"   ✅ Compile success")
                compile_success_count += 1
            else:
                error_summary = CompilationUtil.extract_error_summary(compile_result.get("stderr", ""))
                excel.update_cell(i, "compile_success", False)
                excel.update_cell(i, "compile_error", error_summary)
                print(f"   ❌ Compile failed: {error_summary[:50]}...")
                compile_fail_count += 1
        else:
            print(f"   ❌ Generate failed: {gen_result['error']}")
            excel.update_cell(i, "generate_time_ms", gen_result["time_ms"])
            api_fail_count += 1
        
        excel.save()
    
    print("\n" + "=" * 60)
    print("Step 3 Summary")
    print("=" * 60)
    print(f"Total: {total_rows}")
    print(f"API Success: {success_count}")
    print(f"API Failed: {api_fail_count}")
    print(f"Compile Success: {compile_success_count}")
    print(f"Compile Failed: {compile_fail_count}")
    print(f"Compile Success Rate: {compile_success_count}/{success_count} ({100*compile_success_count/max(success_count,1):.1f}%)")
    print(f"\n✅ Results saved to: {excel_path}")
    print("=" * 60)


if __name__ == "__main__":
    call_generate_test_code()
