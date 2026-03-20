package com.smarttestgen.ideaplugin.util;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;

import java.util.List;

/**
 * 工具类，包含通用方法
 */
public class Utils {

    /**
     * 生成空方法代码
     * @param methodName 方法名
     * @param returnType 返回类型
     * @param parameters 参数信息列表
     * @return 空方法代码
     */
    public static String generateEmptyMethodCode(String methodName, String returnType, List<ParameterInfo> parameters) {
        StringBuilder methodCode = new StringBuilder();
        
        // 添加方法声明
        methodCode.append("public ").append(returnType).append(" ").append(methodName).append("(");
        
        // 添加参数
        for (int i = 0; i < parameters.size(); i++) {
            ParameterInfo param = parameters.get(i);
            methodCode.append(param.type).append(" ").append(param.name);
            if (i < parameters.size() - 1) {
                methodCode.append(", ");
            }
        }
        
        methodCode.append(") {");
        
        // 添加方法体
        if (!"void".equals(returnType)) {
            // 根据返回类型添加默认返回值
            if ("int".equals(returnType) || "long".equals(returnType) || "float".equals(returnType) || "double".equals(returnType)) {
                methodCode.append("\n    return 0;");
            } else if ("boolean".equals(returnType)) {
                methodCode.append("\n    return false;");
            } else {
                methodCode.append("\n    return null;");
            }
        }
        
        methodCode.append("\n}");
        
        return methodCode.toString();
    }

    /**
     * 解析代码结构
     * @param code 代码内容
     * @return 代码结构
     */
    public static String parseCodeStructure(String code) {
        // 简单的代码结构解析，实际项目中可能需要更复杂的解析
        StringBuilder structure = new StringBuilder();
        
        // 按行分割代码
        String[] lines = code.split("\\n");
        
        // 解析类和方法
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("class ") || line.startsWith("interface ")) {
                structure.append("\n").append(line);
            } else if (line.startsWith("public ") || line.startsWith("private ") || line.startsWith("protected ")) {
                if (line.contains("(")) {
                    structure.append("\n  " + line);
                }
            }
        }
        
        return structure.toString();
    }

    /**
     * 获取选中文本
     * @return 选中文本
     */
    public static String getSelectedText() {
        // 获取当前项目和编辑器
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        Project project = projects.length > 0 ? projects[0] : null;
        
        if (project == null) {
            return "";
        }
        
        // 获取当前编辑的文件
        Editor[] editors = EditorFactory.getInstance().getAllEditors();
        Editor currentEditor = null;
        for (Editor editor : editors) {
            if (editor.getProject() == project) {
                currentEditor = editor;
                break;
            }
        }
        
        if (currentEditor == null) {
            return "";
        }
        
        // 获取选中文本
        SelectionModel selectionModel = currentEditor.getSelectionModel();
        return selectionModel.getSelectedText();
    }

    /**
     * 获取选中文本的起始位置
     * @return 选中文本的起始位置
     */
    public static int getSelectionStart() {
        // 获取当前项目和编辑器
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        Project project = projects.length > 0 ? projects[0] : null;
        
        if (project == null) {
            return -1;
        }
        
        // 获取当前编辑的文件
        Editor[] editors = EditorFactory.getInstance().getAllEditors();
        Editor currentEditor = null;
        for (Editor editor : editors) {
            if (editor.getProject() == project) {
                currentEditor = editor;
                break;
            }
        }
        
        if (currentEditor == null) {
            return -1;
        }
        
        // 获取选中文本的起始位置
        SelectionModel selectionModel = currentEditor.getSelectionModel();
        return selectionModel.getSelectionStart();
    }

    /**
     * 获取选中文本的结束位置
     * @return 选中文本的结束位置
     */
    public static int getSelectionEnd() {
        // 获取当前项目和编辑器
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        Project project = projects.length > 0 ? projects[0] : null;
        
        if (project == null) {
            return -1;
        }
        
        // 获取当前编辑的文件
        Editor[] editors = EditorFactory.getInstance().getAllEditors();
        Editor currentEditor = null;
        for (Editor editor : editors) {
            if (editor.getProject() == project) {
                currentEditor = editor;
                break;
            }
        }
        
        if (currentEditor == null) {
            return -1;
        }
        
        // 获取选中文本的结束位置
        SelectionModel selectionModel = currentEditor.getSelectionModel();
        return selectionModel.getSelectionEnd();
    }

    /**
     * 参数信息类，用于存储参数的名称和类型
     */
    public static class ParameterInfo {
        public String name;
        public String type;

        public ParameterInfo(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }

    /**
     * JSON 工具类，用于格式化 JSON 和解析代码结构
     */
    public static class JsonUtils {

        /**
         * 格式化 JSON
         * @param json JSON 字符串
         * @return 格式化后的 JSON 字符串
         */
        public static String formatJson(String json) {
            if (json == null || json.isEmpty()) {
                return "";
            }
            
            StringBuilder formatted = new StringBuilder();
            int indent = 0;
            boolean inQuotes = false;
            boolean inEscape = false;
            
            for (char c : json.toCharArray()) {
                if (inEscape) {
                    formatted.append(c);
                    inEscape = false;
                } else if (c == '\\') {
                    formatted.append(c);
                    inEscape = true;
                } else if (c == '"') {
                    formatted.append(c);
                    inQuotes = !inQuotes;
                } else if (!inQuotes) {
                    switch (c) {
                        case '{':
                        case '[':
                            formatted.append(c);
                            formatted.append('\n');
                            indent++;
                            addIndent(formatted, indent);
                            break;
                        case '}':
                        case ']':
                            formatted.append('\n');
                            indent--;
                            addIndent(formatted, indent);
                            formatted.append(c);
                            break;
                        case ',':
                            formatted.append(c);
                            formatted.append('\n');
                            addIndent(formatted, indent);
                            break;
                        case ':':
                            formatted.append(c);
                            formatted.append(' ');
                            break;
                        case ' ': case '\t': case '\n': case '\r':
                            // 忽略空白字符
                            break;
                        default:
                            formatted.append(c);
                    }
                } else {
                    formatted.append(c);
                }
            }
            
            return formatted.toString();
        }

        /**
         * 添加缩进
         * @param sb StringBuilder
         * @param indent 缩进级别
         */
        private static void addIndent(StringBuilder sb, int indent) {
            for (int i = 0; i < indent; i++) {
                sb.append("  ");
            }
        }

        /**
         * 解析代码结构
         * @param result 代码结构结果
         * @return 解析后的代码结构
         */
        public static String parseCodeStructure(String result) {
            // 简单的代码结构解析，实际项目中可能需要更复杂的解析
            return Utils.parseCodeStructure(result);
        }

        /**
         * 生成空方法代码
         * @param methodName 方法名
         * @param returnType 返回类型
         * @param parameters 参数信息列表
         * @return 空方法代码
         */
        public static String generateEmptyMethodCode(String methodName, String returnType, List<ParameterInfo> parameters) {
            return Utils.generateEmptyMethodCode(methodName, returnType, parameters);
        }
    }
}
