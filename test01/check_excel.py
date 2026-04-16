#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""检查Excel文件中的file_path值"""

import sys
import os

# 添加项目根目录到路径
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from utils.excel_manager import ExcelManager

def main():
    excel_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "results", "test.xlsx")
    
    if not os.path.exists(excel_path):
        print(f"Excel文件不存在: {excel_path}")
        return 1
    
    excel = ExcelManager(excel_path)
    excel.load()
    
    total_rows = excel.get_row_count()
    print(f"总行数: {total_rows}")
    
    # 检查前5行的file_path值
    for i in range(1, min(6, total_rows + 1)):
        row_data = excel.get_row(i)
        test_id = row_data.get("id", i)
        file_path = row_data.get("file_path", "NOT FOUND")
        class_name = row_data.get("class_name", "NOT FOUND")
        print(f"\nRow {i} (ID: {test_id}):")
        print(f"  class_name: {class_name}")
        print(f"  file_path: {file_path}")
        
        # 检查file_path是否是Java文件
        if file_path.endswith(".java"):
            print(f"  ✓ 是Java文件")
        elif file_path.endswith(".xlsx"):
            print(f"  ✗ 是Excel文件！")
        else:
            print(f"  ? 未知文件类型")
    
    return 0

if __name__ == "__main__":
    sys.exit(main())
