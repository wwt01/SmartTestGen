package com.smarttestgen.ideaplugin.service;

/**
 * 路径处理服务类
 * 负责处理和转换文件路径
 */
public class PathService {
    
    /**
     * 从文件路径获取package名称
     * @param filePath 文件路径
     * @return package名称
     */
    public static String getPackageNameFromPath(String filePath) {
        // 移除文件扩展名
        String pathWithoutExt = filePath.replaceFirst("\\.java$", "");
        
        // 移除src/test/java前缀
        pathWithoutExt = removeTestPathPrefix(pathWithoutExt);
        
        // 移除文件名
        int lastSeparatorIndex = getLastSeparatorIndex(pathWithoutExt);
        if (lastSeparatorIndex != -1) {
            pathWithoutExt = pathWithoutExt.substring(0, lastSeparatorIndex);
        }
        
        // 将路径分隔符替换为点
        String packageName = pathWithoutExt.replace('\\', '.').replace('/', '.');
        
        System.out.println("[Test Case Generator] Derived package name: " + packageName);
        return packageName;
    }
    
    /**
     * 移除测试路径前缀
     * @param path 路径
     * @return 移除前缀后的路径
     */
    private static String removeTestPathPrefix(String path) {
        return path.replace("src\\test\\java\\", "")
            .replace("src/test/java/", "")
            .replace("src\\test\\java", "")
            .replace("src/test/java", "");
    }
    
    /**
     * 获取最后一个路径分隔符的索引
     * @param path 路径
     * @return 最后一个分隔符的索引
     */
    private static int getLastSeparatorIndex(String path) {
        int lastBackslash = path.lastIndexOf('\\');
        int lastSlash = path.lastIndexOf('/');
        return Math.max(lastBackslash, lastSlash);
    }
    
    /**
     * 更新测试代码顶部的package信息
     * @param code 测试代码
     * @param packageName package名称
     * @return 更新后的代码
     */
    public static String updatePackageInfo(String code, String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return code;
        }
        
        // 查找现有的package声明
        String packageDeclaration = "package " + packageName + ";";
        
        // 如果代码中已经有package声明，替换它
        if (code.startsWith("package ")) {
            int endOfPackageLine = code.indexOf(';');
            if (endOfPackageLine != -1) {
                return packageDeclaration + code.substring(endOfPackageLine + 1);
            }
        }
        // 如果代码中没有package声明，在开头添加
        else {
            return packageDeclaration + "\n\n" + code;
        }
        
        return code;
    }
    
    /**
     * 将源文件路径转换为测试文件路径
     * @param sourcePath 源文件路径
     * @return 测试文件路径
     */
    public static String convertToTestPath(String sourcePath) {
        String testPath = sourcePath;
        
        // 处理Windows格式的路径
        if (testPath.contains("src\\main\\java")) {
            testPath = testPath.replace("src\\main\\java", "src\\test\\java");
        }
        // 处理Unix格式的路径
        else if (testPath.contains("src/main/java")) {
            testPath = testPath.replace("src/main/java", "src/test/java");
        }
        
        // 确保路径分隔符一致性
        testPath = testPath.replace("\\", "/");
        
        return testPath;
    }
    
    /**
     * 为文件名添加Test后缀
     * @param filePath 文件路径
     * @return 添加Test后缀的文件路径
     */
    public static String addTestSuffix(String filePath) {
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex != -1) {
            return filePath.substring(0, lastDotIndex) + "Test" + filePath.substring(lastDotIndex);
        } else {
            // 如果没有扩展名，添加.java扩展名
            return filePath + "Test.java";
        }
    }
}
