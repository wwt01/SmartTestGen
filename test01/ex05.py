#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
提取results文件夹下Excel表格的description信息，调用后端接口生成结构化信息并注入表格
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
    logger.info("SmartTestGen ex05.py")
    logger.info(f"Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    logger.info("=" * 70)

    # 定义路径
    base_dir = os.path.dirname(os.path.abspath(__file__))
    results_dir = os.path.join(base_dir, "results")

    # 检查目录是否存在
    if not os.path.exists(results_dir):
        logger.error(f"Error: results directory not found at {results_dir}")
        return 1

    # 仅处理指定的三个Excel文件
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

            if not description:
                logger.warning(f"[{i}/{total_rows}] Skipped: No description found for ID {test_id}")
                fail_count += 1
                continue

            logger.info(f"[{i}/{total_rows}] Processing ID {test_id}: {description[:40]}...")

            # 调用parse接口
            parse_result = api.parse_text(description)

            if parse_result["success"]:
                structured_data = parse_result["data"]
                structured_str = json.dumps(structured_data, ensure_ascii=False)

                # 更新结构化结果
                excel.update_cell(i, "structured_result", structured_str)
                excel.update_cell(i, "parse_time_ms", parse_result["time_ms"])

                logger.info(f"   Parse success: {parse_result['time_ms']}ms")
                success_count += 1
            else:
                logger.error(f"   Parse failed: {parse_result['error']}")
                excel.update_cell(i, "parse_time_ms", parse_result["time_ms"])
                fail_count += 1

            # 保存Excel文件
            excel.save()

        logger.info(f"{excel_file} Summary")
        logger.info(f"Total: {total_rows}")
        logger.info(f"Success: {success_count}")
        logger.info(f"Failed: {fail_count}")
        logger.info(f"Results saved to: {excel_path}")

    logger.info("=" * 70)
    logger.info("ex05.py completed successfully")
    logger.info("=" * 70)

    return 0


if __name__ == "__main__":
    sys.exit(main())
