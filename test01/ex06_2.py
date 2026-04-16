#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
从Excel表格中读取测试代码和源文件路径，进行编译验证。
"""

from utils.excel_manager import ExcelManager
from utils.compilation_util import CompilationUtil
from utils.log_manager import logger
import os
import sys
from datetime import datetime

# 添加utils模块路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))


def main():
    """主函数"""
    logger.info("=" * 70)
    logger.info("SmartTestGen ex06_2.py - Compile Test Code")
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

    # 处理每个Excel文件
    for excel_file in excel_files:
        excel_path = os.path.join(results_dir, excel_file)
        logger.info(f"Processing file: {excel_file}")

        # 加载Excel文件
        excel = ExcelManager(excel_path)
        excel.load()

        total_rows = excel.get_row_count()
        logger.info(f"Processing {total_rows} test cases in {excel_file}...")

        success_count = 0
        fail_count = 0

        # 处理每一行
        for i in range(1, total_rows + 1):
            row_data = excel.get_row(i)
            test_id = row_data.get("id", i)
            test_code = row_data.get("test_code", "")
            package_name = row_data.get("package_name", "")
            class_name = row_data.get("class_name", "")
            file_path = row_data.get("file_path", "")

            if not test_code:
                logger.warning(f"[{i}/{total_rows}] Skipped: No test_code found for ID {test_id}")
                fail_count += 1
                continue

            if not package_name or not class_name:
                logger.warning(f"[{i}/{total_rows}] Skipped: No package_name or class_name found for ID {test_id}")
                fail_count += 1
                continue

            if not file_path:
                logger.warning(f"[{i}/{total_rows}] Skipped: No file_path found for ID {test_id}")
                fail_count += 1
                continue

            logger.info(f"[{i}/{total_rows}] Processing ID {test_id}: {class_name}.{package_name}")

            # 检查源文件是否存在
            if not os.path.exists(file_path):
                logger.error(f"   Original Java file not found: {file_path}")
                excel.update_cell(i, "compilation_result", "False")
                excel.update_cell(i, "compilation_error", f"Original Java file not found: {file_path}")
                excel.update_cell(i, "compile_success", "False")
                fail_count += 1
                continue

            # 编译测试代码
            logger.info(f"   Compiling test code with original class: {file_path}")
            compilation_result = CompilationUtil.compile_with_original_class(
                package_name=package_name,
                class_name=class_name,
                test_code=test_code,
                original_java_file=file_path
            )

            if compilation_result["success"]:
                logger.info("   Compilation success")
                excel.update_cell(i, "compilation_result", "True")
                excel.update_cell(i, "compilation_error", "")
                excel.update_cell(i, "compile_success", "True")
                excel.update_cell(i, "compile_error", "")
                success_count += 1
            else:
                error_message = CompilationUtil.extract_error_summary(compilation_result["error_message"])
                logger.error(f"   Compilation failed: {error_message}")
                excel.update_cell(i, "compilation_result", "False")
                excel.update_cell(i, "compilation_error", error_message)
                excel.update_cell(i, "compile_success", "False")
                excel.update_cell(i, "compile_error", error_message)
                fail_count += 1

            # 保存Excel文件
            excel.save()

        logger.info(f"{excel_file} Summary")
        logger.info(f"Total: {total_rows}")
        logger.info(f"Success: {success_count}")
        logger.info(f"Failed: {fail_count}")
        logger.info(f"Results saved to: {excel_path}")

    logger.info("=" * 70)
    logger.info("ex06_2.py completed successfully")
    logger.info("=" * 70)

    return 0


if __name__ == "__main__":
    sys.exit(main())
