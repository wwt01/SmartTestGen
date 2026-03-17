package com.smarttestgen.ideaplugin.dialog;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件管理器，负责文件操作相关功能
 */
public class FileManager {

    /**
     * 递归查找所有Java文件
     */
    public static void findJavaFiles(VirtualFile directory, List<VirtualFile> javaFiles) {
        if (directory == null || !directory.isDirectory()) {
            return;
        }
        
        // 跳过构建目录和测试目录
        String path = directory.getPath();
        if (path.contains("\\build\\") || path.contains("\\out\\") || path.contains(".gradle")) {
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
     * 获取当前编辑文件的内容
     * @return 文件内容
     */
    public static String getCurrentFileContent() {
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
        
        // 获取文件内容
        Document document = currentEditor.getDocument();
        return document.getText();
    }

    /**
     * 从文件路径获取package名称
     */
    public static String getPackageNameFromPath(String filePath) {
        // 移除文件扩展名
        String pathWithoutExt = filePath.replaceFirst("\\.java$", "");
        
        // 移除src/test/java前缀
        pathWithoutExt = pathWithoutExt.replace("src\\test\\java\\", "")
            .replace("src/test/java/", "")
            .replace("src\\test\\java", "")
            .replace("src/test/java", "");
        
        // 移除文件名
        int lastSeparatorIndex = pathWithoutExt.lastIndexOf('\\');
        if (lastSeparatorIndex == -1) {
            lastSeparatorIndex = pathWithoutExt.lastIndexOf('/');
        }
        if (lastSeparatorIndex != -1) {
            pathWithoutExt = pathWithoutExt.substring(0, lastSeparatorIndex);
        }
        
        // 将路径分隔符替换为点
        String packageName = pathWithoutExt.replace('\\', '.').replace('/', '.');
        
        System.out.println("[Test Case Generator] Derived package name: " + packageName);
        return packageName;
    }

    /**
     * 更新测试代码顶部的package信息
     */
    public static String updatePackageInfo(String code, String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return code;
        }
        
        // 查找现有的package声明
        String packageDeclaration = "package " + packageName + ";";
        
        // 如果代码中已经有package声明，替换它
        if (code.startsWith("package ")) {
            int endOfPackageLine = code.indexOf(';');
            if (endOfPackageLine != -1) {
                return packageDeclaration + code.substring(endOfPackageLine + 1);
            }
        }
        // 如果代码中没有package声明，在开头添加
        else {
            return packageDeclaration + "\n\n" + code;
        }
        
        return code;
    }

    /**
     * 创建测试文件
     * @param code 测试代码
     * @param window 父窗口
     * @param selectedFilePath 选中文本所在的文件路径
     */
    public static void createTestFile(String code, JComponent window, String selectedFilePath) {
        // 获取当前项目和编辑器
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        Project project = projects.length > 0 ? projects[0] : null;
        
        if (project == null) {
            JOptionPane.showMessageDialog(window, "No open project found", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // 获取当前文件的虚拟文件
        VirtualFile currentFile = null;
        if (selectedFilePath != null && !selectedFilePath.isEmpty()) {
            // 使用提供的文件路径
            VirtualFileSystem fileSystem = VirtualFileManager.getInstance().getFileSystem("file");
            currentFile = fileSystem.findFileByPath(selectedFilePath);
            System.out.println("[Test Case Generator] Using provided selectedFilePath: " + selectedFilePath);
        }
        
        // 如果没有提供文件路径或文件不存在，尝试获取当前编辑的文件
        if (currentFile == null) {
            Editor[] editors = EditorFactory.getInstance().getAllEditors();
            Editor currentEditor = null;
            for (Editor editor : editors) {
                if (editor.getProject() == project) {
                    currentEditor = editor;
                    break;
                }
            }
            
            if (currentEditor != null) {
                currentFile = currentEditor.getVirtualFile();
                System.out.println("[Test Case Generator] Using current editor file: " + (currentFile != null ? currentFile.getPath() : "null"));
            }
        }
        
        if (currentFile == null) {
            JOptionPane.showMessageDialog(window, "No active file found", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            // 推导测试文件路径
            String currentFilePath = currentFile.getPath();
            System.out.println("[Test Case Generator] Current file path: " + currentFilePath);
            
            // 替换src/main/java为src/test/java（同时处理Windows和Unix路径分隔符）
            String testFilePath = currentFilePath;
            // 先处理Windows格式的路径
            if (testFilePath.contains("src\\main\\java")) {
                testFilePath = testFilePath.replace("src\\main\\java", "src\\test\\java");
            }
            // 再处理Unix格式的路径
            else if (testFilePath.contains("src/main/java")) {
                testFilePath = testFilePath.replace("src/main/java", "src/test/java");
            }
            // 确保路径分隔符一致性
            testFilePath = testFilePath.replace("\\", "/");
            
            // 替换文件名，添加Test后缀
            int lastDotIndex = testFilePath.lastIndexOf('.');
            if (lastDotIndex != -1) {
                testFilePath = testFilePath.substring(0, lastDotIndex) + "Test" + testFilePath.substring(lastDotIndex);
            } else {
                // 如果没有扩展名，添加.java扩展名
                testFilePath += "Test.java";
            }
            
            System.out.println("[Test Case Generator] Test file path: " + testFilePath);
            System.out.println("[Test Case Generator] Current file path: " + currentFilePath);
            
            // 原封不动地使用生成的测试代码，无需改动
            String updatedCode = code;
            
            // 创建目录结构
            // 同时处理 Windows 和 Unix 路径分隔符
            int lastSeparatorIndex = Math.max(testFilePath.lastIndexOf('/'), testFilePath.lastIndexOf('\\'));
            String testDirPath = testFilePath.substring(0, lastSeparatorIndex);
            
            // 创建final副本
            final Project finalProject = project;
            final String finalUpdatedCode = updatedCode;
            final String finalTestFilePath = testFilePath;
            final String finalTestDirPath = testDirPath;
            
            // 所有修改文件系统的操作都需要在 write-action 中执行
            com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project, () -> {
                try {
                    // 获取或创建测试目录
                    VirtualFile projectRoot = finalProject.getBaseDir();
                    if (projectRoot == null) {
                        throw new Exception("Project root is null");
                    }
                    
                    String relativePath = finalTestDirPath.replace(projectRoot.getPath(), "");
                    // 移除开头的路径分隔符
                    if (relativePath.startsWith("/") || relativePath.startsWith("\\")) {
                        relativePath = relativePath.substring(1);
                    }
                    
                    System.out.println("[Test Case Generator] Relative path: " + relativePath);
                    
                    VirtualFile testDir = projectRoot.findFileByRelativePath(relativePath);
                    if (testDir == null) {
                        // 创建目录结构
                        // 使用统一的'/'分隔符处理路径
                        String[] dirs = relativePath.split("/|\\\\");
                        // 过滤空目录名
                        java.util.List<String> dirList = new java.util.ArrayList<>();
                        for (String dir : dirs) {
                            if (!dir.isEmpty()) {
                                dirList.add(dir);
                            }
                        }
                        dirs = dirList.toArray(new String[0]);
                        VirtualFile currentDir = projectRoot;
                        for (String dir : dirs) {
                            if (currentDir == null) {
                                throw new Exception("Current directory is null while creating directory structure");
                            }
                            VirtualFile subDir = currentDir.findChild(dir);
                            if (subDir == null) {
                                try {
                                    subDir = currentDir.createChildDirectory(null, dir);
                                    System.out.println("[Test Case Generator] Created directory: " + dir);
                                } catch (Exception e) {
                                    System.out.println("[Test Case Generator] Error creating directory " + dir + ": " + e.getMessage());
                                    throw e; // 重新抛出异常，确保整个操作失败
                                }
                            }
                            currentDir = subDir;
                        }
                        testDir = currentDir;
                    }
                    
                    if (testDir == null) {
                        throw new Exception("Failed to create test directory: " + finalTestDirPath);
                    }
                    
                    System.out.println("[Test Case Generator] Test directory found/created: " + testDir.getPath());
                    
                    // 创建测试文件
                    int testFileLastSeparatorIndex = Math.max(finalTestFilePath.lastIndexOf('/'), finalTestFilePath.lastIndexOf('\\'));
                    String testFileName = finalTestFilePath.substring(testFileLastSeparatorIndex + 1);
                    VirtualFile testVirtualFile = testDir.findChild(testFileName);
                    if (testVirtualFile == null) {
                        testVirtualFile = testDir.createChildData(null, testFileName);
                        System.out.println("[Test Case Generator] Created test file: " + testFileName);
                    } else {
                        System.out.println("[Test Case Generator] Test file already exists: " + testFileName);
                    }
                    
                    // 写入测试代码
                    testVirtualFile.setBinaryContent(finalUpdatedCode.getBytes("UTF-8"));
                    System.out.println("[Test Case Generator] Wrote test code to file: " + finalTestFilePath);
                    
                    // 显示成功消息
                    SwingUtilities.invokeLater(() -> {
                        String fileName = finalTestFilePath.substring(Math.max(finalTestFilePath.lastIndexOf('/'), finalTestFilePath.lastIndexOf('\\')) + 1);
                        JOptionPane.showMessageDialog(window, "Test file created successfully!\n\nFile: " + fileName + "\nPath: " + finalTestFilePath, "Success", JOptionPane.INFORMATION_MESSAGE);
                    });
                } catch (Exception e) {
                    System.out.println("[Test Case Generator] Error creating test file: " + e.getMessage());
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(window, "Error creating test file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    });
                }
            });
        } catch (Exception e) {
            System.out.println("[Test Case Generator] Error creating test file: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(window, "Error creating test file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
