package com.smarttestgen.ideaplugin.service;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileSystem;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 测试文件创建服务类
 * 负责创建测试文件
 */
public class TestFileCreatorService {
    
    /**
     * 创建测试文件
     * @param code 测试代码
     * @param window 父窗口
     * @param selectedFilePath 选中文本所在的文件路径
     */
    public static void createTestFile(String code, JComponent window, String selectedFilePath) {
        // 获取当前项目
        Project project = getCurrentProject(window);
        if (project == null) {
            return;
        }
        
        // 获取当前文件的虚拟文件
        VirtualFile currentFile = getCurrentFile(project, selectedFilePath);
        if (currentFile == null) {
            JOptionPane.showMessageDialog(window, "No active file found", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            // 推导测试文件路径
            String testFilePath = deriveTestFilePath(currentFile);
            
            // 创建测试文件
            createTestFileInProject(project, currentFile, testFilePath, code, window);
        } catch (Exception e) {
            System.out.println("[Test Case Generator] Error creating test file: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(window, "Error creating test file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * 获取当前项目
     * @param window 父窗口
     * @return 当前项目
     */
    private static Project getCurrentProject(JComponent window) {
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        Project project = projects.length > 0 ? projects[0] : null;
        
        if (project == null) {
            JOptionPane.showMessageDialog(window, "No open project found", "Error", JOptionPane.ERROR_MESSAGE);
        }
        
        return project;
    }
    
    /**
     * 获取当前文件
     * @param project 项目
     * @param selectedFilePath 选中的文件路径
     * @return 当前文件
     */
    private static VirtualFile getCurrentFile(Project project, String selectedFilePath) {
        VirtualFile currentFile = null;
        
        // 使用提供的文件路径
        if (selectedFilePath != null && !selectedFilePath.isEmpty()) {
            VirtualFileSystem fileSystem = VirtualFileManager.getInstance().getFileSystem("file");
            currentFile = fileSystem.findFileByPath(selectedFilePath);
            System.out.println("[Test Case Generator] Using provided selectedFilePath: " + selectedFilePath);
        }
        
        // 如果没有提供文件路径或文件不存在，尝试获取当前编辑的文件
        if (currentFile == null) {
            currentFile = getCurrentEditorFile(project);
            System.out.println("[Test Case Generator] Using current editor file: " + 
                (currentFile != null ? currentFile.getPath() : "null"));
        }
        
        return currentFile;
    }
    
    /**
     * 获取当前编辑器的文件
     * @param project 项目
     * @return 当前编辑器的文件
     */
    private static VirtualFile getCurrentEditorFile(Project project) {
        Editor[] editors = EditorFactory.getInstance().getAllEditors();
        for (Editor editor : editors) {
            if (editor.getProject() == project) {
                return editor.getVirtualFile();
            }
        }
        return null;
    }
    
    /**
     * 推导测试文件路径
     * @param currentFile 当前文件
     * @return 测试文件路径
     */
    private static String deriveTestFilePath(VirtualFile currentFile) {
        String currentFilePath = currentFile.getPath();
        System.out.println("[Test Case Generator] Current file path: " + currentFilePath);
        
        // 转换为测试路径
        String testFilePath = PathService.convertToTestPath(currentFilePath);
        
        // 添加Test后缀
        testFilePath = PathService.addTestSuffix(testFilePath);
        
        System.out.println("[Test Case Generator] Test file path: " + testFilePath);
        return testFilePath;
    }
    
    /**
     * 在项目中创建测试文件
     * @param project 项目
     * @param currentFile 当前文件
     * @param testFilePath 测试文件路径
     * @param code 测试代码
     * @param window 父窗口
     */
    private static void createTestFileInProject(Project project, VirtualFile currentFile, 
                                            String testFilePath, String code, JComponent window) {
        // 获取测试目录路径
        String testDirPath = extractDirectoryPath(testFilePath);
        
        // 创建final副本
        final Project finalProject = project;
        final String finalUpdatedCode = code;
        final String finalTestFilePath = testFilePath;
        final String finalTestDirPath = testDirPath;
        
        // 所有修改文件系统的操作都需要在 write-action 中执行
        com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project, () -> {
            try {
                // 获取或创建测试目录
                VirtualFile testDir = createTestDirectory(finalProject, finalTestDirPath);
                
                // 创建测试文件
                createTestFile(testDir, finalTestFilePath, finalUpdatedCode);
                
                // 显示成功消息
                showSuccessMessage(window, finalTestFilePath);
            } catch (Exception e) {
                System.out.println("[Test Case Generator] Error creating test file: " + e.getMessage());
                e.printStackTrace();
                showErrorMessage(window, e.getMessage());
            }
        });
    }
    
    /**
     * 提取目录路径
     * @param filePath 文件路径
     * @return 目录路径
     */
    private static String extractDirectoryPath(String filePath) {
        int lastSeparatorIndex = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        return filePath.substring(0, lastSeparatorIndex);
    }
    
    /**
     * 创建测试目录
     * @param project 项目
     * @param testDirPath 测试目录路径
     * @return 测试目录
     */
    private static VirtualFile createTestDirectory(Project project, String testDirPath) throws Exception {
        VirtualFile projectRoot = project.getBaseDir();
        if (projectRoot == null) {
            throw new Exception("Project root is null");
        }
        
        String relativePath = testDirPath.replace(projectRoot.getPath(), "");
        // 移除开头的路径分隔符
        if (relativePath.startsWith("/") || relativePath.startsWith("\\")) {
            relativePath = relativePath.substring(1);
        }
        
        System.out.println("[Test Case Generator] Relative path: " + relativePath);
        
        VirtualFile testDir = projectRoot.findFileByRelativePath(relativePath);
        if (testDir == null) {
            testDir = createDirectoryStructure(projectRoot, relativePath);
        }
        
        if (testDir == null) {
            throw new Exception("Failed to create test directory: " + testDirPath);
        }
        
        System.out.println("[Test Case Generator] Test directory found/created: " + testDir.getPath());
        return testDir;
    }
    
    /**
     * 创建目录结构
     * @param root 根目录
     * @param relativePath 相对路径
     * @return 创建的目录
     */
    private static VirtualFile createDirectoryStructure(VirtualFile root, String relativePath) throws Exception {
        // 使用统一的'/'分隔符处理路径
        String[] dirs = relativePath.split("/|\\\\");
        // 过滤空目录名
        List<String> dirList = new ArrayList<>();
        for (String dir : dirs) {
            if (!dir.isEmpty()) {
                dirList.add(dir);
            }
        }
        dirs = dirList.toArray(new String[0]);
        
        VirtualFile currentDir = root;
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
                    throw e;
                }
            }
            currentDir = subDir;
        }
        
        return currentDir;
    }
    
