package com.smarttestgen.ideaplugin.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 代码结构信息模型类
 */
public class CodeStructureInfo {
    private String projectName;
    private List<ClassInfo> classes;
    
    public CodeStructureInfo() {
        this.projectName = "";
        this.classes = new ArrayList<>();
    }
    
    public String getProjectName() {
        return projectName;
    }
    
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
    
    public List<ClassInfo> getClasses() {
        return classes;
    }
    
    public void setClasses(List<ClassInfo> classes) {
        this.classes = classes;
    }
    
    /**
     * 添加类信息
     * @param classInfo 类信息
     */
    public void addClass(ClassInfo classInfo) {
        classes.add(classInfo);
    }
    
    /**
     * 转换为字符串表示
     * @return 格式化的代码结构字符串
     */
    public String toFormattedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("代码库结构：\n\n");
        sb.append("项目名称：").append(projectName).append("\n\n");
        sb.append("=== Java 类 ===\n");
        
        if (classes.isEmpty()) {
            sb.append("没有找到 Java 类\n");
        } else {
            for (ClassInfo classInfo : classes) {
                sb.append(classInfo.toFormattedString()).append("\n");
            }
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return "CodeStructureInfo{" +
                "projectName='" + projectName + '\'' +
                ", classes=" + classes.size() +
                '}';
    }
    
    /**
     * 类信息内部类
     */
    public static class ClassInfo {
        private String name;
        private List<MethodInfo> methods;
        
        public ClassInfo() {
            this.name = "";
            this.methods = new ArrayList<>();
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public List<MethodInfo> getMethods() {
            return methods;
        }
        
        public void setMethods(List<MethodInfo> methods) {
            this.methods = methods;
        }
        
        /**
         * 添加方法信息
         * @param methodInfo 方法信息
         */
        public void addMethod(MethodInfo methodInfo) {
            methods.add(methodInfo);
        }
        
        /**
         * 转换为字符串表示
         * @return 格式化的类信息字符串
         */
        public String toFormattedString() {
            StringBuilder sb = new StringBuilder();
            sb.append("类名：").append(name).append("\n");
            
            for (MethodInfo method : methods) {
                sb.append("  方法：").append(method.toFormattedString()).append("\n");
            }
            
            return sb.toString();
        }
    }
    
    /**
     * 方法信息内部类
     */
    public static class MethodInfo {
        private String name;
        private String returnType;
        private List<ParameterInfo> parameters;
        
        public MethodInfo() {
            this.name = "";
            this.returnType = "void";
            this.parameters = new ArrayList<>();
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getReturnType() {
            return returnType;
        }
        
        public void setReturnType(String returnType) {
            this.returnType = returnType;
        }
        
        public List<ParameterInfo> getParameters() {
            return parameters;
        }
        
        public void setParameters(List<ParameterInfo> parameters) {
            this.parameters = parameters;
        }
        
        /**
         * 添加参数信息
         * @param parameterInfo 参数信息
         */
        public void addParameter(ParameterInfo parameterInfo) {
            parameters.add(parameterInfo);
        }
        
        /**
         * 转换为字符串表示
         * @return 格式化的方法信息字符串
         */
        public String toFormattedString() {
            StringBuilder sb = new StringBuilder();
            sb.append(name).append("(");
            
            for (int i = 0; i < parameters.size(); i++) {
                ParameterInfo param = parameters.get(i);
                sb.append(param.getType()).append(" ").append(param.getName());
                if (i < parameters.size() - 1) {
                    sb.append(", ");
                }
            }
            
            sb.append(") : ").append(returnType);
            return sb.toString();
        }
    }
    
    /**
     * 参数信息内部类
     */
    public static class ParameterInfo {
        private String name;
        private String type;
        
        public ParameterInfo(String name, String type) {
            this.name = name;
            this.type = type;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
    }
}
