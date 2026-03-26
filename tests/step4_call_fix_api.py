"""
步骤4: 调用接口3修复编译错误
读取Excel中编译失败的测试数据，调用修复接口（最多3次）
"""

import os
import sys
import json
from datetime import datetime

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from utils.excel_manager import ExcelManager
from utils.api_client import APIClient
from utils.compilation_util import CompilationUtil

MAX_FIX_ATTEMPTS = 3


def call_fix_compilation_error():
    """调用修复编译错误接口"""
    print("=" * 60)
    print("Step 4: Calling Fix Compilation Error API")
    print(f"Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 60)
    
    results_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "results")
    excel_path = os.path.join(results_dir, "test_results.xlsx")
    
    if not os.path.exists(excel_path):
        print("❌ Error: test_results.xlsx not found. Please run step1-3 first.")
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
        print("❌ Error: javac is not available.")
        return
    
    print("✅ javac is available\n")
    
    total_rows = excel.get_row_count()
    
    failed_rows = []
    for i in range(1, total_rows + 1):
        row_data = excel.get_row(i)
        compile_success = row_data.get("compile_success", None)
        
        if compile_success is False:
            row_data["_row_index"] = i
            failed_rows.append(row_data)
    
    print(f"Found {len(failed_rows)} failed test cases to fix...\n")
    
    if not failed_rows:
        print("✅ All test cases compiled successfully. No need to fix.")
        return
    
    total_fixed = 0
    total_failed = 0
    
    for idx, row_data in enumerate(failed_rows, 1):
        row_index = row_data["_row_index"]
        test_id = row_data.get("id", row_index)
        requirement = row_data.get("requirement", "")
        session_id = row_data.get("session_id", "")
        test_code = row_data.get("test_code", "")
        empty_method = row_data.get("empty_method", "")
        compile_error = row_data.get("compile_error", "")
        
        print(f"[{idx}/{len(failed_rows)}] Fixing ID {test_id}: {requirement[:40]}...")
        print(f"   Error: {compile_error[:60]}...")
        
        current_code = test_code
        fix_count = 0
        final_success = False
        
        for attempt in range(1, MAX_FIX_ATTEMPTS + 1):
            print(f"   Attempt {attempt}/{MAX_FIX_ATTEMPTS}...", end=" ")
            
            request_data = {
                "session_id": session_id,
                "test_code": current_code,
                "empty_method": empty_method,
                "compilation_error": compile_error
            }
            
            fix_result = api.fix_compilation_error(request_data)
            
            if not fix_result["success"]:
                print(f"API failed: {fix_result['error']}")
                continue
            
            fixed_code = fix_result["fixed_code"]
            current_code = fixed_code
            fix_count += 1
            
            compile_result = CompilationUtil.compile_test_code(
                package_name=row_data.get("package_name", "com.example"),
                class_name=row_data.get("class_name", "TestClass"),
                empty_method=empty_method,
                test_code=fixed_code
            )
            
            fix_col = f"fix_code_{attempt}"
            success_col = f"fix_success_{attempt}"
            time_col = f"fix_time_{attempt}"
            
            excel.update_cell(row_index, fix_col, fixed_code)
            excel.update_cell(row_index, success_col, compile_result["success"])
            excel.update_cell(row_index, time_col, fix_result["time_ms"])
            
            if compile_result["success"]:
                print(f"✅ Fixed!")
                excel.update_cell(row_index, "test_code", fixed_code)
                excel.update_cell(row_index, "compile_success", True)
                excel.update_cell(row_index, "compile_error", "")
                final_success = True
                total_fixed += 1
                break
            else:
                new_error = CompilationUtil.extract_error_summary(compile_result.get("stderr", ""))
                compile_error = new_error
                print(f"Still failed")
        
        excel.update_cell(row_index, "final_success", final_success)
        excel.update_cell(row_index, "total_fix_count", fix_count)
        
        if not final_success:
            total_failed += 1
        
        excel.save()
    
    print("\n" + "=" * 60)
    print("Step 4 Summary")
    print("=" * 60)
    print(f"Total Failed Cases: {len(failed_rows)}")
    print(f"Fixed Successfully: {total_fixed}")
    print(f"Still Failed: {total_failed}")
    print(f"Fix Success Rate: {total_fixed}/{len(failed_rows)} ({100*total_fixed/max(len(failed_rows),1):.1f}%)")
    print(f"\n✅ Results saved to: {excel_path}")
    print("=" * 60)


if __name__ == "__main__":
    call_fix_compilation_error()
