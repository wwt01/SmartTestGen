#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
修复编译失败的测试代码，最多尝试三次。
"""

from utils.excel_manager import ExcelManager
from utils.compilation_util import CompilationUtil
from utils.api_client import APIClient
from utils.log_manager import logger
import os
import sys
from datetime import datetime

# 添加utils模块路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))


def main():
    """主函数"""
    logger.info("=" * 70)
    logger.info("SmartTestGen ex07.py - Fix Compilation Errors")
    logger.info(f"Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    logger.info("=" * 70)

    # 定义路径
    base_dir = os.path.dirname(os.path.abspath(__file__))
    results_dir = os.path.join(base_dir, "results")

    # 检查目录是否存在
    if not os.path.exists(results_dir):
        logger.error(f"Error: results directory not found at {results_dir}")
        return 1

    # 仅处理指定的Excel文件
    # 可以通过修改此列表来增加或删减文件
    target_files = [
        # "complex_group_select.xlsx",
        # "generic_group_select.xlsx",
        "simple_group_select.xlsx"
        # "test.xlsx"
        # "maths_group_select.xlsx"
    ]

    # 获取存在的Excel文件
    excel_files = []
    for file in target_files:
        file_path = os.path.join(results_dir, file)
        if os.path.exists(file_path):
            excel_files.append(file)
        else:
            logger.warning(f"Warning: {file} not found, skipping")

    if not excel_files:
        logger.error("Error: No target Excel files found")
        return 1

    # 检查javac是否可用
    if not CompilationUtil.check_javac_available():
        logger.error("Error: javac not found. Please install JDK.")
        return 1

    logger.info("javac is available")

    # 初始化API客户端
    api = APIClient()

    # 检查API连接
    logger.info("Checking API connection...")
    if not api.health_check():
        logger.error("Error: API server is not running. Please start the backend server first.")
        logger.error("Run: cd backend && python -m uvicorn app.main:app --reload")
        return 1

    logger.info("API server is running")

    # 处理每个Excel文件
    for excel_file in excel_files:
        excel_path = os.path.join(results_dir, excel_file)
        logger.info(f"Processing file: {excel_file}")

        # 加载Excel文件
        excel = ExcelManager(excel_path)
        excel.load()

        total_rows = excel.get_row_count()
        logger.info(f"Processing {total_rows} test cases in {excel_file}...")

        total_fixed = 0
        total_processed = 0

        # 处理每一行
        for i in range(1, total_rows + 1):
            row_data = excel.get_row(i)
            test_id = row_data.get("id", i)
            test_code = row_data.get("test_code", "")
            compilation_error = row_data.get("compilation_error", "") or row_data.get("compile_error", "")
            session_id = row_data.get("session_id", "")
            package_name = row_data.get("package_name", "")
            class_name = row_data.get("class_name", "")
            file_path = row_data.get("file_path", "")

            # 只处理编译失败的例子
            compile_success = row_data.get("compile_success", "")
            if compile_success == "True":
                logger.info(f"[{i}/{total_rows}] Skipped: Already compiled successfully for ID {test_id}")
                continue

            if not test_code:
                logger.warning(f"[{i}/{total_rows}] Skipped: No test_code found for ID {test_id}")
                continue

            if not compilation_error:
                logger.warning(f"[{i}/{total_rows}] Skipped: No compilation_error found for ID {test_id}")
                logger.warning(f"   Debug: compilation_error='{row_data.get('compilation_error', '')}', compile_error='{row_data.get('compile_error', '')}'")
                continue

            if not session_id:
                logger.warning(f"[{i}/{total_rows}] Skipped: No session_id found for ID {test_id}")
                continue

            if not package_name or not class_name:
                logger.warning(f"[{i}/{total_rows}] Skipped: No package_name or class_name found for ID {test_id}")
                continue

            if not file_path:
                logger.warning(f"[{i}/{total_rows}] Skipped: No file_path found for ID {test_id}")
                continue

            logger.info(f"[{i}/{total_rows}] Processing ID {test_id}: {class_name}.{package_name}")
            logger.info(f"   Compilation error: {compilation_error[:100]}...")

            total_processed += 1
            fix_count = 0
            final_success = False

            # 最多尝试三次修复
            for attempt in range(1, 4):
                logger.info(f"   Attempt {attempt}/3: Fixing compilation error")

                # 读取方法源码
                method_source = "No information available yet."
                if file_path and os.path.exists(file_path):
                    try:
                        with open(file_path, 'r', encoding='utf-8') as f:
                            method_source = f.read()
                        logger.info(f"   Read method source from: {file_path}")
                    except Exception as e:
                        logger.warning(f"   Failed to read method source: {str(e)}")

                # 调用修复编译错误接口
                fix_data = {
                    "test_code": test_code,
                    "compilation_error": compilation_error,
                    "session_id": session_id,
                    "method_source": method_source
                }

                fix_result = api.fix_compilation_error(fix_data)

                if fix_result["success"]:
                    fixed_code = fix_result["fixed_code"]
                    fix_time_ms = fix_result.get("time_ms", 0)

                    logger.info(f"   Fix completed: {fix_time_ms}ms")

                    # 编译修复后的代码
                    logger.info(f"   Compiling fixed test code")
                    compilation_result = CompilationUtil.compile_with_original_class(
                        package_name=package_name,
                        class_name=class_name,
                        test_code=fixed_code,
                        original_java_file=file_path
                    )

                    if compilation_result["success"]:
                        logger.info(f"   Compilation success after fix attempt {attempt}")
                        # 更新表格
                        excel.update_cell(i, f"fix_code_{attempt}", fixed_code)
                        excel.update_cell(i, f"fix_success_{attempt}", "True")
                        excel.update_cell(i, f"fix_compile_error_{attempt}", "")
                        excel.update_cell(i, f"fix_time_{attempt}", fix_time_ms)
                        fix_count += 1
                        final_success = True
                        break
                    else:
                        error_message = CompilationUtil.extract_error_summary(compilation_result["error_message"])
                        logger.error(f"   Compilation failed after fix attempt {attempt}: {error_message}")
                        # 更新表格
                        excel.update_cell(i, f"fix_code_{attempt}", fixed_code)
                        excel.update_cell(i, f"fix_success_{attempt}", "False")
                        excel.update_cell(i, f"fix_compile_error_{attempt}", error_message)
                        excel.update_cell(i, f"fix_time_{attempt}", fix_time_ms)
                        # 继续下一次尝试
                        test_code = fixed_code
                        compilation_error = error_message
                        fix_count += 1
                else:
                    logger.error(f"   Fix failed: {fix_result['error']}")
                    # 更新表格
                    excel.update_cell(i, f"fix_code_{attempt}", "")
                    excel.update_cell(i, f"fix_success_{attempt}", "False")
                    excel.update_cell(i, f"fix_compile_error_{attempt}", fix_result["error"])
                    excel.update_cell(i, f"fix_time_{attempt}", fix_result.get("time_ms", 0))
                    fix_count += 1

                # 保存Excel文件
                excel.save()

            # 更新最终结果
            excel.update_cell(i, "final_success", "True" if final_success else "False")
            excel.update_cell(i, "total_fix_count", fix_count)

            if final_success:
                logger.info(f"   Final result: SUCCESS after {fix_count} attempt(s)")
                total_fixed += 1
            else:
                logger.info(f"   Final result: FAILED after {fix_count} attempt(s)")

            # 保存Excel文件
            excel.save()

        logger.info(f"{excel_file} Summary")
        logger.info(f"Total processed: {total_processed}")
        logger.info(f"Total fixed: {total_fixed}")
        logger.info(f"Success rate: {total_fixed / total_processed * 100:.1f}%" if total_processed > 0 else "N/A")
        logger.info(f"Results saved to: {excel_path}")

    logger.info("=" * 70)
    logger.info("ex07.py completed successfully")
    logger.info("=" * 70)

    return 0


if __name__ == "__main__":
    sys.exit(main())
