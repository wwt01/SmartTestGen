"""
步骤2: 调用接口1和初始化接口
读取Excel中的测试数据，调用解析接口和初始化会话接口
"""

import os
import sys
import json
from datetime import datetime

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from utils.excel_manager import ExcelManager
from utils.api_client import APIClient


def call_parse_and_init():
    """调用解析接口和初始化接口"""
    print("=" * 60)
    print("Step 2: Calling Parse API and Init Session API")
    print(f"Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 60)
    
    results_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "results")
    excel_path = os.path.join(results_dir, "test_results.xlsx")
    
    if not os.path.exists(excel_path):
        print("❌ Error: test_results.xlsx not found. Please run step1_prepare_data.py first.")
        return
    
    excel = ExcelManager(excel_path)
    excel.load()
    
    api = APIClient()
    
    print("\nChecking API connection...")
    if not api.health_check():
        print("❌ Error: API server is not running. Please start the backend server first.")
        print("   Run: cd backend && python -m uvicorn app.main:app --reload")
        return
    
    print("✅ API server is running\n")
    
    total_rows = excel.get_row_count()
    print(f"Processing {total_rows} test cases...\n")
    
    success_count = 0
    fail_count = 0
    
    for i in range(1, total_rows + 1):
        row_data = excel.get_row(i)
        test_id = row_data.get("id", i)
        requirement = row_data.get("requirement", "")
        
        print(f"[{i}/{total_rows}] Processing ID {test_id}: {requirement[:40]}...")
        
        parse_result = api.parse_text(requirement)
        
        if parse_result["success"]:
            structured_data = parse_result["data"]
            structured_str = json.dumps(structured_data, ensure_ascii=False)
            
            excel.update_cell(i, "structured_result", structured_str)
            excel.update_cell(i, "parse_time_ms", parse_result["time_ms"])
            
            print(f"   ✅ Parse success: {parse_result['time_ms']}ms")
            
            session_data = {
                "class_name": row_data.get("class_name", ""),
                "is_interface": row_data.get("is_interface", False),
                "package_name": row_data.get("package_name", ""),
                "class_type": row_data.get("class_type", ""),
                "fields": json.loads(row_data.get("fields", "[]")),
                "methods": [],
                "dependencies": json.loads(row_data.get("dependencies", "[]"))
            }
            
            init_result = api.init_session(session_data)
            
            if init_result["success"]:
                excel.update_cell(i, "session_id", init_result["session_id"])
                print(f"   ✅ Init session success: {init_result['session_id'][:20]}...")
                success_count += 1
            else:
                print(f"   ❌ Init session failed: {init_result['error']}")
                fail_count += 1
        else:
            print(f"   ❌ Parse failed: {parse_result['error']}")
            excel.update_cell(i, "parse_time_ms", parse_result["time_ms"])
            fail_count += 1
        
        excel.save()
    
    print("\n" + "=" * 60)
    print("Step 2 Summary")
    print("=" * 60)
    print(f"Total: {total_rows}")
    print(f"Success: {success_count}")
    print(f"Failed: {fail_count}")
    print(f"\n✅ Results saved to: {excel_path}")
    print("=" * 60)


if __name__ == "__main__":
    call_parse_and_init()
