package com.smarttestgen.ideaplugin.service;

import com.intellij.openapi.vfs.VirtualFile;

import java.util.List;

/**
 * 文件查找服务类
 * 负责查找和过滤文件
 */
public class FileFinderService {
    
    /**
     * 递归查找所有Java文件
     * @param directory 要搜索的目录
     * @param javaFiles 用于存储找到的Java文件的列表
     */
    public static void findJavaFiles(VirtualFile directory, List<VirtualFile> javaFiles) {
        if (directory == null || !directory.isDirectory()) {
            return;
        }
        
        // 跳过构建目录和测试目录
        String path = directory.getPath();
        if (shouldSkipDirectory(path)) {
            return;
        }
        
        VirtualFile[] children = directory.getChildren();
        if (children == null) {
            return;
        }
        
        for (VirtualFile child : children) {
            if (child.isDirectory()) {
                findJavaFiles(child, javaFiles);
            } else if ("java".equals(child.getExtension())) {
                javaFiles.add(child);
            }
        }
    }
    
    /**
     * 判断是否应该跳过该目录
     * @param path 目录路径
     * @return 是否应该跳过
     */
    private static boolean shouldSkipDirectory(String path) {
        return path.contains("\\build\\") || 
               path.contains("\\out\\") || 
               path.contains(".gradle") ||
               path.contains("/build/") || 
               path.contains("/out/");
    }
}
