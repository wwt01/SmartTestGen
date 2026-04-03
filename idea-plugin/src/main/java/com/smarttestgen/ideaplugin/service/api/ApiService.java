package com.smarttestgen.ideaplugin.service.api;

import com.smarttestgen.ideaplugin.util.Constants;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
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
        HttpURLConnection connection = getHttpURLConnection(Constants.INIT_SESSION_URL, requestBody);
        return executeRequest(connection);
    }

    private static @NotNull HttpURLConnection getHttpURLConnection(String urlString, String requestBody) throws IOException {
        URL url = new URL(urlString);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        return connection;
    }

    /**
     * 处理文本，调用后端API
     * @param content 文本内容
     * @return API响应结果
     * @throws Exception 异常
     */
    public static String processText(String content) throws Exception {
        // 构建请求体
        String requestBody = buildRequestBody(content);
        HttpURLConnection connection = getHttpURLConnection(Constants.API_URL, requestBody);
        return executeRequest(connection);
    }
    
    /**
     * 生成测试代码，调用后端API
     * @param requestBody 请求体
     * @return API响应结果
     * @throws Exception 异常
     */
    public static String generateTestCode(String requestBody) throws Exception {
        HttpURLConnection connection = getHttpURLConnection(Constants.GENERATE_TEST_URL, requestBody);
        return executeRequest(connection);
    }
    
    /**
     * 修复编译错误，调用后端API（接受完整请求体）
     * @param requestBody 请求体
     * @return API响应结果
     * @throws Exception 异常
     */
    public static String fixCompilationError(String requestBody) throws Exception {
        HttpURLConnection connection = getHttpURLConnection(Constants.FIX_COMPILATION_ERROR_URL, requestBody);
        return executeRequest(connection);
    }
    
    /**
     * 预编译测试代码，调用后端API
     * @param requestBody 请求体（包含package_name, class_name, empty_method, test_code）
     * @return API响应结果
     * @throws Exception 异常
     */
    public static String preCompile(String requestBody) throws Exception {
        HttpURLConnection connection = getHttpURLConnection(Constants.PRE_COMPILE_URL, requestBody);
        return executeRequest(connection);
    }
    
    /**
     * 执行HTTP请求并处理响应
     * @param connection HTTP连接
     * @return 响应结果
     * @throws Exception 异常
     */
    private static String executeRequest(HttpURLConnection connection) throws Exception {
        int responseCode = connection.getResponseCode();
        
        // 读取响应
        String response = readResponse(connection, responseCode);
        
        // 关闭连接
        connection.disconnect();
        
        // 检查响应码
        if (responseCode >= 300) {
            throw new Exception("API request failed with code " + responseCode + ": " + response);
        }
        
        return response;
    }
    
    /**
     * 读取HTTP响应
     * @param connection HTTP连接
     * @param responseCode 响应码
     * @return 响应内容
     * @throws IOException 异常
     */
    private static String readResponse(HttpURLConnection connection, int responseCode) throws IOException {
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        responseCode >= 200 && responseCode < 300 ? connection.getInputStream() : connection.getErrorStream(), 
                        StandardCharsets.UTF_8
                )
        )) {
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
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
