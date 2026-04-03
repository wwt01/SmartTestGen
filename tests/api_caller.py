"""
API调用统一脚本
合并所有API调用功能，支持不同步骤的执行
"""

from utils.log_manager import logger
from utils.compilation_util import CompilationUtil
from utils.api_client import APIClient
from utils.excel_manager import ExcelManager
import os
import sys
import json
import argparse
from datetime import datetime

# 添加utils模块路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))


MAX_FIX_ATTEMPTS = 3


def call_parse_and_init():
    """调用解析接口和初始化接口"""
    logger.info("Step 2: Calling Parse API and Init Session API")
    logger.info(f"Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")

    results_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "results")
    excel_path = os.path.join(results_dir, "test_results.xlsx")

    if not os.path.exists(excel_path):
        logger.error("Error: test_results.xlsx not found. Please run step1_prepare_data.py first.")
        return False

    excel = ExcelManager(excel_path)
    excel.load()

    api = APIClient()

    logger.info("Checking API connection...")
    if not api.health_check():
        logger.error("Error: API server is not running. Please start the backend server first.")
        logger.error("Run: cd backend && python -m uvicorn app.main:app --reload")
        return False

    logger.info("API server is running")

    total_rows = excel.get_row_count()
    logger.info(f"Processing {total_rows} test cases...")

    success_count = 0
    fail_count = 0

    for i in range(1, total_rows + 1):
        row_data = excel.get_row(i)
        test_id = row_data.get("id", i)
        requirement = row_data.get("requirement", "")

        logger.info(f"[{i}/{total_rows}] Processing ID {test_id}: {requirement[:40]}...")

        parse_result = api.parse_text(requirement)

        if parse_result["success"]:
            structured_data = parse_result["data"]
            structured_str = json.dumps(structured_data, ensure_ascii=False)

            excel.update_cell(i, "structured_result", structured_str)
            excel.update_cell(i, "parse_time_ms", parse_result["time_ms"])

            logger.info(f"   Parse success: {parse_result['time_ms']}ms")

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
                logger.info(f"   Init session success: {init_result['session_id'][:20]}...")
                success_count += 1
            else:
                logger.error(f"   Init session failed: {init_result['error']}")
                fail_count += 1
        else:
            logger.error(f"   Parse failed: {parse_result['error']}")
            excel.update_cell(i, "parse_time_ms", parse_result["time_ms"])
            fail_count += 1

        excel.save()

    logger.info("Step 2 Summary")
    logger.info(f"Total: {total_rows}")
    logger.info(f"Success: {success_count}")
    logger.info(f"Failed: {fail_count}")
    logger.info(f"Results saved to: {excel_path}")

    return True


def call_generate_test_code():
    """调用生成测试代码接口"""
    logger.info("Step 3: Calling Generate Test Code API")
    logger.info(f"Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")

    results_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "results")
    excel_path = os.path.join(results_dir, "test_results.xlsx")

    if not os.path.exists(excel_path):
        logger.error("Error: test_results.xlsx not found. Please run step1 and step2 first.")
        return False

    excel = ExcelManager(excel_path)
    excel.load()

    api = APIClient()

    logger.info("Checking API connection...")
    if not api.health_check():
        logger.error("Error: API server is not running.")
        return False

    logger.info("API server is running")

    logger.info("Checking javac availability...")
    if not CompilationUtil.check_javac_available():
        logger.error("Error: javac is not available. Please install JDK.")
        return False

    logger.info("javac is available")

    total_rows = excel.get_row_count()
    logger.info(f"Processing {total_rows} test cases...")

    success_count = 0
    compile_success_count = 0
    compile_fail_count = 0
    api_fail_count = 0

    for i in range(1, total_rows + 1):
        row_data = excel.get_row(i)
        test_id = row_data.get("id", i)
        requirement = row_data.get("requirement", "")
        session_id = row_data.get("session_id", "")

        logger.info(f"[{i}/{total_rows}] Processing ID {test_id}: {requirement[:40]}...")

        if not session_id:
            logger.warning("   Skipped: No session_id found")
            continue

        structured_result = row_data.get("structured_result", "")
        if not structured_result:
            logger.warning("   Skipped: No structured_result found")
            continue

        try:
            structured_data = json.loads(structured_result)
        except json.JSONDecodeError:
            logger.warning("   Skipped: Invalid structured_result JSON")
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

            logger.info(f"   Generate success: {gen_result['time_ms']}ms")
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
                logger.info("   Compile success")
                compile_success_count += 1
            else:
                error_summary = CompilationUtil.extract_error_summary(compile_result.get("stderr", ""))
                excel.update_cell(i, "compile_success", False)
                excel.update_cell(i, "compile_error", error_summary)
                logger.error(f"   Compile failed: {error_summary[:50]}...")
                compile_fail_count += 1
        else:
            logger.error(f"   Generate failed: {gen_result['error']}")
            excel.update_cell(i, "generate_time_ms", gen_result["time_ms"])
            api_fail_count += 1

        excel.save()

    logger.info("Step 3 Summary")
    logger.info(f"Total: {total_rows}")
    logger.info(f"API Success: {success_count}")
    logger.info(f"API Failed: {api_fail_count}")
    logger.info(f"Compile Success: {compile_success_count}")
    logger.info(f"Compile Failed: {compile_fail_count}")
    logger.info(f"Compile Success Rate: {compile_success_count}/{success_count} ({100 * compile_success_count / max(success_count, 1):.1f}%)")
    logger.info(f"Results saved to: {excel_path}")

    return True


