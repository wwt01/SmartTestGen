package com.smarttestgen.ideaplugin.model;

import java.util.List;

/**
 * 文件位置信息模型类
 */
public class FileLocationInfo {
    private String filePath;
    private String fileName;
    private int lineNumber;
    private boolean isInterface;
    private String className;
    private List<String> implementationFiles;

    public FileLocationInfo() {
        this.filePath = "";
        this.fileName = "";
        this.lineNumber = -1;
        this.isInterface = false;
        this.className = "";
        this.implementationFiles = new java.util.ArrayList<>();
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
