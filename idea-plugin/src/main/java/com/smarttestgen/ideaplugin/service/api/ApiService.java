package com.smarttestgen.ideaplugin.service.api;

import com.smarttestgen.ideaplugin.util.Constants;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * API调用服务类
 */
public class ApiService {
    /**
     * 初始化会话，存储静态上下文信息
     * @param requestBody 请求体（包含类名、包名、字段、方法、依赖等）
     * @return API响应结果（包含session_id）
     * @throws Exception 异常
     */
    public static String initSession(String requestBody) throws Exception {
        URL url = new URL(Constants.INIT_SESSION_URL);
        
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);
        
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.getBytes("UTF-8");
            os.write(input, 0, input.length);
        }
        
        int responseCode = connection.getResponseCode();
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(responseCode >= 200 && responseCode < 300 ? connection.getInputStream() : connection.getErrorStream(), "UTF-8"))) {
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
        
        connection.disconnect();
        
        if (responseCode >= 300) {
            throw new Exception("API request failed with code " + responseCode + ": " + response.toString());
        }
        
        return response.toString();
    }

    /**
     * 处理文本，调用后端API
     * @param content 文本内容
     * @return API响应结果
     * @throws Exception 异常
     */
    public static String processText(String content) throws Exception {
        // 创建 URL对象
        URL url = new URL(Constants.API_URL);
        
        // 创建 HttpURLConnection对象
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        // 设置请求方法
        connection.setRequestMethod("POST");
        
        // 设置请求头
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);
        
        // 构建请求体
        String requestBody = buildRequestBody(content);
        
        // 发送请求体
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        
        // 读取响应
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
        
        // 关闭连接
        connection.disconnect();
        
        return response.toString();
    }
    
    /**
     * 生成测试代码，调用后端API
     * @param requestBody 请求体
     * @return API响应结果
     * @throws Exception 异常
     */
    public static String generateTestCode(String requestBody) throws Exception {
        // 创建URL对象
        URL url = new URL(Constants.GENERATE_TEST_URL);
        
        // 创建HttpURLConnection对象
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        // 设置请求方法
        connection.setRequestMethod("POST");
        
        // 设置请求头
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);
        
        // 发送请求体
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.getBytes("UTF-8");
            os.write(input, 0, input.length);
        }
        
        // 获取响应码
        int responseCode = connection.getResponseCode();
        
        // 读取响应
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(responseCode >= 200 && responseCode < 300 ? connection.getInputStream() : connection.getErrorStream(), "UTF-8"))) {
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
        
        // 关闭连接
        connection.disconnect();
        
        // 如果响应码不是200，抛出异常
        if (responseCode >= 300) {
            throw new Exception("API request failed with code " + responseCode + ": " + response.toString());
        }
        
        return response.toString();
    }
    
    /**
     * 修复编译错误，调用后端API（接受完整请求体）
     * @param requestBody 请求体
     * @return API响应结果
     * @throws Exception 异常
     */
    public static String fixCompilationError(String requestBody) throws Exception {
        // 创建URL对象
        URL url = new URL(Constants.FIX_COMPILATION_ERROR_URL);
        
        // 创建HttpURLConnection对象
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        // 设置请求方法
        connection.setRequestMethod("POST");
        
        // 设置请求头
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);
        
        // 发送请求体
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.getBytes("UTF-8");
            os.write(input, 0, input.length);
        }
        
        // 获取响应码
        int responseCode = connection.getResponseCode();
        
        // 读取响应
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(responseCode >= 200 && responseCode < 300 ? connection.getInputStream() : connection.getErrorStream(), "UTF-8"))) {
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
        
        // 关闭连接
        connection.disconnect();
        
        // 如果响应码不是200，抛出异常
        if (responseCode >= 300) {
            throw new Exception("API request failed with code " + responseCode + ": " + response.toString());
        }
        
        return response.toString();
    }
    
    /**
     * 预编译测试代码，调用后端API
     * @param requestBody 请求体（包含package_name, class_name, empty_method, test_code）
     * @return API响应结果
     * @throws Exception 异常
     */
    public static String preCompile(String requestBody) throws Exception {
        URL url = new URL(Constants.PRE_COMPILE_URL);
        
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);
        
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.getBytes("UTF-8");
            os.write(input, 0, input.length);
        }
        
        int responseCode = connection.getResponseCode();
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(responseCode >= 200 && responseCode < 300 ? connection.getInputStream() : connection.getErrorStream(), "UTF-8"))) {
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
        
        connection.disconnect();
        
        if (responseCode >= 300) {
            throw new Exception("API request failed with code " + responseCode + ": " + response.toString());
        }
        
        return response.toString();
    }
    
    /**
     * 构建请求体
     * @param content 文本内容
     * @return 请求体字符串
     */
    private static String buildRequestBody(String content) {
        // 转义特殊字符
        String escapedContent = escapeContent(content);
        
        // 构建JSON请求体
        return "{\"" + Constants.REQUEST_BODY_FIELD + "\": \"" + escapedContent + "\"}";
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
        
        // 转义其他控制字符
        StringBuilder sb = new StringBuilder();
        for (char c : content.toCharArray()) {
            if (c < 32) {
                // 对于控制字符，使用Unicode转义序列
                sb.append(String.format("\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        
        return sb.toString();
    }
}
