package com.smarttestgen.ideaplugin.model;

/**
 * 字段信息模型类
 */
public class FieldInfo {
    /** 字段名 */
    private String name;
    /** 字段类型 */
    private String type;
    /** 是否为私有字段 */
    private boolean isPrivate;

    public FieldInfo() {
        this.name = "";
        this.type = "";
        this.isPrivate = true;
    }

    public FieldInfo(String name, String type) {
        this.name = name;
        this.type = type;
        this.isPrivate = true;
    }

    public FieldInfo(String name, String type, boolean isPrivate) {
        this.name = name;
        this.type = type;
        this.isPrivate = isPrivate;
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

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    @Override
    public String toString() {
        return "FieldInfo{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", isPrivate=" + isPrivate +
                '}';
    }
}
