"""
编译工具类
用于编译Java代码并获取编译错误信息
"""

import os
import subprocess
import tempfile
import shutil
from typing import Dict, List


JUNIT_JUPITER_API_URL = "https://repo1.maven.org/maven2/org/junit/jupiter/junit-jupiter-api/5.10.0/junit-jupiter-api-5.10.0.jar"
JUNIT_PLATFORM_COMMON_URL = "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-commons/1.10.0/junit-platform-commons-1.10.0.jar"
OPENTEST4J_URL = "https://repo1.maven.org/maven2/org/opentest4j/opentest4j/1.3.0/opentest4j-1.3.0.jar"
APIGUARDIAN_URL = "https://repo1.maven.org/maven2/org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar"

LIB_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "lib")


class CompilationUtil:
    """Java编译工具"""

    @staticmethod
    def check_javac_available() -> bool:
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

    @staticmethod
    def download_junit_jars() -> List[str]:
        """下载JUnit jar文件"""
        os.makedirs(LIB_DIR, exist_ok=True)

        jar_urls = [
            (JUNIT_JUPITER_API_URL, "junit-jupiter-api-5.10.0.jar"),
            (JUNIT_PLATFORM_COMMON_URL, "junit-platform-commons-1.10.0.jar"),
            (OPENTEST4J_URL, "opentest4j-1.3.0.jar"),
            (APIGUARDIAN_URL, "apiguardian-api-1.1.2.jar"),
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
                    return []

        return jar_paths

    @staticmethod
    def get_junit_classpath() -> str:
        """获取JUnit classpath"""
        jar_paths = CompilationUtil.download_junit_jars()
        if jar_paths:
            return os.pathsep.join(jar_paths)
        return ""

    @staticmethod
    def create_temp_project() -> Dict[str, str]:
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

    @staticmethod
    def create_class_file(src_dir: str, package: str, class_name: str, code: str) -> str:
        """创建Java类文件"""
        package_dir = os.path.join(src_dir, *package.split('.'))
        os.makedirs(package_dir, exist_ok=True)

        file_path = os.path.join(package_dir, f"{class_name}.java")
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(code)

        return file_path

    @staticmethod
    def compile_project(src_dir: str, target_dir: str, classpath: str = "") -> Dict:
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
                    "error": "No Java files found",
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
                "error": result.stderr if result.returncode != 0 else "",
                "returncode": result.returncode
            }

        except subprocess.TimeoutExpired:
            return {
                "success": False,
                "error": "Compilation timeout",
                "returncode": -1
            }
        except FileNotFoundError:
            return {
                "success": False,
                "error": "javac not found",
                "returncode": -1
            }
        except Exception as e:
            return {
                "success": False,
                "error": str(e),
                "returncode": -1
            }

    @staticmethod
    def cleanup_project(base_dir: str):
        """清理临时项目"""
        try:
            shutil.rmtree(base_dir, ignore_errors=True)
        except Exception:
            pass

    @staticmethod
    def compile_test_code(
        package_name: str,
        class_name: str,
        empty_method: str,
        test_code: str
    ) -> Dict:
        """
        编译测试代码

        Args:
            package_name: 包名
            class_name: 类名
            empty_method: 空方法代码
            test_code: 测试代码

        Returns:
            编译结果字典
        """
        project = CompilationUtil.create_temp_project()

        try:
            main_class_code = f"""package {package_name};

public class {class_name} {{
    
    {empty_method}
}}
"""

            CompilationUtil.create_class_file(
                project["src_dir"],
                package_name,
                class_name,
                main_class_code
            )

            test_class_name = f"{class_name}Test"
            CompilationUtil.create_class_file(
                project["src_dir"],
                package_name,
                test_class_name,
                test_code
            )

            junit_classpath = CompilationUtil.get_junit_classpath()

            result = CompilationUtil.compile_project(
                project["src_dir"],
                project["target_dir"],
                classpath=junit_classpath
            )

            return result

        finally:
            CompilationUtil.cleanup_project(project["base_dir"])

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
