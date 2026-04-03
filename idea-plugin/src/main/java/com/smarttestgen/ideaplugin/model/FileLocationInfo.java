package com.smarttestgen.ideaplugin.model;

import java.util.List;
import java.util.ArrayList;

/**
 * 文件位置信息模型类
 */
public class FileLocationInfo {
    /** 文件完整路径 */
    private String filePath;
    /** 文件名 */
    private String fileName;
    /** 选中文本所在行号 */
    private int lineNumber;
    /** 是否为接口类 */
    private boolean isInterface;
    /** 类名 */
    private String className;
    /** 实现该接口的类文件路径列表 */
    private List<String> implementationFiles;

    public FileLocationInfo() {
        this.filePath = "";
        this.fileName = "";
        this.lineNumber = -1;
        this.isInterface = false;
        this.className = "";
        this.implementationFiles = new ArrayList<>();
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public boolean isInterface() {
        return isInterface;
    }

    public void setInterface(boolean isInterface) {
        this.isInterface = isInterface;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<String> getImplementationFiles() {
        return implementationFiles;
    }

    public void setImplementationFiles(List<String> implementationFiles) {
        this.implementationFiles = implementationFiles;
    }

    @Override
    public String toString() {
        return "FileLocationInfo{" +
                "filePath='" + filePath + '\'' +
                ", fileName='" + fileName + '\'' +
                ", lineNumber=" + lineNumber +
                ", isInterface=" + isInterface +
                ", className='" + className + '\'' +
                ", implementationFiles=" + implementationFiles.size() +
                '}';
    }
}
