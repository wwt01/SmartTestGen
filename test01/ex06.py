#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
解析指定Excel表格调用后端接口生成测试代码，将生成的测试代码注入表格。
"""

from utils.excel_manager import ExcelManager
from utils.api_client import APIClient
from utils.log_manager import logger
import os
import sys
import json
from datetime import datetime

# 添加utils模块路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))


def main():
    """主函数"""
    logger.info("=" * 70)
    logger.info("SmartTestGen ex06.py - Generate Test Code")
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

        success_count = 0
        fail_count = 0

        # 处理每一行
        for i in range(1, total_rows + 1):
            row_data = excel.get_row(i)
            test_id = row_data.get("id", i)
            description = row_data.get("description", "")
            session_id = row_data.get("session_id", "")

            if not description:
                logger.warning(f"[{i}/{total_rows}] Skipped: No description found for ID {test_id}")
                fail_count += 1
                continue

            if not session_id:
                logger.warning(f"[{i}/{total_rows}] Skipped: No session_id found for ID {test_id}")
                fail_count += 1
                continue

            logger.info(f"[{i}/{total_rows}] Processing ID {test_id}: {description[:40]}...")

            # 从structured_result中获取方法信息
            structured_result = row_data.get("structured_result", "")
            if not structured_result:
                logger.warning(f"[{i}/{total_rows}] Skipped: No structured_result found for ID {test_id}")
                fail_count += 1
                continue

            try:
                # 解析structured_result字段，它是一个JSON字符串
                structured_data = json.loads(structured_result)

                # 从structured_data中获取真正的结构化结果
                actual_structured_result = structured_data.get("structured_result", {})
                if not actual_structured_result:
                    logger.warning(f"[{i}/{total_rows}] Skipped: No actual structured_result found in JSON for ID {test_id}")
                    fail_count += 1
                    continue
            except json.JSONDecodeError as e:
                logger.error(f"[{i}/{total_rows}] Failed to parse structured_result: {e}")
                fail_count += 1
                continue

            # 调用生成测试代码接口
            generate_data = {
                "session_id": session_id,
                "method_name": actual_structured_result.get("method_name", ""),
                "parameters": actual_structured_result.get("parameters", []),
                "return_type": actual_structured_result.get("return_type", ""),
                "expectations": actual_structured_result.get("expectations", []),
                "is_static": actual_structured_result.get("is_static", False)
            }

            generate_result = api.generate_test_code(generate_data)

            if generate_result["success"]:
                test_code = generate_result["test_code"]
                empty_method = generate_result["empty_method"]
                generate_time_ms = generate_result.get("time_ms", 0)

                logger.info(f"   Test code generated: {generate_time_ms}ms")

                # 更新empty_method、generate_time_ms和test_code
                excel.update_cell(i, "empty_method", empty_method)
                excel.update_cell(i, "generate_time_ms", generate_time_ms)
                excel.update_cell(i, "test_code", test_code)
                success_count += 1
            else:
                logger.error(f"   Test code generation failed: {generate_result['error']}")
                excel.update_cell(i, "generate_error", generate_result["error"])
                fail_count += 1

            # 保存Excel文件
            excel.save()

        logger.info(f"{excel_file} Summary")
        logger.info(f"Total: {total_rows}")
        logger.info(f"Success: {success_count}")
        logger.info(f"Failed: {fail_count}")
        logger.info(f"Results saved to: {excel_path}")

    logger.info("=" * 70)
    logger.info("ex06.py completed successfully")
    logger.info("=" * 70)

    return 0


if __name__ == "__main__":
    sys.exit(main())
