package com.smarttestgen.ideaplugin.util;

/**
 * JSON解析工具类
 */
public class JsonUtils {
    /**
     * 从 JSON 字符串中提取字段值（适用于 data.structured_result 格式）
     * @param json JSON字符串
     * @param fieldName 字段名
     * @return 字段值
     */
    public static String extractField(String json, String fieldName) {
        try {
            // 先找到 data 字段
            String dataPattern = "\"data\":{";
            int dataStartIndex = json.indexOf(dataPattern);
            if (dataStartIndex == -1) return "";
            
            dataStartIndex += dataPattern.length();
            
            // 再找到 structured_result 字段
            String structuredResultPattern = "\"structured_result\":{";
            int structuredResultStartIndex = json.indexOf(structuredResultPattern, dataStartIndex);
            if (structuredResultStartIndex == -1) return "";
            
            structuredResultStartIndex += structuredResultPattern.length();
            
            // 最后找到目标字段
            String fieldPattern = "\"" + fieldName + "\":\"";
            int fieldStartIndex = json.indexOf(fieldPattern, structuredResultStartIndex);
            if (fieldStartIndex == -1) return "";
            
            fieldStartIndex += fieldPattern.length();
            
            // 找到对应的结束引号，考虑转义字符
            int fieldEndIndex = findMatchingQuote(json, fieldStartIndex);
            if (fieldEndIndex == -1) return "";
            
            return json.substring(fieldStartIndex, fieldEndIndex);
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * 从 JSON 字符串中提取 data 下的字段值（适用于生成测试代码接口响应格式）
     * @param json JSON字符串
     * @param fieldName 字段名
     * @return 字段值
     */
    public static String extractDataField(String json, String fieldName) {
        try {
            // 先找到 data 字段
            String dataPattern = "\"data\":{";
            int dataStartIndex = json.indexOf(dataPattern);
            if (dataStartIndex == -1) return "";
            
            dataStartIndex += dataPattern.length();
            
            // 找到目标字段
            String fieldPattern = "\"" + fieldName + "\":";
            int fieldStartIndex = json.indexOf(fieldPattern, dataStartIndex);
            if (fieldStartIndex == -1) return "";
            
            fieldStartIndex += fieldPattern.length();
            
            // 跳过空白字符
            while (fieldStartIndex < json.length() && Character.isWhitespace(json.charAt(fieldStartIndex))) {
                fieldStartIndex++;
            }
            
            // 检查字段值是否被引号包围
            if (fieldStartIndex < json.length() && json.charAt(fieldStartIndex) == '"') {
                // 带引号的字段值
                fieldStartIndex++;
                int fieldEndIndex = findMatchingQuote(json, fieldStartIndex);
                if (fieldEndIndex == -1) return "";
                return json.substring(fieldStartIndex, fieldEndIndex);
            } else {
                // 不带引号的字段值（可能是多行文本或对象）
                // 对于多行文本，我们需要找到对应的结束标记
                // 这里采用更简单的方法：找到下一个逗号或右花括号
                int fieldEndIndex = json.indexOf(',', fieldStartIndex);
                if (fieldEndIndex == -1) {
                    fieldEndIndex = json.indexOf('}', fieldStartIndex);
                }
                if (fieldEndIndex == -1) return "";
                
                // 提取字段值并去除首尾空白
                String fieldValue = json.substring(fieldStartIndex, fieldEndIndex).trim();
                
                // 如果是多行文本，可能包含转义字符，需要处理
                return fieldValue;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
    
    /**
     * 查找匹配的结束引号，处理多行文本
     * @param json JSON字符串
     * @param startIndex 开始索引
     * @return 结束引号的索引
     */
    private static int findMatchingQuote(String json, int startIndex) {
        int index = startIndex;
        boolean inEscape = false;
        
        while (index < json.length()) {
            char c = json.charAt(index);
            
            if (c == '\\' && !inEscape) {
                inEscape = true;
            } else if (c == '"' && !inEscape) {
                return index;
            } else {
                inEscape = false;
            }
            
            index++;
        }
        
        return -1;
    }
    
    /**
     * 从 JSON 字符串中提取布尔字段值
     * @param json JSON字符串
     * @param fieldName 字段名
     * @param parentField 父字段名
     * @return 布尔字段值
     */
    public static boolean extractBooleanField(String json, String fieldName, String parentField) {
        try {
            // 先找到 data 字段
            String dataPattern = "\"data\":{";
            int dataStartIndex = json.indexOf(dataPattern);
            if (dataStartIndex == -1) return false;
            
            dataStartIndex += dataPattern.length();
            
            // 再找到 structured_result 字段
            String structuredResultPattern = "\"structured_result\":{";
            int structuredResultStartIndex = json.indexOf(structuredResultPattern, dataStartIndex);
            if (structuredResultStartIndex == -1) return false;
            
            structuredResultStartIndex += structuredResultPattern.length();
            
            // 找到父字段
            String parentPattern = "\"" + parentField + "\":{";
            int parentStartIndex = json.indexOf(parentPattern, structuredResultStartIndex);
            if (parentStartIndex == -1) return false;
            
            parentStartIndex += parentPattern.length();
            
            // 找到目标字段
            String fieldPattern = "\"" + fieldName + "\":";
            int fieldStartIndex = json.indexOf(fieldPattern, parentStartIndex);
            if (fieldStartIndex == -1) return false;
            
            fieldStartIndex += fieldPattern.length();
            int fieldEndIndex = json.indexOf(",", fieldStartIndex);
            if (fieldEndIndex == -1) fieldEndIndex = json.indexOf("}", fieldStartIndex);
            if (fieldEndIndex == -1) return false;
            
            return Boolean.parseBoolean(json.substring(fieldStartIndex, fieldEndIndex).trim());
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 从 JSON 字符串中提取数组字段值
     * @param json JSON字符串
     * @param fieldName 字段名
     * @return 数组字段值
     */
    public static String extractArrayField(String json, String fieldName) {
        try {
            // 先找到 data 字段
            String dataPattern = "\"data\":{";
            int dataStartIndex = json.indexOf(dataPattern);
            if (dataStartIndex == -1) return "";
            
            dataStartIndex += dataPattern.length();
            
            // 再找到 structured_result 字段
            String structuredResultPattern = "\"structured_result\":{";
            int structuredResultStartIndex = json.indexOf(structuredResultPattern, dataStartIndex);
            if (structuredResultStartIndex == -1) return "";
            
            structuredResultStartIndex += structuredResultPattern.length();
            
            // 最后找到目标字段
            String fieldPattern = "\"" + fieldName + "\":[";
            int fieldStartIndex = json.indexOf(fieldPattern, structuredResultStartIndex);
            if (fieldStartIndex == -1) return "";
            
            fieldStartIndex += fieldPattern.length();
            int fieldEndIndex = json.indexOf("]", fieldStartIndex);
            if (fieldEndIndex == -1) return "";
            
            return json.substring(fieldStartIndex, fieldEndIndex).trim();
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * 格式化 JSON 字符串，使其更易读
     * @param jsonString JSON字符串
     * @return 格式化后的 JSON 字符串
     */
    public static String formatJson(String jsonString) {
        StringBuilder sb = new StringBuilder();
        int indentLevel = 0;
        boolean inString = false;
        
        for (char c : jsonString.toCharArray()) {
            switch (c) {
                case '{':
                case '[':
                    sb.append(c);
                    if (!inString) {
                        sb.append('\n');
                        appendIndent(sb, ++indentLevel);
                    }
                    break;
                case '}':
                case ']':
                    if (!inString) {
                        sb.append('\n');
                        appendIndent(sb, --indentLevel);
                    }
                    sb.append(c);
                    break;
                case ',':
                    sb.append(c);
                    if (!inString) {
                        sb.append('\n');
                        appendIndent(sb, indentLevel);
                    }
                    break;
                case ':':
                    sb.append(c);
                    if (!inString) sb.append(' ');
                    break;
                case '"':
                    sb.append(c);
                    inString = !inString;
                    break;
                default:
                    sb.append(c);
            }
        }
        
        return sb.toString();
    }
    
    /**
     * 追加缩进
     * @param sb StringBuilder
     * @param indentLevel 缩进级别
     */
    private static void appendIndent(StringBuilder sb, int indentLevel) {
        for (int i = 0; i < indentLevel; i++) {
            sb.append("  ");
        }
    }
    
    /**
     * 参数信息类
     */
    public static class ParameterInfo {
        public String name;
        public String type;
        public String constraints;
        
        public ParameterInfo(String name, String type, String constraints) {
            this.name = name;
            this.type = type;
            this.constraints = constraints;
        }
    }
    
    /**
     * 解析参数数组
     * @param json JSON字符串
     * @param fieldName 字段名
     * @return 参数信息列表
     */
    public static java.util.List<ParameterInfo> extractParameters(String json, String fieldName) {
        java.util.List<ParameterInfo> parameters = new java.util.ArrayList<>();
        
        try {
            // 先找到 data 字段
            String dataPattern = "\"data\":{";
            int dataStartIndex = json.indexOf(dataPattern);
            if (dataStartIndex == -1) return parameters;
            
            dataStartIndex += dataPattern.length();
            
            // 再找到 structured_result 字段
            String structuredResultPattern = "\"structured_result\":{";
            int structuredResultStartIndex = json.indexOf(structuredResultPattern, dataStartIndex);
            if (structuredResultStartIndex == -1) return parameters;
            
            structuredResultStartIndex += structuredResultPattern.length();
            
            // 最后找到目标字段
            String fieldPattern = "\"" + fieldName + "\":[";
            int fieldStartIndex = json.indexOf(fieldPattern, structuredResultStartIndex);
            if (fieldStartIndex == -1) return parameters;
            
            fieldStartIndex += fieldPattern.length();
            
            // 找到数组的结束位置
            int fieldEndIndex = findMatchingBracket(json, fieldStartIndex, '[', ']');
            if (fieldEndIndex == -1) return parameters;
            
            String arrayContent = json.substring(fieldStartIndex, fieldEndIndex);
            
            // 解析每个参数对象
            int pos = 0;
            while (pos < arrayContent.length()) {
                // 跳过空白字符
                while (pos < arrayContent.length() && Character.isWhitespace(arrayContent.charAt(pos))) {
                    pos++;
                }
                
                if (pos >= arrayContent.length() || arrayContent.charAt(pos) == ']') {
                    break;
                }
                
                if (arrayContent.charAt(pos) == '{') {
                    int objEnd = findMatchingBracket(arrayContent, pos + 1, '{', '}');
                    if (objEnd == -1) break;
                    
                    String objContent = arrayContent.substring(pos + 1, objEnd);
                    ParameterInfo param = parseParameter(objContent);
                    if (param != null) {
                        parameters.add(param);
                    }
                    
                    pos = objEnd + 1;
                }
                
                // 跳过逗号
                while (pos < arrayContent.length() && Character.isWhitespace(arrayContent.charAt(pos))) {
                    pos++;
                }
                if (pos < arrayContent.length() && arrayContent.charAt(pos) == ',') {
                    pos++;
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return parameters;
    }
    
    /**
     * 解析单个参数对象
     * @param objContent 对象内容
     * @return 参数信息
     */
    private static ParameterInfo parseParameter(String objContent) {
        String name = "";
        String type = "";
        String constraints = "";
        
        try {
            // 提取 name
            String namePattern = "\"name\":\"";
            int nameIndex = objContent.indexOf(namePattern);
            if (nameIndex != -1) {
                int nameStart = nameIndex + namePattern.length();
                int nameEnd = findMatchingQuote(objContent, nameStart);
                if (nameEnd != -1) {
                    name = objContent.substring(nameStart, nameEnd);
                }
            }
            
            // 提取 type
            String typePattern = "\"type\":\"";
            int typeIndex = objContent.indexOf(typePattern);
            if (typeIndex != -1) {
                int typeStart = typeIndex + typePattern.length();
                int typeEnd = findMatchingQuote(objContent, typeStart);
                if (typeEnd != -1) {
                    type = objContent.substring(typeStart, typeEnd);
                }
            }
            
            // 提取 constraints
            String constraintsPattern = "\"constraints\":[";
            int constraintsIndex = objContent.indexOf(constraintsPattern);
            if (constraintsIndex != -1) {
                int constraintsStart = constraintsIndex + constraintsPattern.length();
                int constraintsEnd = findMatchingBracket(objContent, constraintsStart, '[', ']');
                if (constraintsEnd != -1) {
                    constraints = objContent.substring(constraintsStart, constraintsEnd);
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return new ParameterInfo(name, type, constraints);
    }
    
    /**
     * 查找匹配的括号
     * @param str 字符串
     * @param startIndex 开始索引（从第一个字符开始）
     * @param openChar 开括号
     * @param closeChar 闭括号
     * @return 闭括号的索引
     */
    private static int findMatchingBracket(String str, int startIndex, char openChar, char closeChar) {
        int depth = 1;  // 从1开始，因为第一个字符已经是开括号
        for (int i = startIndex; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == openChar) {
                depth++;
            } else if (c == closeChar) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
}
