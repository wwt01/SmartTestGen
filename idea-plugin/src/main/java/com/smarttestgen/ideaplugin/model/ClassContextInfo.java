package com.smarttestgen.ideaplugin.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 当前类上下文信息模型类
 * 用于存储当前编辑文件的完整类信息
 */
public class ClassContextInfo {
    /** 完整类名（包含包名） */
    private String qualifiedName;
    /** 类名 */
    private String className;
    /** 包名 */
    private String packageName;
    /** 类类型：DTO/Service/Controller/Entity/VO/Unknown */
    private String classType;
    /** 是否为接口 */
    private boolean isInterface;
    /** 字段列表 */
    private List<FieldInfo> fields;
    /** 方法列表 */
    private List<MethodInfo> methods;
    /** 依赖类列表 */
    private List<String> dependencies;

    public ClassContextInfo() {
        this.qualifiedName = "";
        this.className = "";
        this.packageName = "";
        this.classType = "Unknown";
        this.isInterface = false;
        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();
        this.dependencies = new ArrayList<>();
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public void setQualifiedName(String qualifiedName) {
        this.qualifiedName = qualifiedName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getClassType() {
        return classType;
    }

    public void setClassType(String classType) {
        this.classType = classType;
    }

    public boolean isInterface() {
        return isInterface;
    }

    public void setInterface(boolean anInterface) {
        isInterface = anInterface;
    }

    public List<FieldInfo> getFields() {
        return fields;
    }

    public void setFields(List<FieldInfo> fields) {
        this.fields = fields;
    }

    public List<MethodInfo> getMethods() {
        return methods;
    }

    public void setMethods(List<MethodInfo> methods) {
        this.methods = methods;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    public void addField(FieldInfo field) {
        this.fields.add(field);
    }

    public void addMethod(MethodInfo method) {
        this.methods.add(method);
    }

    public void addDependency(String dependency) {
        this.dependencies.add(dependency);
    }

    @Override
    public String toString() {
        return "ClassContextInfo{" +
                "qualifiedName='" + qualifiedName + '\'' +
                ", className='" + className + '\'' +
                ", packageName='" + packageName + '\'' +
                ", classType='" + classType + '\'' +
                ", isInterface=" + isInterface +
                ", fields=" + fields.size() +
                ", methods=" + methods.size() +
                ", dependencies=" + dependencies.size() +
                '}';
    }
}
