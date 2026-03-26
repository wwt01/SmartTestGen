package com.smarttestgen.ideaplugin.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 方法信息模型类
 */
public class MethodInfo {
    /** 方法名 */
    private String name;
    /** 返回类型 */
    private String returnType;
    /** 方法参数信息列表 */
    private List<ParameterInfo> parameters;

    public MethodInfo() {
        this.name = "";
        this.returnType = "void";
        this.parameters = new ArrayList<>();
    }

    public MethodInfo(String name, String returnType) {
        this.name = name;
        this.returnType = returnType;
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

    public void addParameter(ParameterInfo parameter) {
        this.parameters.add(parameter);
    }

    @Override
    public String toString() {
        return "MethodInfo{" +
                "name='" + name + '\'' +
                ", returnType='" + returnType + '\'' +
                ", parameters=" + parameters.size() +
                '}';
    }
}