    /**
     * 创建测试文件
     * @param testDir 测试目录
     * @param testFilePath 测试文件路径
     * @param code 测试代码
     */
    private static void createTestFile(VirtualFile testDir, String testFilePath, String code) throws Exception {
        // 获取文件名
        int testFileLastSeparatorIndex = Math.max(testFilePath.lastIndexOf('/'), testFilePath.lastIndexOf('\\'));
        String testFileName = testFilePath.substring(testFileLastSeparatorIndex + 1);
        
        // 创建或获取测试文件
        VirtualFile testVirtualFile = testDir.findChild(testFileName);
        if (testVirtualFile == null) {
            testVirtualFile = testDir.createChildData(null, testFileName);
            System.out.println("[Test Case Generator] Created test file: " + testFileName);
        } else {
            System.out.println("[Test Case Generator] Test file already exists: " + testFileName);
        }
        
        // 写入测试代码
        testVirtualFile.setBinaryContent(code.getBytes("UTF-8"));
        System.out.println("[Test Case Generator] Wrote test code to file: " + testFilePath);
    }
    
    /**
     * 显示成功消息
     * @param window 父窗口
     * @param testFilePath 测试文件路径
     */
    private static void showSuccessMessage(JComponent window, String testFilePath) {
        ThreadPoolService.getInstance().runInEdt(() -> {
            String fileName = testFilePath.substring(
                Math.max(testFilePath.lastIndexOf('/'), testFilePath.lastIndexOf('\\')) + 1
            );
            JOptionPane.showMessageDialog(
                window, 
                "Test file created successfully!\n\nFile: " + fileName + "\nPath: " + testFilePath, 
                "Success", 
                JOptionPane.INFORMATION_MESSAGE
            );
        });
    }
    
    /**
     * 显示错误消息
     * @param window 父窗口
     * @param errorMessage 错误消息
     */
    private static void showErrorMessage(JComponent window, String errorMessage) {
        ThreadPoolService.getInstance().runInEdt(() -> {
            JOptionPane.showMessageDialog(
                window, 
                "Error creating test file: " + errorMessage, 
                "Error", 
                JOptionPane.ERROR_MESSAGE
            );
        });
    }
}