def call_fix_compilation_error():
    """调用修复编译错误接口"""
    logger.info("Step 4: Calling Fix Compilation Error API")
    logger.info(f"Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")

    results_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "results")
    excel_path = os.path.join(results_dir, "test_results.xlsx")

    if not os.path.exists(excel_path):
        logger.error("Error: test_results.xlsx not found. Please run step1-3 first.")
        return False

    excel = ExcelManager(excel_path)
    excel.load()

    api = APIClient()

    logger.info("Checking API connection...")
    if not api.health_check():
        logger.error("Error: API server is not running.")
        return False

    logger.info("API server is running")

    logger.info("Checking javac availability...")
    if not CompilationUtil.check_javac_available():
        logger.error("Error: javac is not available.")
        return False

    logger.info("javac is available")

    total_rows = excel.get_row_count()

    failed_rows = []
    for i in range(1, total_rows + 1):
        row_data = excel.get_row(i)
        compile_success = row_data.get("compile_success", None)

        if compile_success is False:
            row_data["_row_index"] = i
            failed_rows.append(row_data)

    logger.info(f"Found {len(failed_rows)} failed test cases to fix...")

    if not failed_rows:
        logger.info("All test cases compiled successfully. No need to fix.")
        return True

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

        logger.info(f"[{idx}/{len(failed_rows)}] Fixing ID {test_id}: {requirement[:40]}...")
        logger.info(f"   Error: {compile_error[:60]}...")

        current_code = test_code
        fix_count = 0
        final_success = False

        for attempt in range(1, MAX_FIX_ATTEMPTS + 1):
            logger.info(f"   Attempt {attempt}/{MAX_FIX_ATTEMPTS}...")

            request_data = {
                "session_id": session_id,
                "test_code": current_code,
                "empty_method": empty_method,
                "compilation_error": compile_error
            }

            fix_result = api.fix_compilation_error(request_data)

            if not fix_result["success"]:
                logger.error(f"   API failed: {fix_result['error']}")
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
                logger.info("   Fixed!")
                excel.update_cell(row_index, "test_code", fixed_code)
                excel.update_cell(row_index, "compile_success", True)
                excel.update_cell(row_index, "compile_error", "")
                final_success = True
                total_fixed += 1
                break
            else:
                new_error = CompilationUtil.extract_error_summary(compile_result.get("stderr", ""))
                compile_error = new_error
                logger.info("   Still failed")

        excel.update_cell(row_index, "final_success", final_success)
        excel.update_cell(row_index, "total_fix_count", fix_count)

        if not final_success:
            total_failed += 1

        excel.save()

    logger.info("Step 4 Summary")
    logger.info(f"Total Failed Cases: {len(failed_rows)}")
    logger.info(f"Fixed Successfully: {total_fixed}")
    logger.info(f"Still Failed: {total_failed}")
    logger.info(f"Fix Success Rate: {total_fixed}/{len(failed_rows)} ({100 * total_fixed / max(len(failed_rows), 1):.1f}%)")
    logger.info(f"Results saved to: {excel_path}")

    return True


def main():
    """主函数"""
    parser = argparse.ArgumentParser(description="API Caller for SmartTestGen")
    parser.add_argument(
        "--step",
        choices=["parse", "generate", "fix", "all"],
        default="all",
        help="Specify which step to run"
    )

    args = parser.parse_args()

    logger.info("=" * 70)
    logger.info("SmartTestGen API Caller")
    logger.info(f"Step: {args.step}")
    logger.info(f"Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    logger.info("=" * 70)

    if args.step == "parse" or args.step == "all":
        if not call_parse_and_init():
            logger.error("Parse and init step failed")
            return 1

    if args.step == "generate" or args.step == "all":
        if not call_generate_test_code():
            logger.error("Generate step failed")
            return 1

    if args.step == "fix" or args.step == "all":
        if not call_fix_compilation_error():
            logger.error("Fix step failed")
            return 1

    logger.info("=" * 70)
    logger.info("API Caller completed successfully")
    logger.info("=" * 70)

    return 0


if __name__ == "__main__":
    sys.exit(main())
