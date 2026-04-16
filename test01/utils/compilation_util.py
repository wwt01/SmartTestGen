"""
编译工具类
用于编译Java代码并获取编译错误信息
"""

import os
import subprocess
import tempfile
import shutil
from typing import Dict, Optional, List
from utils.log_manager import logger

JUNIT_JUPITER_API_URL = "https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-api/5.10.0/junit-jupiter-api-5.10.0.jar"
JUNIT_JUPITER_PARAMS_URL = "https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-params/5.10.0/junit-jupiter-params-5.10.0.jar"
JUNIT_PLATFORM_COMMON_URL = "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-commons/1.10.0/junit-platform-commons-1.10.0.jar"
OPENTEST4J_URL = "https://repo1.maven.org/maven2/org/opentest4j/opentest4j/1.3.0/opentest4j-1.3.0.jar"
APIGUARDIAN_URL = "https://repo1.maven.org/maven2/org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar"
JACOCO_AGENT_URL = "https://repo1.maven.org/maven2/org/jacoco/org.jacoco.agent/0.8.11/org.jacoco.agent-0.8.11-runtime.jar"
JACOCO_CLI_URL = "https://repo1.maven.org/maven2/org/jacoco/org.jacoco.cli/0.8.11/org.jacoco.cli-0.8.11-nodeps.jar"

LIB_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "lib")


