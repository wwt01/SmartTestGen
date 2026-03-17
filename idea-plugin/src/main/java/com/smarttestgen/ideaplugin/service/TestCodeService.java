package com.smarttestgen.ideaplugin.service;

import com.smarttestgen.ideaplugin.util.JsonUtils;

import javax.swing.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 测试代码生成服务类
 * 负责处理测试代码的生成请求和响应处理
 */
public class TestCodeService {
    
    /**
     * 测试代码生成结果
     */
    public static class TestCodeResult {
        private final String testCode;
        private final String emptyMethodCode;
        private final boolean success;
        private final String errorMessage;
        
        public TestCodeResult(String testCode, String emptyMethodCode) {
            this.testCode = testCode;
            this.emptyMethodCode = emptyMethodCode;
            this.success = true;
            this.errorMessage = null;
        }
        
        public TestCodeResult(String errorMessage) {
            this.testCode = null;
            this.emptyMethodCode = null;
            this.success = false;
            this.errorMessage = errorMessage;
        }
        
        public String getTestCode() {
            return testCode;
        }
        
        public String getEmptyMethodCode() {
            return emptyMethodCode;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
    
    /**
     * 生成测试代码
     * @param methodName 方法名
     * @param returnType 返回类型
     * @param parametersStr 参数字符串（JSON格式）
     * @param expectationsStr 期望字符串
     * @param codeStructure 代码结构
     * @param currentClassName 当前类名
     * @param isInterfaceFile 是否为接口文件
     * @param onSuccess 成功回调
     * @param onError 错误回调
     */
    public static void generateTestCode(
            String methodName,
            String returnType,
            String parametersStr,
            String expectationsStr,
            String codeStructure,
            String currentClassName,
            boolean isInterfaceFile,
            Consumer<TestCodeResult> onSuccess,
            Consumer<String> onError) {
        
        CompletableFuture.runAsync(() -> {
            try {
                // 构建请求体
                String requestBody = buildTestRequest(
                    methodName, 
                    returnType, 
                    parametersStr, 
                    expectationsStr, 
                    codeStructure,
                    currentClassName,
                    isInterfaceFile
                );
                
                // 打印请求体
        System.out.println("[Test Case Generator] Request Body Length: " + requestBody.length());
        System.out.println("[Test Case Generator] Request Body: " + requestBody);
                
                // 调用后端接口
                String response = ApiService.generateTestCode(requestBody);
                
                // 打印响应
                System.out.println("[Test Case Generator] Response: " + 
                    (response.length() > 500 ? response.substring(0, 500) + "..." : response));
                
                // 提取生成的测试代码和空方法代码
                String generatedCode = JsonUtils.extractDataField(response, "test_code");
                String emptyMethodCode = JsonUtils.extractDataField(response, "empty_method");
                
                // 打印提取结果
                System.out.println("[Test Case Generator] Extracted test_code length: " + 
                    (generatedCode != null ? generatedCode.length() : 0));
                System.out.println("[Test Case Generator] Extracted empty_method length: " + 
                    (emptyMethodCode != null ? emptyMethodCode.length() : 0));
                
                // 反转义代码中的特殊字符
                String unescapedCode = unescapeCode(generatedCode);
                String unescapedEmptyMethodCode = unescapeCode(emptyMethodCode);
                
                // 创建结果对象
                TestCodeResult result = new TestCodeResult(unescapedCode, unescapedEmptyMethodCode);
                
                // 在 EDT 线程中回调
                SwingUtilities.invokeLater(() -> onSuccess.accept(result));
                
            } catch (Exception e) {
                System.out.println("[Test Case Generator] Error generating test code: " + e.getMessage());
                e.printStackTrace();
                
                // 在 EDT 线程中回调错误
                SwingUtilities.invokeLater(() -> onError.accept(e.getMessage()));
            }
        });
    }
    
    /**
     * 构建测试请求
     * @param methodName 方法名
     * @param returnType 返回类型
     * @param parametersStr 参数字符串
     * @param expectationsStr 期望字符串
     * @param codeStructure 代码结构
     * @param currentClassName 当前类名
     * @param isInterfaceFile 是否为接口文件
     * @return 请求体
     */
    private static String buildTestRequest(
            String methodName, 
            String returnType, 
            String parametersStr, 
            String expectationsStr, 
            String codeStructure,
            String currentClassName,
            boolean isInterfaceFile) {
        
        // 使用从前端获取的实际参数数组
        String parametersArray = parametersStr;
        if (parametersArray == null || parametersArray.isEmpty()) {
            parametersArray = "[]";
        }
        
        // 构建期望数组
        String expectationsArray = "[]";
        if (expectationsStr != null && !expectationsStr.isEmpty()) {
            // 确保期望数组格式正确
            if (!expectationsStr.startsWith("[")) {
                expectationsArray = "[" + expectationsStr + "]";
            } else {
                expectationsArray = expectationsStr;
            }
        }
        
        // 获取当前编辑文件的内容
        String fileContent = FileContentService.getCurrentFileContent();
        
        // 构建请求体
        String className = currentClassName != null && !currentClassName.isEmpty() ? currentClassName : "TestClass";
        System.out.println("[Test Case Generator] Using class name: " + className);
        System.out.println("[Test Case Generator] Is interface: " + isInterfaceFile);
        
        // 确保expectationsArray是一个有效的JSON数组
        String expectationsJson = expectationsArray;
        if (expectationsJson.startsWith("\"")) {
            // 如果是字符串形式的数组，去除引号
            expectationsJson = expectationsJson.substring(1, expectationsJson.length() - 1);
        }
        
        String requestBody = "{" +
                "\"method_name\":\"" + escapeContent(methodName) + "\"," +
                "\"parameters\":" + parametersArray + "," +
                "\"return_type\":\"" + escapeContent(returnType) + "\"," +
                "\"expectations\":" + expectationsJson + "," +
                "\"class_name\":\"" + escapeContent(className) + "\"," +
                "\"is_interface\":" + isInterfaceFile + "," +
                "\"code_structure\":\"" + escapeContent(codeStructure) + "\"," +
                "\"file_content\":\"" + escapeContent(fileContent) + "\"" +
                "}";
        
        // 打印请求体的长度和前1000个字符，以便排查问题
        System.out.println("[Test Case Generator] Request Body Length: " + requestBody.length());
        System.out.println("[Test Case Generator] Request Body (first 1000 chars): " + 
            (requestBody.length() > 1000 ? requestBody.substring(0, 1000) + "..." : requestBody));
        
        return requestBody;
    }
    
    /**
     * 反转义代码中的特殊字符
     * @param code 原始代码
     * @return 反转义后的代码
     */
    private static String unescapeCode(String code) {
        if (code == null) {
            return null;
        }
        
        return code
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\f", "\f")
            .replace("\\b", "\b")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\");
    }
    
    /**
     * 转义内容中的特殊字符
     * @param content 原始内容
     * @return 转义后的内容
     */
    private static String escapeContent(String content) {
        if (content == null) return "";
        
        // 转义反斜杠
        content = content.replace("\\", "\\\\");
        
        // 转义双引号
        content = content.replace("\"", "\\\"");
        
        // 转义换行符
        content = content.replace("\n", "\\n");
        
        // 转义回车符
        content = content.replace("\r", "\\r");
        
        // 转义制表符
        content = content.replace("\t", "\\t");
        
        // 转义其他可能导致JSON解析错误的字符
        content = content.replace("\f", "\\f");
        content = content.replace("\b", "\\b");
        
        return content;
    }
    
    /**
     * 修复编译错误
     * @param requestBody 请求体
     * @param onSuccess 成功回调
     * @param onError 错误回调
     */
    public static void fixCompilationError(
            String requestBody,
            Consumer<TestCodeResult> onSuccess,
            Consumer<String> onError) {
        
        CompletableFuture.runAsync(() -> {
            try {
                // 打印请求体
                System.out.println("[Test Case Generator] Fix compilation error request body length: " + requestBody.length());
                System.out.println("[Test Case Generator] Fix compilation error request body: " + requestBody);
                
                // 调用后端接口
                String response = ApiService.fixCompilationError(requestBody);
                
                // 打印响应
                System.out.println("[Test Case Generator] Fix compilation error response: " + 
                    (response.length() > 500 ? response.substring(0, 500) + "..." : response));
                
                // 提取修复后的测试代码
                String fixedCode = JsonUtils.extractDataField(response, "test_code");
                
                // 打印提取结果
                System.out.println("[Test Case Generator] Extracted fixed test_code length: " + 
                    (fixedCode != null ? fixedCode.length() : 0));
                
                // 反转义代码中的特殊字符
                String unescapedCode = unescapeCode(fixedCode);
                
                // 创建结果对象
                TestCodeResult result = new TestCodeResult(unescapedCode, null);
                
                // 在 EDT 线程中回调
                SwingUtilities.invokeLater(() -> onSuccess.accept(result));
                
            } catch (Exception e) {
                System.out.println("[Test Case Generator] Error fixing compilation error: " + e.getMessage());
                e.printStackTrace();
                
                // 在 EDT 线程中回调错误
                SwingUtilities.invokeLater(() -> onError.accept(e.getMessage()));
            }
        });
    }
}
