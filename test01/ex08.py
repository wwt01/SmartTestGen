#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
运行测试代码，获取每个测试用例的运行通过情况。
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
    logger.info("SmartTestGen ex08.py - Run Test Cases")
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
        # "simple_group_select.xlsx"
        # "maths_group_select.xlsx"
        "test.xlsx"
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

    # 检查java是否可用
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

        total_run = 0
        total_passed = 0
        total_failed = 0
        total_coverage = 0.0

        # 处理每一行
        for i in range(1, total_rows + 1):
            row_data = excel.get_row(i)
            test_id = row_data.get("id", i)
            package_name = row_data.get("package_name", "")
            class_name = row_data.get("class_name", "")
            file_path = row_data.get("file_path", "")

            # 检查编译状态
            compile_success = row_data.get("compile_success", "")
            fix_success_1 = row_data.get("fix_success_1", "")
            fix_success_2 = row_data.get("fix_success_2", "")
            fix_success_3 = row_data.get("fix_success_3", "")

            # 确定使用哪个测试代码
            test_code = None
            if compile_success == "True":
                test_code = row_data.get("test_code", "")
                logger.info(f"[{i}/{total_rows}] Using original test code for ID {test_id}")
            elif fix_success_3 == "True":
                test_code = row_data.get("fix_code_3", "")
                logger.info(f"[{i}/{total_rows}] Using fix_code_3 for ID {test_id}")
            elif fix_success_2 == "True":
                test_code = row_data.get("fix_code_2", "")
                logger.info(f"[{i}/{total_rows}] Using fix_code_2 for ID {test_id}")
            elif fix_success_1 == "True":
                test_code = row_data.get("fix_code_1", "")
                logger.info(f"[{i}/{total_rows}] Using fix_code_1 for ID {test_id}")
            else:
                logger.info(f"[{i}/{total_rows}] Skipped: No compiled test code found for ID {test_id}")
                continue

            if not test_code:
                logger.warning(f"[{i}/{total_rows}] Skipped: No test_code found for ID {test_id}")
                continue

            if not package_name or not class_name:
                logger.warning(f"[{i}/{total_rows}] Skipped: No package_name or class_name found for ID {test_id}")
                continue

            if not file_path:
                logger.warning(f"[{i}/{total_rows}] Skipped: No file_path found for ID {test_id}")
                continue

            # 第一阶段：测试正确代码
            logger.info(f"[{i}/{total_rows}] Running tests on correct code for ID {test_id}: {class_name}.{package_name}")

            # 运行测试（正确代码）
            correct_test_result = CompilationUtil.run_test(
                package_name=package_name,
                class_name=class_name,
                original_java_file=file_path,
                test_code=test_code,
                use_wrong_code=False
            )

            if correct_test_result["success"]:
                total_tests = correct_test_result["total_tests"]
                passed_tests_correct = correct_test_result["passed_tests"]
                failed_tests_correct = correct_test_result["failed_tests"]
                rate_correct = passed_tests_correct / total_tests if total_tests > 0 else 0
                coverage = correct_test_result.get("coverage", 0.0)

                logger.info(f"   Correct code test execution completed")
                logger.info(f"   Total tests: {total_tests}")
                logger.info(f"   Passed: {passed_tests_correct}")
                logger.info(f"   Failed: {failed_tests_correct}")
                logger.info(f"   Rate correct: {rate_correct:.2f}")
                logger.info(f"   Coverage: {coverage:.2f}%")

                # 打印测试运行时间信息
                if 'Test run finished after' in correct_test_result.get('stdout', ''):
                    import re
                    match = re.search(r'Test run finished after (.+)', correct_test_result['stdout'])
                    if match:
                        logger.info(f"   Execution time: {match.group(1)}")

                # 第二阶段：如果有通过的测试用例，测试错误代码
                if passed_tests_correct > 0:
                    logger.info(f"[{i}/{total_rows}] Running tests on wrong code for ID {test_id}: {class_name}.{package_name}")

                    # 运行测试（错误代码）
                    wrong_test_result = CompilationUtil.run_test(
                        package_name=package_name,
                        class_name=class_name,
                        original_java_file=file_path,
                        test_code=test_code,
                        use_wrong_code=True
                    )

                    if wrong_test_result["success"]:
                        passed_tests_wrong = wrong_test_result["passed_tests"]
                        failed_tests_wrong = wrong_test_result["failed_tests"]
                        rate_wrong = failed_tests_wrong / total_tests if total_tests > 0 else 0

                        logger.info(f"   Wrong code test execution completed")
                        logger.info(f"   Passed: {passed_tests_wrong}")
                        logger.info(f"   Failed: {failed_tests_wrong}")
                        logger.info(f"   Rate wrong: {rate_wrong:.2f}")

                        # 打印测试运行时间信息
                        if 'Test run finished after' in wrong_test_result.get('stdout', ''):
                            import re
                            match = re.search(r'Test run finished after (.+)', wrong_test_result['stdout'])
                            if match:
                                logger.info(f"   Execution time: {match.group(1)}")
                    else:
                        error_message = wrong_test_result.get("error_message", "Unknown error")
                        logger.error(f"   Wrong code test execution failed: {error_message}")
                        passed_tests_wrong = 0
                        failed_tests_wrong = 0
                        rate_wrong = 0
                else:
                    logger.info(f"   Skipping wrong code test: No passed tests in correct code")
                    passed_tests_wrong = 0
                    failed_tests_wrong = 0
                    rate_wrong = 0

                # 更新Excel表格
                excel.update_cell(i, "run_success", "True")
                excel.update_cell(i, "total_tests", total_tests)
                excel.update_cell(i, "passed_tests", passed_tests_correct)
                excel.update_cell(i, "failed_tests", failed_tests_correct)
                excel.update_cell(i, "passed_tests_correct", passed_tests_correct)
                excel.update_cell(i, "failed_tests_correct", failed_tests_correct)
                excel.update_cell(i, "rate_correct", rate_correct)
                excel.update_cell(i, "passed_tests_wrong", passed_tests_wrong)
                excel.update_cell(i, "failed_tests_wrong", failed_tests_wrong)
                excel.update_cell(i, "rate_wrong", rate_wrong)
                excel.update_cell(i, "coverage", coverage)

                total_run += 1
                total_passed += passed_tests_correct
                total_failed += failed_tests_correct
                total_coverage += coverage
            else:
                error_message = correct_test_result.get("error_message", "Unknown error")
                logger.error(f"   Correct code test execution failed: {error_message}")
                excel.update_cell(i, "run_success", "False")
                excel.update_cell(i, "run_error", error_message)

            # 保存Excel文件
            excel.save()

        logger.info(f"{excel_file} Summary")
        logger.info(f"Total run: {total_run}")
        logger.info(f"Total passed tests: {total_passed}")
        logger.info(f"Total failed tests: {total_failed}")
        if total_passed + total_failed > 0:
            pass_rate = (total_passed / (total_passed + total_failed)) * 100
            logger.info(f"Overall pass rate: {pass_rate:.1f}%")
        else:
            logger.info("Overall pass rate: N/A")
        if total_run > 0:
            avg_coverage = total_coverage / total_run
            logger.info(f"Average coverage: {avg_coverage:.2f}%")
        else:
            logger.info("Average coverage: N/A")
        logger.info(f"Results saved to: {excel_path}")

    logger.info("=" * 70)
    logger.info("ex08.py completed successfully")
    logger.info("=" * 70)

    return 0


if __name__ == "__main__":
    sys.exit(main())
