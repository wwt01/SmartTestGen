"""
测试集分析和方法选择模块
分析测试集，选择适合的方法进行测试
"""

import os
import json
from datetime import datetime

class TestAnalyzer:
    """测试集分析器"""
    
    def __init__(self, test_data=None):
        """初始化测试集分析器"""
        self.test_data = test_data or []
    
    def load_from_json(self, json_path):
        """从JSON文件加载测试数据"""
        if not os.path.exists(json_path):
            print(f"File not found: {json_path}")
            return False
        
        try:
            with open(json_path, 'r', encoding='utf-8') as f:
                self.test_data = json.load(f)
            print(f"Loaded {len(self.test_data)} test cases from {json_path}")
            return True
        except Exception as e:
            print(f"Error loading test data: {e}")
            return False
    
    def analyze_test_data(self):
        """分析测试数据"""
        if not self.test_data:
            print("No test data to analyze")
            return
        
        print("=" * 70)
        print("Test Data Analysis")
        print(f"Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print("=" * 70)
        
        # 统计信息
        total_cases = len(self.test_data)
        interface_cases = sum(1 for case in self.test_data if case['is_interface'])
        class_cases = total_cases - interface_cases
        
        # 按类类型统计
        class_types = {}
        for case in self.test_data:
            class_type = case['class_type']
            if class_type not in class_types:
                class_types[class_type] = 0
            class_types[class_type] += 1
        
        # 按返回类型统计
        return_types = {}
        for case in self.test_data:
            return_type = case['return_type']
            if return_type not in return_types:
                return_types[return_type] = 0
            return_types[return_type] += 1
        
        print(f"Total test cases: {total_cases}")
        print(f"Interface cases: {interface_cases}")
        print(f"Class cases: {class_cases}")
        print("\nClass types:")
        for class_type, count in class_types.items():
            print(f"  {class_type}: {count}")
        print("\nReturn types:")
        for return_type, count in return_types.items():
            if count >= 2:  # 只显示出现2次以上的返回类型
                print(f"  {return_type}: {count}")
        
        print("=" * 70)
    
    def select_test_cases(self, max_cases=20):
        """选择测试用例"""
        if not self.test_data:
            print("No test data to select from")
            return []
        
        # 按类型分组
        interface_cases = [case for case in self.test_data if case['is_interface']]
        class_cases = [case for case in self.test_data if not case['is_interface']]
        
        # 选择一定比例的接口和类测试用例
        interface_count = min(len(interface_cases), max_cases // 3)
        class_count = min(len(class_cases), max_cases - interface_count)
        
        selected_cases = interface_cases[:interface_count] + class_cases[:class_count]
        
        # 确保测试用例数量不超过限制
        selected_cases = selected_cases[:max_cases]
        
        print(f"Selected {len(selected_cases)} test cases for testing")
        print("=" * 70)
        
        return selected_cases
    
    def validate_test_cases(self):
        """验证测试用例的有效性"""
        valid_cases = []
        invalid_cases = []
        
        for case in self.test_data:
            # 检查必要字段
            required_fields = ['id', 'requirement', 'class_name', 'method_name', 'parameters', 'return_type']
            is_valid = True
            
            for field in required_fields:
                if field not in case or not case[field]:
                    is_valid = False
                    break
            
            if is_valid:
                valid_cases.append(case)
            else:
                invalid_cases.append(case)
        
        print(f"Valid test cases: {len(valid_cases)}")
        print(f"Invalid test cases: {len(invalid_cases)}")
        
        return valid_cases, invalid_cases
    
    def export_selected_cases(self, selected_cases, output_path):
        """导出选择的测试用例"""
        if not selected_cases:
            print("No selected cases to export")
            return False
        
        try:
            # 确保输出目录存在
            output_dir = os.path.dirname(output_path)
            if output_dir:
                os.makedirs(output_dir, exist_ok=True)
            
            # 保存为JSON文件
            with open(output_path, 'w', encoding='utf-8') as f:
                json.dump(selected_cases, f, ensure_ascii=False, indent=2)
            
            print(f"Exported {len(selected_cases)} selected test cases to {output_path}")
            return True
        except Exception as e:
            print(f"Error exporting selected cases: {e}")
            return False

def main():
    """主函数"""
    analyzer = TestAnalyzer()
    
    # 加载爬取的测试数据
    json_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "results", "crawled_test_data.json")
    analyzer.load_from_json(json_path)
    
    # 分析测试数据
    analyzer.analyze_test_data()
    
    # 验证测试用例
    valid_cases, invalid_cases = analyzer.validate_test_cases()

    # ✅ 只保留有效用例
    analyzer.test_data = valid_cases

    # 选择测试用例
    selected_cases = analyzer.select_test_cases(max_cases=20)
    
    # 导出选择的测试用例
    output_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "results", "selected_test_cases.json")
    analyzer.export_selected_cases(selected_cases, output_path)

if __name__ == "__main__":
    main()