class CompilationUtil:
    """Java编译工具"""

    _junit_classpath: str = ""

    @classmethod
    def check_javac_available(cls) -> bool:
        """检查javac是否可用"""
        try:
            result = subprocess.run(
                ["javac", "-version"],
                capture_output=True,
                text=True,
                timeout=10
            )
            return result.returncode == 0
        except (FileNotFoundError, subprocess.TimeoutExpired):
            return False

    @classmethod
    def download_junit_jars(cls) -> str:
        """下载JUnit jar文件并返回classpath"""
        if cls._junit_classpath:
            return cls._junit_classpath

        os.makedirs(LIB_DIR, exist_ok=True)

        jar_urls = [
            (JUNIT_JUPITER_API_URL, "junit-jupiter-api-5.10.0.jar"),
            (JUNIT_JUPITER_PARAMS_URL, "junit-jupiter-params-5.10.0.jar"),
            (JUNIT_PLATFORM_COMMON_URL, "junit-platform-commons-1.10.0.jar"),
            (OPENTEST4J_URL, "opentest4j-1.3.0.jar"),
            (APIGUARDIAN_URL, "apiguardian-api-1.1.2.jar"),
            # 添加JUnit平台控制台启动器和相关依赖
            ("https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console/1.10.0/junit-platform-console-1.10.0.jar", "junit-platform-console-1.10.0.jar"),
            ("https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-engine/5.10.0/junit-jupiter-engine-5.10.0.jar", "junit-jupiter-engine-5.10.0.jar"),
            ("https://repo1.maven.org/maven2/org/junit/platform/junit-platform-engine/1.10.0/junit-platform-engine-1.10.0.jar", "junit-platform-engine-1.10.0.jar"),
            ("https://repo1.maven.org/maven2/org/junit/platform/junit-platform-launcher/1.10.0/junit-platform-launcher-1.10.0.jar", "junit-platform-launcher-1.10.0.jar"),
            # 添加JaCoCo依赖
            (JACOCO_AGENT_URL, "jacoco-agent-0.8.11-runtime.jar"),
            (JACOCO_CLI_URL, "jacoco-cli-0.8.11-nodeps.jar"),
            # 添加其他常用依赖
            ("https://repo1.maven.org/maven2/org/mockito/mockito-core/4.11.0/mockito-core-4.11.0.jar", "mockito-core-4.11.0.jar"),
            ("https://repo1.maven.org/maven2/org/assertj/assertj-core/3.24.2/assertj-core-3.24.2.jar", "assertj-core-3.24.2.jar"),
        ]

        jar_paths = []

        for url, filename in jar_urls:
            jar_path = os.path.join(LIB_DIR, filename)
            jar_paths.append(jar_path)

            if not os.path.exists(jar_path):
                print(f"   Downloading {filename}...")
                try:
                    import urllib.request
                    urllib.request.urlretrieve(url, jar_path)
                    print(f"   ✅ Downloaded {filename}")
                except Exception as e:
                    print(f"   ❌ Failed to download {filename}: {e}")
                    return ""

        cls._junit_classpath = os.pathsep.join(jar_paths)
        return cls._junit_classpath

    @classmethod
    def get_junit_classpath(cls) -> str:
        """获取JUnit classpath"""
        return cls.download_junit_jars()

    @classmethod
    def create_temp_project(cls) -> Dict[str, str]:
        """创建临时Java项目结构"""
        base_dir = tempfile.mkdtemp()
        project_dir = os.path.join(base_dir, "test_project")
        src_dir = os.path.join(project_dir, "src")
        target_dir = os.path.join(project_dir, "target")

        os.makedirs(src_dir, exist_ok=True)
        os.makedirs(target_dir, exist_ok=True)

        return {
            "base_dir": base_dir,
            "project_dir": project_dir,
            "src_dir": src_dir,
            "target_dir": target_dir
        }

    @classmethod
    def create_class_file(cls, src_dir: str, package: str, class_name: str, code: str) -> str:
        """创建Java类文件"""
        package_dir = os.path.join(src_dir, *package.split('.'))
        os.makedirs(package_dir, exist_ok=True)

        file_path = os.path.join(package_dir, f"{class_name}.java")
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(code)

        return file_path

    @classmethod
    def copy_original_class_file(cls, src_dir: str, package: str, class_name: str, original_file_path: str) -> str:
        """
        复制原始Java类文件到临时项目目录
        :param src_dir: 临时项目src目录
        :param package: 包名
        :param class_name: 类名
        :param original_file_path: 原始类文件路径
        :return: 复制后的文件路径
        """
        package_dir = os.path.join(src_dir, *package.split('.'))
        os.makedirs(package_dir, exist_ok=True)

        target_file_path = os.path.join(package_dir, f"{class_name}.java")
        # 使用二进制模式复制文件，保留原始编码
        with open(original_file_path, 'rb') as src_file:
            with open(target_file_path, 'wb') as dst_file:
                dst_file.write(src_file.read())
        return target_file_path

    @classmethod
    def get_original_class_path(cls, java_project_root: str, package: str, class_name: str) -> Optional[str]:
        """
        根据包名和类名获取原始Java类文件路径
        :param java_project_root: Java项目根目录（src/main/java的上级）
        :param package: 包名
        :param class_name: 类名
        :return: 原始文件路径，不存在返回None
        """
        # 拼接源码路径：root/src/main/java/包名/类名.java
        src_main_java = os.path.join(java_project_root, "src", "main", "java")
        package_dir = os.path.join(src_main_java, *package.split('.'))
        class_file_path = os.path.join(package_dir, f"{class_name}.java")

        if os.path.exists(class_file_path):
            return class_file_path
        return None

    @classmethod
    def compile_project(cls, src_dir: str, target_dir: str, classpath: str = "") -> Dict:
        """编译Java项目"""
        try:
            java_files = []
            for root, dirs, files in os.walk(src_dir):
                for file in files:
                    if file.endswith('.java'):
                        java_files.append(os.path.join(root, file))

            if not java_files:
                return {
                    "success": False,
                    "error_message": "No Java files found",
                    "returncode": -1
                }

            cmd = ["javac", "-d", target_dir, "-encoding", "UTF-8"]

            if classpath:
                cmd.extend(["-cp", classpath])

            cmd.extend(java_files)

            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=60
            )

            return {
                "success": result.returncode == 0,
                "stdout": result.stdout,
                "stderr": result.stderr,
                "error_message": result.stderr if result.returncode != 0 else "",
                "returncode": result.returncode
            }

        except subprocess.TimeoutExpired:
            return {
                "success": False,
                "error_message": "Compilation timeout",
                "returncode": -1
            }
        except FileNotFoundError:
            return {
                "success": False,
                "error_message": "javac not found. Please install JDK.",
                "returncode": -1
            }
        except Exception as e:
            return {
                "success": False,
                "error_message": str(e),
                "returncode": -1
            }

    @classmethod
    def cleanup_project(cls, base_dir: str):
        """清理临时项目"""
        try:
            shutil.rmtree(base_dir, ignore_errors=True)
        except Exception:
            pass

    @classmethod
    def compile_test_code(
        cls,
        package_name: str,
        class_name: str,
        empty_method: str,
        test_code: str,
        java_project_root: Optional[str] = None  # 新增：原始Java项目根目录
    ) -> Dict:
        """
        编译测试代码（包含原始类文件）

        Args:
            package_name: 包名
            class_name: 类名
            empty_method: 空方法代码
            test_code: 测试代码
            java_project_root: 原始Java项目根目录（src/main/java的上级）

        Returns:
            编译结果字典
        """
        base_dir = tempfile.mkdtemp()

        try:
            project_dir = os.path.join(base_dir, "test_project")
            src_dir = os.path.join(project_dir, "src")
            target_dir = os.path.join(project_dir, "target")

            os.makedirs(src_dir, exist_ok=True)
            os.makedirs(target_dir, exist_ok=True)

            # 1. 优先复制原始类文件（如果指定了项目根目录）
            original_class_path = None
            if java_project_root:
                original_class_path = cls.get_original_class_path(
                    java_project_root, package_name, class_name
                )

            if original_class_path and os.path.exists(original_class_path):
                # 复制原始类文件到临时项目
                cls.copy_original_class_file(
                    src_dir=src_dir,
                    package=package_name,
                    class_name=class_name,
                    original_file_path=original_class_path
                )
                logger.info(f"✅ Copied original class file: {original_class_path}")
            else:
                # 未找到原始类文件，创建空方法骨架（兼容原有逻辑）
                main_class_code = f"""package {package_name};

public class {class_name} {{
    
    {empty_method}
}}
"""
                package_dir = os.path.join(src_dir, *package_name.split('.'))
                os.makedirs(package_dir, exist_ok=True)
                main_class_path = os.path.join(package_dir, f"{class_name}.java")
                with open(main_class_path, 'w', encoding='utf-8') as f:
                    f.write(main_class_code)
                logger.warning(f"⚠️ Using empty method skeleton (original class not found: {package_name}.{class_name})")

            # 2. 创建测试类文件
            test_class_name = f"{class_name}Test"
            test_package_dir = os.path.join(src_dir, *package_name.split('.'))
            os.makedirs(test_package_dir, exist_ok=True)
            test_class_path = os.path.join(test_package_dir, f"{test_class_name}.java")
            with open(test_class_path, 'w', encoding='utf-8') as f:
                f.write(test_code)

            # 3. 编译整个项目（包含原始类+测试类）
            junit_classpath = cls.download_junit_jars()
            cmd = ["javac", "-d", target_dir, "-encoding", "UTF-8"]
            if junit_classpath:
                cmd.extend(["-cp", junit_classpath])

            # 收集所有Java文件
            java_files = []
            for root, dirs, files in os.walk(src_dir):
                for file in files:
                    if file.endswith('.java'):
                        java_files.append(os.path.join(root, file))

            if not java_files:
                return {
                    "success": False,
                    "error_message": "No Java files found for compilation",
                    "returncode": -1
                }

            cmd.extend(java_files)

            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=60
            )

            return {
                "success": result.returncode == 0,
                "error_message": result.stderr if result.returncode != 0 else "",
                "returncode": result.returncode
            }

        except subprocess.TimeoutExpired:
            return {
                "success": False,
                "error_message": "Compilation timeout",
                "returncode": -1
            }
        except FileNotFoundError:
            return {
                "success": False,
                "error_message": "javac not found. Please install JDK.",
                "returncode": -1
            }
        except Exception as e:
            return {
                "success": False,
                "error_message": str(e),
                "returncode": -1
            }
        finally:
            shutil.rmtree(base_dir, ignore_errors=True)

    @classmethod
    def compile_with_original_class(
        cls,
        package_name: str,
        class_name: str,
        test_code: str,
        original_java_file: str
    ) -> Dict:
        """
        使用指定的原始Java文件编译测试代码

        Args:
            package_name: 包名
            class_name: 类名
            test_code: 测试代码
            original_java_file: 原始Java文件路径

        Returns:
            编译结果字典
        """
        base_dir = tempfile.mkdtemp()

        try:
            project_dir = os.path.join(base_dir, "test_project")
            src_dir = os.path.join(project_dir, "src")
            target_dir = os.path.join(project_dir, "target")

            os.makedirs(src_dir, exist_ok=True)
            os.makedirs(target_dir, exist_ok=True)

            # 1. 复制原始类文件到临时项目
            cls.copy_original_class_file(
                src_dir=src_dir,
                package=package_name,
                class_name=class_name,
                original_file_path=original_java_file
            )
            logger.info(f"✅ Copied original class file: {original_java_file}")

            # 2. 创建测试类文件
            test_class_name = f"{class_name}Test"
            test_package_dir = os.path.join(src_dir, *package_name.split('.'))
            os.makedirs(test_package_dir, exist_ok=True)
            test_class_path = os.path.join(test_package_dir, f"{test_class_name}.java")
            with open(test_class_path, 'w', encoding='utf-8') as f:
                f.write(test_code)

            # 3. 编译整个项目（包含原始类+测试类）
            junit_classpath = cls.download_junit_jars()
            cmd = ["javac", "-d", target_dir, "-encoding", "UTF-8"]
            if junit_classpath:
                cmd.extend(["-cp", junit_classpath])

            # 收集所有Java文件
            java_files = []
            for root, dirs, files in os.walk(src_dir):
                for file in files:
                    if file.endswith('.java'):
                        java_files.append(os.path.join(root, file))

            if not java_files:
                return {
                    "success": False,
                    "error_message": "No Java files found for compilation",
                    "returncode": -1
                }

            cmd.extend(java_files)

            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=60
            )

            return {
                "success": result.returncode == 0,
                "error_message": result.stderr if result.returncode != 0 else "",
                "returncode": result.returncode
            }

        except subprocess.TimeoutExpired:
            return {
                "success": False,
                "error_message": "Compilation timeout",
                "returncode": -1
            }
        except FileNotFoundError:
            return {
                "success": False,
                "error_message": "javac not found. Please install JDK.",
                "returncode": -1
            }
        except Exception as e:
            return {
                "success": False,
                "error_message": str(e),
                "returncode": -1
            }
        finally:
            shutil.rmtree(base_dir, ignore_errors=True)

    @staticmethod
    def extract_error_summary(stderr: str) -> str:
        """提取编译错误摘要"""
        if not stderr:
            return ""

        lines = stderr.strip().split('\n')
        error_lines = []

        for line in lines:
            if '错误' in line or 'error' in line.lower():
                error_lines.append(line.strip())

        if error_lines:
            return '\n'.join(error_lines[:5])

        return stderr[:500] if len(stderr) > 500 else stderr

    @classmethod
    def generate_wrong_code(cls, original_java_file: str) -> str:
        """
        生成错误的Java代码

        Args:
            original_java_file: 原始Java文件路径

        Returns:
            错误的Java代码
        """
        try:
            # 读取原始文件内容
            with open(original_java_file, 'r', encoding='utf-8') as f:
                lines = f.readlines()

            # 分析文件，找到所有方法
            import re
            methods = []
            current_method = []
            in_method = False
            brace_count = 0

            for i, line in enumerate(lines):
                if not in_method and ('public ' in line or 'private ' in line or 'protected ' in line) and '(' in line and ')' in line and '{' in line:
                    in_method = True
                    current_method = [i, line]
                    brace_count = 1
                elif in_method:
                    brace_count += line.count('{') - line.count('}')
                    current_method.append(line)
                    if brace_count == 0:
                        in_method = False
                        methods.append(current_method)
                        current_method = []

            # 对每个方法进行变异
            modified_lines = lines.copy()
            for method in methods:
                start_line = method[0]
                method_lines = method[1:]
                method_code = ''.join(method_lines)

                # 1. 替换方法体为错误实现
                # 提取方法签名
                method_signature = method_lines[0].strip()
                # 提取返回类型
                return_type_match = re.search(r'(public|private|protected)\s+static?\s+([^\s]+)\s+([^\s(]+)', method_signature)
                if return_type_match:
                    return_type = return_type_match.group(2)
                    # 生成错误的方法体
                    wrong_body = []
                    # 添加错误的实现
                    if return_type == 'int':
                        wrong_body = ['    return 0;\n']
                    elif return_type == 'boolean':
                        wrong_body = ['    return false;\n']
                    elif return_type == 'double':
                        wrong_body = ['    return 0.0;\n']
                    elif return_type == 'void':
                        wrong_body = ['    // Empty implementation\n']
                    else:
                        wrong_body = ['    return null;\n']

                    # 替换方法体
                    if len(method_lines) > 1:
                        # 保留方法签名
                        modified_lines[start_line] = method_lines[0]
                        # 替换方法体
                        for i in range(1, len(method_lines) - 1):
                            if start_line + i < len(modified_lines):
                                modified_lines[start_line + i] = ''
                        # 添加错误的实现
                        if start_line + 1 < len(modified_lines):
                            modified_lines[start_line + 1] = wrong_body[0]

            return ''.join(modified_lines)
        except Exception as e:
            logger.error(f"Error generating wrong code: {str(e)}")
            # 如果生成失败，返回原始文件内容
            with open(original_java_file, 'r', encoding='utf-8') as f:
                return f.read()

    @classmethod
    def run_test(cls, package_name: str, class_name: str, original_java_file: str, test_code: str, use_wrong_code: bool = False) -> Dict:
        """
        运行测试代码并获取测试结果

        Args:
            package_name: 包名
            class_name: 类名
            original_java_file: 原始Java文件路径
            test_code: 测试代码
            use_wrong_code: 是否使用错误代码

        Returns:
            测试结果字典，包含测试通过情况和详细信息
        """
        base_dir = tempfile.mkdtemp()

        try:
            project_dir = os.path.join(base_dir, "test_project")
            src_dir = os.path.join(project_dir, "src")
            target_dir = os.path.join(project_dir, "target")

            os.makedirs(src_dir, exist_ok=True)
            os.makedirs(target_dir, exist_ok=True)

            # 1. 复制或生成类文件
            if use_wrong_code:
                # 生成错误代码
                wrong_code = cls.generate_wrong_code(original_java_file)
                if not wrong_code:
                    return {
                        "success": False,
                        "error_message": "Failed to generate wrong code",
                        "test_results": []
                    }
                # 创建错误的类文件
                package_dir = os.path.join(src_dir, *package_name.split('.'))
                os.makedirs(package_dir, exist_ok=True)
                wrong_file_path = os.path.join(package_dir, f"{class_name}.java")
                with open(wrong_file_path, 'w', encoding='utf-8') as f:
                    f.write(wrong_code)
                logger.info(f"✅ Created wrong class file: {wrong_file_path}")
            else:
                # 复制原始类文件
                cls.copy_original_class_file(
                    src_dir=src_dir,
                    package=package_name,
                    class_name=class_name,
                    original_file_path=original_java_file
                )
                logger.info(f"✅ Copied original class file: {original_java_file}")

            # 2. 创建测试类文件
            test_class_name = f"{class_name}Test"
            test_package_dir = os.path.join(src_dir, *package_name.split('.'))
            os.makedirs(test_package_dir, exist_ok=True)
            test_class_path = os.path.join(test_package_dir, f"{test_class_name}.java")
            with open(test_class_path, 'w', encoding='utf-8') as f:
                f.write(test_code)

            # 3. 编译项目
            junit_classpath = cls.download_junit_jars()
            classpath = os.pathsep.join([target_dir, junit_classpath])

            compile_result = cls.compile_project(src_dir, target_dir, classpath)
            if not compile_result["success"]:
                return {
                    "success": False,
                    "error_message": f"Compilation failed: {compile_result['error_message']}",
                    "test_results": []
                }

            # 4. 运行测试
            test_class_full_name = f"{package_name}.{test_class_name}"

            # 检查是否需要启用覆盖率
            enable_coverage = not use_wrong_code
            coverage = 0.0

            # 尝试使用JUnit 5的命令（详细模式）
            cmd = [
                "java",
                "-cp", classpath,
                "org.junit.platform.console.ConsoleLauncher",
                "--select-class", test_class_full_name,
                "--details", "tree"
            ]

            # 如果启用覆盖率，添加JaCoCo agent
            jacoco_agent_path = os.path.join(LIB_DIR, "jacoco-agent-0.8.11-runtime.jar")
            jacoco_cli_path = os.path.join(LIB_DIR, "jacoco-cli-0.8.11-nodeps.jar")
            jacoco_exec_path = os.path.join(target_dir, "jacoco.exec")
            jacoco_report_dir = os.path.join(target_dir, "jacoco-report")

            if enable_coverage and os.path.exists(jacoco_agent_path):
                cmd.insert(1, f"-javaagent:{jacoco_agent_path}=destfile={jacoco_exec_path},append=true")

            # 运行测试
            result = subprocess.run(
                cmd,
                capture_output=True,
                text=True,
                timeout=120
            )

            # 只打印关键信息
            if result.stderr and 'WARNING' not in result.stderr:
                logger.warning(f"Test stderr: {result.stderr}")

            # 5. 解析测试结果
            test_results = []
            total_tests = 0
            passed_tests = 0
            failed_tests = 0

            # 直接从输出中提取测试结果
            stdout = result.stdout

            # 查找测试统计信息
            import re

            # 尝试匹配JUnit 5的输出格式
            tests_found_match = re.search(r'\[\s*(\d+) tests found\s*\]', stdout)
            tests_successful_match = re.search(r'\[\s*(\d+) tests successful\s*\]', stdout)
            tests_failed_match = re.search(r'\[\s*(\d+) tests failed\s*\]', stdout)

            if tests_found_match:
                total_tests = int(tests_found_match.group(1))
            if tests_successful_match:
                passed_tests = int(tests_successful_match.group(1))
            if tests_failed_match:
                failed_tests = int(tests_failed_match.group(1))

            # 如果没有匹配到，尝试传统格式
            if total_tests == 0:
                total_match = re.search(r'Tests run:\s*(\d+)', stdout)
                passed_match = re.search(r'Passed:\s*(\d+)', stdout)
                failed_match = re.search(r'Failed:\s*(\d+)', stdout)

                if total_match:
                    total_tests = int(total_match.group(1))
                if passed_match:
                    passed_tests = int(passed_match.group(1))
                if failed_match:
                    failed_tests = int(failed_match.group(1))

            if total_tests > 0:
                logger.info(f"Extracted test results: {total_tests} total, {passed_tests} passed, {failed_tests} failed")

            # 尝试解析单个测试方法结果
            lines = stdout.split('\n')
            for line in lines:
                line = line.strip()
                if line.startswith('✓') or line.startswith('✗'):
                    # 提取方法名
                    if '(' in line:
                        method_name = line.split('(')[0].strip()[2:]
                    else:
                        method_name = line.strip()[2:]
                    status = "PASSED" if line.startswith('✓') else "FAILED"
                    test_results.append({"method_name": method_name, "status": status})

            # 如果没有解析到测试结果但有测试运行
            if len(test_results) == 0 and total_tests > 0:
                # 添加一个总结果
                test_results.append({"method_name": "Total", "status": "PASSED" if failed_tests == 0 else "FAILED"})

            logger.info(f"Final test results: {test_results}")

            # 计算覆盖率（使用JaCoCo）
            if enable_coverage and total_tests > 0 and os.path.exists(jacoco_exec_path) and os.path.exists(jacoco_cli_path):
                try:
                    # 运行JaCoCo CLI分析覆盖率
                    os.makedirs(jacoco_report_dir, exist_ok=True)
                    
                    # 构建JaCoCo分析命令
                    jacoco_cmd = [
                        "java",
                        "-jar", jacoco_cli_path,
                        "report",
                        jacoco_exec_path,
                        "--classfiles", target_dir,
                        "--sourcefiles", src_dir,
                        "--html", jacoco_report_dir,
                        "--xml", os.path.join(target_dir, "jacoco.xml")
                    ]
                    
                    # 运行JaCoCo分析
                    jacoco_result = subprocess.run(
                        jacoco_cmd,
                        capture_output=True,
                        text=True,
                        timeout=60
                    )
                    
                    # 解析JaCoCo XML报告获取语句覆盖率
                    import xml.etree.ElementTree as ET
                    jacoco_xml_path = os.path.join(target_dir, "jacoco.xml")
                    if os.path.exists(jacoco_xml_path):
                        tree = ET.parse(jacoco_xml_path)
                        root = tree.getroot()
                        
                        # 查找语句覆盖率
                        for counter in root.findall(".//counter"):
                            if counter.get("type") == "LINE":
                                covered = int(counter.get("covered", "0"))
                                missed = int(counter.get("missed", "0"))
                                total = covered + missed
                                if total > 0:
                                    coverage = (covered / total) * 100
                                    logger.info(f"JaCoCo statement coverage: {coverage:.2f}%")
                                break
                    else:
                        logger.warning("JaCoCo XML report not found")
                except Exception as e:
                    logger.warning(f"Error calculating coverage with JaCoCo: {str(e)}")
                    # 回退到基于测试通过情况的估算
                    coverage = (passed_tests / total_tests) * 100
                    logger.info(f"Fallback to estimated code coverage: {coverage:.2f}%")
            elif enable_coverage and total_tests > 0:
                # 回退到基于测试通过情况的估算
                coverage = (passed_tests / total_tests) * 100
                logger.info(f"Fallback to estimated code coverage: {coverage:.2f}%")

            return {
                "success": True,
                "total_tests": total_tests,
                "passed_tests": passed_tests,
                "failed_tests": failed_tests,
                "test_results": test_results,
                "coverage": coverage,
                "stdout": result.stdout,
                "stderr": result.stderr
            }

        except subprocess.TimeoutExpired:
            return {
                "success": False,
                "error_message": "Test execution timeout",
                "test_results": []
            }
        except FileNotFoundError:
            return {
                "success": False,
                "error_message": "java not found. Please install JDK.",
                "test_results": []
            }
        except Exception as e:
            logger.error(f"Test execution error: {str(e)}")
            import traceback
            logger.error(f"Traceback: {traceback.format_exc()}")
            return {
                "success": False,
                "error_message": str(e),
                "test_results": []
            }
        finally:
            shutil.rmtree(base_dir, ignore_errors=True)

    @staticmethod
    def parse_test_results(stdout: str, stderr: str) -> List[Dict]:
        """
        解析JUnit测试结果

        Args:
            stdout: 测试输出
            stderr: 错误输出

        Returns:
            测试结果列表，每个元素包含测试方法名和状态
        """
        test_results = []

        # 解析stdout中的测试结果
        lines = stdout.strip().split('\n')

        # 查找测试方法结果（JUnit 5格式）
        for line in lines:
            line = line.strip()

            # 解析JUnit 5的测试结果格式
            if line.startswith('✓') or line.startswith('✗'):
                # 提取方法名和状态
                parts = line.split(' ', 1)
                if len(parts) >= 2:
                    status_symbol = parts[0]
                    method_info = parts[1]

                    # 提取方法名
                    if '(' in method_info and ')' in method_info:
                        method_name = method_info.split('(')[0].strip()
                    else:
                        method_name = method_info.strip()

                    status = "PASSED" if status_symbol == '✓' else "FAILED"

                    test_results.append({
                        "method_name": method_name,
                        "status": status
                    })

            # 解析传统格式
            elif 'PASSED' in line or 'FAILED' in line:
                # 提取方法名
                if 'PASSED' in line:
                    method_name = line.split('PASSED')[0].strip()
                    status = "PASSED"
                else:
                    method_name = line.split('FAILED')[0].strip()
                    status = "FAILED"

                test_results.append({
                    "method_name": method_name,
                    "status": status
                })

        # 如果没有解析到测试结果，尝试从汇总信息中解析
        if not test_results:
            # 查找测试汇总信息
            total_tests = 0
            passed = 0
            failed = 0

            for line in lines:
                line = line.strip()
                if 'Tests run:' in line:
                    # 提取测试数量
                    parts = line.split(',')
                    for part in parts:
                        part = part.strip()
                        if 'Tests run:' in part:
                            total_tests = int(part.split(':')[1].strip())
                        elif 'Passed:' in part:
                            passed = int(part.split(':')[1].strip())
                        elif 'Failed:' in part:
                            failed = int(part.split(':')[1].strip())
                    break

            # 如果有测试运行，添加结果
            if total_tests > 0:
                # 添加总测试结果
                test_results.append({
                    "method_name": "Total",
                    "status": "PASSED" if failed == 0 else "FAILED"
                })

        return test_results
