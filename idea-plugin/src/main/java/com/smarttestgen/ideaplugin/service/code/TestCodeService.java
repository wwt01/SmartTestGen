package com.smarttestgen.ideaplugin.service.code;

import com.smarttestgen.ideaplugin.model.ClassContextInfo;
import com.smarttestgen.ideaplugin.model.FieldInfo;
import com.smarttestgen.ideaplugin.model.MethodInfo;
import com.smarttestgen.ideaplugin.model.ParameterInfo;
import com.smarttestgen.ideaplugin.service.api.ApiService;
import com.smarttestgen.ideaplugin.util.JsonUtils;
import com.smarttestgen.ideaplugin.util.LogUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class TestCodeService {
    
    private static String currentSessionId = null;
    
    public static String getCurrentSessionId() {
        return currentSessionId;
    }
    
    public static void setCurrentSessionId(String sessionId) {
        currentSessionId = sessionId;
    }
    
    public static class InitSessionResult {
        private final String sessionId;
        private final boolean success;
        private final String errorMessage;
        
        public InitSessionResult(String sessionId) {
            this.sessionId = sessionId;
            this.success = true;
            this.errorMessage = null;
        }
        
        public InitSessionResult(String errorMessage, boolean failed) {
            this.sessionId = null;
            this.success = false;
            this.errorMessage = errorMessage;
        }
        
        public String getSessionId() { return sessionId; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    public static void initSession(
            ClassContextInfo classContextInfo,
            Consumer<InitSessionResult> onSuccess,
            Consumer<String> onError) {
        
        long startTime = System.currentTimeMillis();
        LogUtil.section("初始化会话");
        LogUtil.info("Session", "类名: " + classContextInfo.getClassName() + 
                     ", 类型: " + classContextInfo.getClassType());
        
        CompletableFuture.runAsync(() -> {
            try {
                String requestBody = buildInitSessionRequest(classContextInfo);
                LogUtil.request("InitSession", requestBody);
                
                String response = ApiService.initSession(requestBody);
                LogUtil.response("InitSession", response);
                
                String sessionId = extractSessionId(response);
                
                if (sessionId != null && !sessionId.isEmpty()) {
                    currentSessionId = sessionId;
                    long elapsed = System.currentTimeMillis() - startTime;
                    LogUtil.success("Session", "会话ID: " + sessionId + " [" + elapsed + "ms]");
                    ApplicationManager.getApplication().invokeLater(() -> 
                        onSuccess.accept(new InitSessionResult(sessionId)), ModalityState.nonModal());
                } else {
                    String errorMsg = extractErrorMessage(response);
                    long elapsed = System.currentTimeMillis() - startTime;
                    LogUtil.error("Session", "初始化失败 [" + elapsed + "ms]: " + errorMsg);
                    ApplicationManager.getApplication().invokeLater(() -> 
                        onError.accept(errorMsg != null ? errorMsg : "Failed to get session_id"), ModalityState.nonModal());
                }
                
            } catch (Exception e) {
                long elapsed = System.currentTimeMillis() - startTime;
                LogUtil.error("Session", "异常 [" + elapsed + "ms]: " + e.getMessage());
                ApplicationManager.getApplication().invokeLater(() -> 
                    onError.accept(e.getMessage()), ModalityState.nonModal());
            }
        });
    }
    
    public static String buildInitSessionRequest(ClassContextInfo classInfo) {
        String className = classInfo.getClassName() != null ? classInfo.getClassName() : "";
        String packageName = classInfo.getPackageName() != null ? classInfo.getPackageName() : "";
        String classType = classInfo.getClassType() != null ? classInfo.getClassType() : "Unknown";
        boolean isInterface = classInfo.isInterface();
        
        String fieldsJson = buildFieldsJson(classInfo.getFields());
        String methodsJson = buildMethodsJson(classInfo.getMethods());
        String dependenciesJson = buildDependenciesJson(classInfo.getDependencies());
        
        return "{" +
                "\"class_name\":\"" + escapeContent(className) + "\"," +
                "\"is_interface\":" + isInterface + "," +
                "\"package_name\":\"" + escapeContent(packageName) + "\"," +
                "\"class_type\":\"" + escapeContent(classType) + "\"," +
                "\"fields\":" + fieldsJson + "," +
                "\"methods\":" + methodsJson + "," +
                "\"dependencies\":" + dependenciesJson +
                "}";
    }
    
    private static String extractSessionId(String response) {
        try {
            String pattern = "\"session_id\":\"";
            int startIndex = response.indexOf(pattern);
            if (startIndex == -1) return null;
            
            startIndex += pattern.length();
            int endIndex = response.indexOf("\"", startIndex);
            if (endIndex == -1) return null;
            
            return response.substring(startIndex, endIndex);
        } catch (Exception e) {
            return null;
        }
    }
    
    private static String extractErrorMessage(String response) {
        try {
            String pattern = "\"msg\":\"";
            int startIndex = response.indexOf(pattern);
            if (startIndex == -1) return null;
            
            startIndex += pattern.length();
            int endIndex = response.indexOf("\"", startIndex);
            if (endIndex == -1) return null;
            
            return response.substring(startIndex, endIndex);
        } catch (Exception e) {
            return null;
        }
    }
    
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
        
        public String getTestCode() { return testCode; }
        public String getEmptyMethodCode() { return emptyMethodCode; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    public static void generateTestCodeWithSession(
            String sessionId,
            String methodName,
            String returnType,
            String parametersStr,
            String expectationsStr,
            Consumer<TestCodeResult> onSuccess,
            Consumer<String> onError) {
        
        long startTime = System.currentTimeMillis();
        LogUtil.section("生成测试代码");
        LogUtil.info("Generate", "方法: " + methodName + ", 返回类型: " + returnType);
        
        CompletableFuture.runAsync(() -> {
            try {
                String requestBody = buildTestRequestWithSession(sessionId, methodName, returnType, parametersStr, expectationsStr);
                LogUtil.request("GenerateTest", requestBody);
                
                String response = ApiService.generateTestCode(requestBody);
                LogUtil.response("GenerateTest", response);
                
                String generatedCode = JsonUtils.extractDataField(response, "test_code");
                String emptyMethodCode = JsonUtils.extractDataField(response, "empty_method");
                
                long elapsed = System.currentTimeMillis() - startTime;
                LogUtil.success("Generate", "测试代码长度: " + (generatedCode != null ? generatedCode.length() : 0) + 
                               ", 空方法长度: " + (emptyMethodCode != null ? emptyMethodCode.length() : 0) + 
                               " [" + elapsed + "ms]");
                
                String unescapedCode = unescapeCode(generatedCode);
                String unescapedEmptyMethodCode = unescapeCode(emptyMethodCode);
                
                TestCodeResult result = new TestCodeResult(unescapedCode, unescapedEmptyMethodCode);
                ApplicationManager.getApplication().invokeLater(() -> 
                    onSuccess.accept(result), ModalityState.nonModal());
                
            } catch (Exception e) {
                long elapsed = System.currentTimeMillis() - startTime;
                LogUtil.error("Generate", "异常 [" + elapsed + "ms]: " + e.getMessage());
                ApplicationManager.getApplication().invokeLater(() -> 
                    onError.accept(e.getMessage()), ModalityState.nonModal());
            }
        });
    }
    
    private static String buildTestRequestWithSession(
            String sessionId,
            String methodName, 
            String returnType, 
            String parametersStr, 
            String expectationsStr) {
        
        String parametersArray = parametersStr;
        if (parametersArray == null || parametersArray.isEmpty()) {
            parametersArray = "[]";
        }
        
        String expectationsArray = "[]";
        if (expectationsStr != null && !expectationsStr.isEmpty()) {
            if (!expectationsStr.startsWith("[")) {
                expectationsArray = "[" + expectationsStr + "]";
            } else {
                expectationsArray = expectationsStr;
            }
        }
        
        String expectationsJson = expectationsArray;
        if (expectationsJson.startsWith("\"")) {
            expectationsJson = expectationsJson.substring(1, expectationsJson.length() - 1);
        }
        
        return "{" +
                "\"session_id\":\"" + escapeContent(sessionId) + "\"," +
                "\"method_name\":\"" + escapeContent(methodName) + "\"," +
                "\"parameters\":" + parametersArray + "," +
                "\"return_type\":\"" + escapeContent(returnType) + "\"," +
                "\"expectations\":" + expectationsJson +
                "}";
    }
    
    private static String buildFieldsJson(List<FieldInfo> fields) {
        if (fields == null || fields.isEmpty()) return "[]";
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < fields.size(); i++) {
            FieldInfo field = fields.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"name\":\"").append(escapeContent(field.getName())).append("\",");
            sb.append("\"type\":\"").append(escapeContent(field.getType())).append("\"}");
        }
        sb.append("]");
        return sb.toString();
    }
    
    private static String buildMethodsJson(List<MethodInfo> methods) {
        if (methods == null || methods.isEmpty()) return "[]";
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < methods.size(); i++) {
            MethodInfo method = methods.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"name\":\"").append(escapeContent(method.getName())).append("\",");
            sb.append("\"return_type\":\"").append(escapeContent(method.getReturnType())).append("\",");
            sb.append("\"parameters\":").append(buildParametersJson(method.getParameters())).append("}");
        }
        sb.append("]");
        return sb.toString();
    }
    
    private static String buildParametersJson(List<ParameterInfo> parameters) {
        if (parameters == null || parameters.isEmpty()) return "[]";
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < parameters.size(); i++) {
            ParameterInfo param = parameters.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"name\":\"").append(escapeContent(param.getName())).append("\",");
            sb.append("\"type\":\"").append(escapeContent(param.getType())).append("\"}");
        }
        sb.append("]");
        return sb.toString();
    }
    
    private static String buildDependenciesJson(List<String> dependencies) {
        if (dependencies == null || dependencies.isEmpty()) return "[]";
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < dependencies.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeContent(dependencies.get(i))).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }
    
    private static String unescapeCode(String code) {
        if (code == null) return null;
        return code
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\f", "\f")
            .replace("\\b", "\b")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\");
    }
    
    private static String escapeContent(String content) {
        if (content == null) return "";
        return content
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .replace("\f", "\\f")
            .replace("\b", "\\b");
    }
    
    public static void fixCompilationErrorWithSession(
            String requestBody,
            Consumer<TestCodeResult> onSuccess,
            Consumer<String> onError) {
        
        long startTime = System.currentTimeMillis();
        LogUtil.section("修复编译错误");
        
        CompletableFuture.runAsync(() -> {
            try {
                LogUtil.request("FixError", requestBody);
                
                String response = ApiService.fixCompilationError(requestBody);
                LogUtil.response("FixError", response);
                
                String fixedCode = JsonUtils.extractDataField(response, "test_code");
                
                long elapsed = System.currentTimeMillis() - startTime;
                LogUtil.success("FixError", "修复后代码长度: " + (fixedCode != null ? fixedCode.length() : 0) + 
                               " [" + elapsed + "ms]");
                
                String unescapedCode = unescapeCode(fixedCode);
                TestCodeResult result = new TestCodeResult(unescapedCode, null);
                ApplicationManager.getApplication().invokeLater(() -> 
                    onSuccess.accept(result), ModalityState.nonModal());
                
            } catch (Exception e) {
                long elapsed = System.currentTimeMillis() - startTime;
                LogUtil.error("FixError", "异常 [" + elapsed + "ms]: " + e.getMessage());
                ApplicationManager.getApplication().invokeLater(() -> 
                    onError.accept(e.getMessage()), ModalityState.nonModal());
            }
        });
    }
}
