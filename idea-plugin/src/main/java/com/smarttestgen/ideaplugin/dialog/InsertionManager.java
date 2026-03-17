package com.smarttestgen.ideaplugin.dialog;

import com.smarttestgen.ideaplugin.service.FileFinderService;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 插入管理器，负责代码插入相关功能
 */
public class InsertionManager {

    /**
     * 插入空方法代码到原文件
     * @param emptyMethodCode 空方法代码
     * @param window 父窗口
     */
    public static void insertEmptyMethodToFile(String emptyMethodCode, JComponent window) {
        // 获取当前项目和编辑器
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        Project project = projects.length > 0 ? projects[0] : null;
        
        if (project == null) {
            JOptionPane.showMessageDialog(window, "No open project found", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // 获取当前编辑的文件，使用与getFileLocationInfo相同的方式
        Editor currentEditor = null;
        VirtualFile currentFile = null;
        try {
            // 尝试使用 FileEditorManager 获取当前活动的编辑器
            com.intellij.openapi.fileEditor.FileEditorManager fileEditorManager = 
                com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project);
            VirtualFile[] selectedFiles = fileEditorManager.getSelectedFiles();
            if (selectedFiles.length > 0) {
                currentFile = selectedFiles[0];
                System.out.println("[Test Case Generator] Using selected file from FileEditorManager: " + currentFile.getName());
                
                // 找到与该文件关联的编辑器
                for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
                    if (editor.getVirtualFile() != null && editor.getVirtualFile().equals(currentFile)) {
                        currentEditor = editor;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[Test Case Generator] Error getting file from FileEditorManager: " + e.getMessage());
        }
        
        // 如果使用 FileEditorManager 失败，尝试遍历所有编辑器
        if (currentEditor == null) {
            System.out.println("[Test Case Generator] Getting editor from all editors...");
            Editor[] editors = EditorFactory.getInstance().getAllEditors();
            for (Editor editor : editors) {
                if (editor.getProject() == project) {
                    currentEditor = editor;
                    currentFile = editor.getVirtualFile();
                    System.out.println("[Test Case Generator] Found editor from all editors: " + (currentFile != null ? currentFile.getName() : "unknown"));
                    break;
                }
            }
        }
        
        if (currentEditor == null) {
            JOptionPane.showMessageDialog(window, "No active editor found", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (currentFile == null) {
            JOptionPane.showMessageDialog(window, "No active file found", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // 获取选中范围
        SelectionModel selectionModel = currentEditor.getSelectionModel();
        // 使用当前编辑器的选择，确保使用最新的选中文本位置
        final int finalSelectionEnd = selectionModel.getSelectionEnd();
        System.out.println("[Test Case Generator] InsertionManager using selection end: " + finalSelectionEnd);
        
        // 创建final副本
        final Editor finalEditor = currentEditor;
        final String finalEmptyMethodCode = emptyMethodCode;
        final Project finalProject = project;
        final VirtualFile finalCurrentFile = currentFile;
        
        // 插入代码到文件
        try {
            // 使用WriteCommandAction来修改文档，支持撤销操作
            WriteCommandAction.runWriteCommandAction(project, () -> {
                try {
                    Document currentDocument = finalEditor.getDocument();
                    
                    // 找到选中文本所在行的行号
                    int lineNumber = currentDocument.getLineNumber(finalSelectionEnd);
                    // 找到选中文本所在行的结束位置
                    int lineEndOffset = currentDocument.getLineEndOffset(lineNumber);
                    // 插入点设置为选中文本所在行的结束位置，这样空方法会插入到下一行
                    int insertionPoint = lineEndOffset;
                    
                    // 检查当前文件是否是接口类
                    boolean isInterface = false;
                    String interfaceName = null;
                    PsiFile psiFile = PsiManager.getInstance(finalProject).findFile(finalCurrentFile);
                    if (psiFile != null) {
                        // 遍历 psiFile 的所有子元素，查找 PsiClass 元素
                        for (PsiElement element : psiFile.getChildren()) {
                            if (element instanceof PsiClass) {
                                PsiClass psiClass = (PsiClass) element;
                                isInterface = psiClass.isInterface();
                                interfaceName = psiClass.getName();
                                break;
                            }
                        }
                    }
                    
                    // 创建final副本，用于lambda表达式
                    final boolean finalIsInterface = isInterface;
                    
                    // 查找实现了该接口的所有类
                    List<VirtualFile> implementationFiles = new ArrayList<>();
                    
                    if (isInterface && interfaceName != null) {
                        // 对于接口类，只插入方法声明，不插入方法体
                        String methodDeclaration = finalEmptyMethodCode;
                        // 简化方法声明，移除方法体
                        methodDeclaration = methodDeclaration.replaceAll("\\{[\\s\\S]*?\\}", ";");
                        // 移除修饰符，接口方法默认为 public abstract
                        methodDeclaration = methodDeclaration.replaceAll("public\\s+", "");
                        methodDeclaration = methodDeclaration.replaceAll("abstract\\s+", "");
                        
                        // 插入方法声明到选中文本的正下方（下一行）
                        currentDocument.insertString(insertionPoint, "\n\n" + methodDeclaration);
                        
                        // 查找实现了该接口的所有类
                        // 创建final副本，用于lambda表达式
                        final String finalInterfaceName = interfaceName;
                        // 直接在主线程中查找实现类，确保实现类查找完成后再执行插入操作
                        try {
                            // 获取项目根目录
                            VirtualFile projectRoot = finalProject.getBaseDir();
                            if (projectRoot != null) {
                                // 递归查找所有Java文件
                                List<VirtualFile> javaFiles = new ArrayList<>();
                                FileFinderService.findJavaFiles(projectRoot, javaFiles);
                                
                                // 遍历所有Java文件，查找实现了该接口的类
                                for (VirtualFile file : javaFiles) {
                                    // 跳过当前文件（接口文件本身）
                                    if (file.equals(finalCurrentFile)) {
                                        continue;
                                    }
                                    
                                    // 检查文件是否实现了该接口
                                    PsiFile implementationPsiFile = PsiManager.getInstance(finalProject).findFile(file);
                                    if (implementationPsiFile != null) {
                                        for (PsiElement element : implementationPsiFile.getChildren()) {
                                            if (element instanceof PsiClass) {
                                                PsiClass psiClass = (PsiClass) element;
                                                // 检查是否实现了该接口
                                                for (PsiClass implementedInterface : psiClass.getInterfaces()) {
                                                    String implementedInterfaceName = implementedInterface.getName();
                                                    // 确保精确匹配接口名称
                                                    if (finalInterfaceName.equals(implementedInterfaceName)) {
                                                        implementationFiles.add(file);
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            System.out.println("[Test Case Generator] Error finding implementation classes: " + e.getMessage());
                            e.printStackTrace();
                        }
                        
                        // 对于每个实现类，插入空方法代码
                        for (VirtualFile implementationFile : implementationFiles) {
                            try {
                                // 获取实现类的文档
                                Document implementationDocument = null;
                                // 查找是否有打开的编辑器
                                for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
                                    if (editor.getProject() == finalProject && editor.getVirtualFile() != null && editor.getVirtualFile().equals(implementationFile)) {
                                        implementationDocument = editor.getDocument();
                                        break;
                                    }
                                }
                                
                                // 如果没有打开的编辑器，从文件中读取
                                if (implementationDocument == null) {
                                    try {
                                        String content = new String(implementationFile.contentsToByteArray(), "UTF-8");
                                        implementationDocument = EditorFactory.getInstance().createDocument(content);
                                    } catch (Exception e) {
                                        System.out.println("[Test Case Generator] Error reading implementation file: " + e.getMessage());
                                        e.printStackTrace();
                                        continue;
                                    }
                                }
                                
                                if (implementationDocument != null) {
                                    String implementationText = implementationDocument.getText();
                                    
                                    // 找到类体的结束位置
                                    int classEndIndex = -1;
                                    int openBraces = 0;
                                    boolean inClass = false;
                                    for (int i = 0; i < implementationText.length(); i++) {
                                        char c = implementationText.charAt(i);
                                        if (c == '{') {
                                            openBraces++;
                                            inClass = true;
                                        } else if (c == '}') {
                                            openBraces--;
                                            if (inClass && openBraces == 0) {
                                                classEndIndex = i;
                                                break;
                                            }
                                        }
                                    }
                                    
                                    if (classEndIndex != -1) {
                                        // 在类体末尾插入空方法代码
                                        String implementationCodeToInsert = "\n\n    " + finalEmptyMethodCode + "\n";
                                        implementationDocument.insertString(classEndIndex, implementationCodeToInsert);
                                        
                                        // 如果是从文件中读取的，写回文件
                                        if (implementationFile != null) {
                                            try {
                                                implementationFile.setBinaryContent(implementationDocument.getText().getBytes("UTF-8"));
                                            } catch (Exception e) {
                                                System.out.println("[Test Case Generator] Error writing to implementation file: " + e.getMessage());
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                System.out.println("[Test Case Generator] Error inserting method into implementation class: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    } else {
                        // 对于非接口类，直接插入空方法代码到选中文本的正下方（下一行）
                        currentDocument.insertString(insertionPoint, "\n\n" + finalEmptyMethodCode);
                    }
                    
                    // 显示成功消息
                    SwingUtilities.invokeLater(() -> {
                        if (finalIsInterface) {
                            // 构建实现类信息
                            StringBuilder implementationInfo = new StringBuilder();
                            if (!implementationFiles.isEmpty()) {
                                implementationInfo.append("\n\nImplementation classes:");
                                for (VirtualFile file : implementationFiles) {
                                    implementationInfo.append("\n- " + file.getName() + " (" + file.getPath() + ")");
                                }
                            }
                            
                            JOptionPane.showMessageDialog(window, "Method declaration inserted successfully to interface!\n\nSelected text location:\nFile: " + finalCurrentFile.getName() + "\nPath: " + finalCurrentFile.getPath() + "\nLine: " + (lineNumber + 1) + "\n\nFile type: Interface class" + implementationInfo.toString(), "Success", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(window, "Empty method code inserted successfully!\n\nSelected text location:\nFile: " + finalCurrentFile.getName() + "\nPath: " + finalCurrentFile.getPath() + "\nLine: " + (lineNumber + 1) + "\n\nFile type: Regular class", "Success", JOptionPane.INFORMATION_MESSAGE);
                        }
                    });
                } catch (Exception e) {
                    System.out.println("[Test Case Generator] Error inserting empty method: " + e.getMessage());
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(window, "Error inserting empty method: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    });
                }
            });
        } catch (Exception e) {
            System.out.println("[Test Case Generator] Error in WriteCommandAction: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(window, "Error inserting empty method: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 插入代码到文件
     * @param code 要插入的代码
     * @param window 父窗口
     */
    public static void insertCodeToFile(String code, JComponent window) {
        // 获取当前项目和编辑器
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        Project project = projects.length > 0 ? projects[0] : null;
        
        if (project == null) {
            JOptionPane.showMessageDialog(window, "No open project found", "Error", JOptionPane.ERROR_MESSAGE);
            return;
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
            JOptionPane.showMessageDialog(window, "No active editor found", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // 创建final副本
        final Editor finalEditor = currentEditor;
        final String finalCode = code;
        final VirtualFile finalCurrentFile = currentEditor.getVirtualFile();
        
        // 插入代码到文件
        try {
            // 使用WriteCommandAction来修改文档，支持撤销操作
            WriteCommandAction.runWriteCommandAction(project, () -> {
                try {
                    Document currentDocument = finalEditor.getDocument();
                    String currentText = currentDocument.getText();
                    
                    // 找到需求注释的位置
                    // 假设需求注释是以特定格式标记的，例如以 "// 需求:" 或 "/* 需求:" 开头
                    int insertionPoint = findInsertionPoint(currentText);
                    
                    if (insertionPoint == -1) {
                        // 如果没有找到需求注释，默认插入到文件末尾
                        insertionPoint = currentText.length();
                    }
                    
                    // 插入代码
                    currentDocument.insertString(insertionPoint, "\n\n" + finalCode);
                    
                    // 显示成功消息
                    SwingUtilities.invokeLater(() -> {
                        if (finalCurrentFile != null) {
                            JOptionPane.showMessageDialog(window, "Code inserted successfully!\n\nFile: " + finalCurrentFile.getName() + "\nPath: " + finalCurrentFile.getPath(), "Success", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(window, "Code inserted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        }
                    });
                } catch (Exception e) {
                    System.out.println("[Test Case Generator] Error inserting code: " + e.getMessage());
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(window, "Error inserting code: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    });
                }
            });
        } catch (Exception e) {
            System.out.println("[Test Case Generator] Error in WriteCommandAction: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(window, "Error inserting code: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 查找插入点（需求注释的末尾）
     * @param text 文件内容
     * @return 插入点位置
     */
    private static int findInsertionPoint(String text) {
        // 查找需求注释的标记
        String[] markers = {
            "// 需求:",
            "/* 需求:",
            "// Requirement:",
            "/* Requirement:"
        };
        
        for (String marker : markers) {
            int index = text.indexOf(marker);
            if (index != -1) {
                System.out.println("[Test Case Generator] Found marker: " + marker + " at position: " + index);
                // 找到注释的末尾
                // 对于单行注释，找到下一个换行符
                if (marker.startsWith("//")) {
                    int endOfLine = text.indexOf("\n", index);
                    if (endOfLine != -1) {
                        System.out.println("[Test Case Generator] Found end of line at position: " + endOfLine);
                        return endOfLine;
                    }
                }
                // 对于多行注释，找到注释结束标记
                else if (marker.startsWith("/*")) {
                    int endOfComment = text.indexOf("*/", index);
                    if (endOfComment != -1) {
                        System.out.println("[Test Case Generator] Found end of comment at position: " + (endOfComment + 2));
                        return endOfComment + 2;
                    }
                }
            }
        }
        
        return -1; // 没有找到需求注释
    }
}
