package com.smarttestgen.ideaplugin.model;

/**
 * 参数信息模型类
 */
public class ParameterInfo {
    /** 参数名 */
    private String name;
    /** 参数类型 */
    private String type;

    public ParameterInfo() {
        this.name = "";
        this.type = "";
    }

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

    @Override
    public String toString() {
        return "ParameterInfo{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
