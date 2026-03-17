package com.smarttestgen.ideaplugin.util;

/**
 * 常量定义类
 */
public class Constants {

    /**
     * 需求文本 提取结构化信息API请求URL
     */
    public static final String API_URL = "http://localhost:8000/api/text/parse";
    
    /**
     * 生成测试代码 API请求URL
     */
    public static final String GENERATE_TEST_URL = "http://localhost:8000/api/text/generate-test";
    
    /**
     * 修复编译错误 API请求URL
     */
    public static final String FIX_COMPILATION_ERROR_URL = "http://localhost:8000/api/text/fix-compilation-error";
    
    /**
     * 请求体字段名
     */
    public static final String REQUEST_BODY_FIELD = "content";
    
    /**
     * 响应体字段名
     */
    public static final String RESPONSE_DATA_FIELD = "data";
    public static final String RESPONSE_STRUCTURED_RESULT_FIELD = "structured_result";
    public static final String RESPONSE_METHOD_NAME_FIELD = "method_name";
    public static final String RESPONSE_PARAMETERS_FIELD = "parameters";
    public static final String RESPONSE_RETURN_TYPE_FIELD = "return_type";
    public static final String RESPONSE_EXPECTATIONS_FIELD = "expectations";
    public static final String RESPONSE_IS_CONSTRUCTED_FIELD = "is_constructed";
}
