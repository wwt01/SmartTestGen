package com.smarttestgen.ideaplugin.service;

import com.smarttestgen.ideaplugin.model.FileLocationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件位置服务，负责获取当前编辑文件的位置信息
 */
public class FileLocationService {

    /**
     * 获取当前文件的位置信息
     * @param selectionEnd 选中文本的结束位置
     * @return 文件位置信息
     */
    public static FileLocationInfo getFileLocationInfo(int selectionEnd) {
        System.out.println("[Test Case Generator] Entering getFileLocationInfo()");
        
        FileLocationInfo info = new FileLocationInfo();
        
        // 获取当前项目
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        if (projects.length == 0) {
            System.out.println("[Test Case Generator] No open projects found");
            return info;
        }
        
        Project project = projects[0];
        System.out.println("[Test Case Generator] Using project: " + project.getName());
        
        // 获取当前编辑的文件，使用更直接的方式
        Editor currentEditor = null;
        try {
            // 尝试使用 FileEditorManager 获取当前活动的编辑器
            com.intellij.openapi.fileEditor.FileEditorManager fileEditorManager = 
                com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project);
            VirtualFile[] selectedFiles = fileEditorManager.getSelectedFiles();
            if (selectedFiles.length > 0) {
                VirtualFile selectedFile = selectedFiles[0];
                System.out.println("[Test Case Generator] Found selected file: " + selectedFile.getName() + " at " + selectedFile.getPath());
                
                // 更新文件信息
                info.setFilePath(selectedFile.getPath());
                info.setFileName(selectedFile.getName());
                
                // 获取选中文本所在行的行号
                if (selectionEnd != -1) {
                    // 尝试获取与该文件关联的编辑器
                    for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
                        if (editor.getVirtualFile() != null && editor.getVirtualFile().equals(selectedFile)) {
                            currentEditor = editor;
                            break;
                        }
                    }
                    
                    if (currentEditor != null) {
                        Document currentDocument = currentEditor.getDocument();
                        if (currentDocument != null) {
                            info.setLineNumber(currentDocument.getLineNumber(selectionEnd) + 1);
                            System.out.println("[Test Case Generator] Selected line number: " + info.getLineNumber());
                        }
                    }
                }
                
                // 检查是否是接口类
                ApplicationManager.getApplication().runReadAction(() -> {
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(selectedFile);
                    if (psiFile != null) {
                        System.out.println("[Test Case Generator] Found PSI file for: " + selectedFile.getName());
                        // 遍历 psiFile 的所有子元素，查找包含选中文本的 PsiClass 元素
                        for (PsiElement element : psiFile.getChildren()) {
                            if (element instanceof PsiClass) {
                                PsiClass psiClass = (PsiClass) element;
                                // 检查选中文本是否在这个类中
                                if (selectionEnd >= element.getTextOffset() && 
                                    selectionEnd <= element.getTextOffset() + element.getTextLength()) {
                                    info.setInterface(psiClass.isInterface());
                                    info.setClassName(psiClass.getName());
                                    System.out.println("[Test Case Generator] Found PSI class containing selection: " + psiClass.getName() + ", Is interface: " + info.isInterface());
                                    // 如果是接口类，查找实现类
                                    if (info.isInterface()) {
                                        List<String> implementationFiles = findImplementationClasses(project, psiClass.getName());
                                        info.setImplementationFiles(implementationFiles);
                                        System.out.println("[Test Case Generator] Found " + implementationFiles.size() + " implementation classes");
                                    }
                                    return;
                                }
                            }
                        }
                        // 如果没有找到包含选中文本的类，使用第一个找到的类
                        if (info.getClassName() == null || info.getClassName().isEmpty()) {
                            for (PsiElement element : psiFile.getChildren()) {
                                if (element instanceof PsiClass) {
                                    PsiClass psiClass = (PsiClass) element;
                                    info.setInterface(psiClass.isInterface());
                                    info.setClassName(psiClass.getName());
                                    System.out.println("[Test Case Generator] Using first PSI class: " + psiClass.getName() + ", Is interface: " + info.isInterface());
                                    // 如果是接口类，查找实现类
                                    if (info.isInterface()) {
                                        List<String> implementationFiles = findImplementationClasses(project, psiClass.getName());
                                        info.setImplementationFiles(implementationFiles);
                                        System.out.println("[Test Case Generator] Found " + implementationFiles.size() + " implementation classes");
                                    }
                                    return;
                                }
                            }
                        }
                    } else {
                        System.out.println("[Test Case Generator] No PSI file found for: " + selectedFile.getName());
                    }
                });
                
                System.out.println("[Test Case Generator] File location info: " + info.toString());
                System.out.println("[Test Case Generator] Exiting getFileLocationInfo()");
                return info;
            }
        } catch (Exception e) {
            System.out.println("[Test Case Generator] Error getting file from FileEditorManager: " + e.getMessage());
        }
        
        // 如果使用 FileEditorManager 失败，尝试遍历所有编辑器
        System.out.println("[Test Case Generator] Getting editor from all editors...");
        Editor[] editors = EditorFactory.getInstance().getAllEditors();
        System.out.println("[Test Case Generator] Found " + editors.length + " editors");
        
        for (Editor editor : editors) {
            if (editor.getProject() == project) {
                currentEditor = editor;
                System.out.println("[Test Case Generator] Found editor for project: " + project.getName());
                break;
            }
        }
        
        if (currentEditor == null) {
            System.out.println("[Test Case Generator] No active editor found");
            return info;
        }
        
        VirtualFile currentFile = currentEditor.getVirtualFile();
        if (currentFile == null) {
            System.out.println("[Test Case Generator] No active file found");
            return info;
        }
        
        System.out.println("[Test Case Generator] Found active file: " + currentFile.getName() + " at " + currentFile.getPath());
        
        // 更新文件信息
        info.setFilePath(currentFile.getPath());
        info.setFileName(currentFile.getName());
        
        // 获取选中文本所在行的行号
        Document currentDocument = currentEditor.getDocument();
        if (currentDocument != null && selectionEnd != -1) {
            info.setLineNumber(currentDocument.getLineNumber(selectionEnd) + 1);
            System.out.println("[Test Case Generator] Selected line number: " + info.getLineNumber());
        } else if (currentDocument != null) {
            // 如果没有选中文本，获取当前光标所在行的行号
            CaretModel caretModel = currentEditor.getCaretModel();
            int caretOffset = caretModel.getOffset();
            info.setLineNumber(currentDocument.getLineNumber(caretOffset) + 1);
            System.out.println("[Test Case Generator] Caret line number: " + info.getLineNumber());
        } else {
            System.out.println("[Test Case Generator] No document found");
        }
        
        // 检查是否是接口类
        ApplicationManager.getApplication().runReadAction(() -> {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(currentFile);
            if (psiFile != null) {
                System.out.println("[Test Case Generator] Found PSI file for: " + currentFile.getName());
                // 遍历 psiFile 的所有子元素，查找包含选中文本的 PsiClass 元素
                for (PsiElement element : psiFile.getChildren()) {
                    if (element instanceof PsiClass) {
                        PsiClass psiClass = (PsiClass) element;
                        // 检查选中文本是否在这个类中
                        if (selectionEnd >= element.getTextOffset() && 
                            selectionEnd <= element.getTextOffset() + element.getTextLength()) {
                            info.setInterface(psiClass.isInterface());
                            info.setClassName(psiClass.getName());
                            System.out.println("[Test Case Generator] Found PSI class containing selection: " + psiClass.getName() + ", Is interface: " + info.isInterface());
                            // 如果是接口类，查找实现类
                            if (info.isInterface()) {
                                List<String> implementationFiles = findImplementationClasses(project, psiClass.getName());
                                info.setImplementationFiles(implementationFiles);
                                System.out.println("[Test Case Generator] Found " + implementationFiles.size() + " implementation classes");
                            }
                            return;
                        }
                    }
                }
                // 如果没有找到包含选中文本的类，使用第一个找到的类
                if (info.getClassName() == null || info.getClassName().isEmpty()) {
                    for (PsiElement element : psiFile.getChildren()) {
                        if (element instanceof PsiClass) {
                            PsiClass psiClass = (PsiClass) element;
                            info.setInterface(psiClass.isInterface());
                            info.setClassName(psiClass.getName());
                            System.out.println("[Test Case Generator] Using first PSI class: " + psiClass.getName() + ", Is interface: " + info.isInterface());
                            // 如果是接口类，查找实现类
                            if (info.isInterface()) {
                                List<String> implementationFiles = findImplementationClasses(project, psiClass.getName());
                                info.setImplementationFiles(implementationFiles);
                                System.out.println("[Test Case Generator] Found " + implementationFiles.size() + " implementation classes");
                            }
                            return;
                        }
                    }
                }
            } else {
                System.out.println("[Test Case Generator] No PSI file found for: " + currentFile.getName());
            }
        });
        
        System.out.println("[Test Case Generator] File location info: " + info.toString());
        System.out.println("[Test Case Generator] Exiting getFileLocationInfo()");
        return info;
    }

    /**
     * 查找实现了指定接口的所有类
     * @param project 项目
     * @param interfaceName 接口名称
     * @return 实现类文件路径列表
     */
    public static List<String> findImplementationClasses(Project project, String interfaceName) {
        List<String> implementationFiles = new ArrayList<>();
        
        if (interfaceName == null) {
            return implementationFiles;
        }
        
        // 在 read-action 中安全地访问 PSI
        ApplicationManager.getApplication().runReadAction(() -> {
            try {
                // 获取项目根目录
                VirtualFile projectRoot = project.getBaseDir();
                if (projectRoot != null) {
                    // 递归查找所有Java文件
                    List<VirtualFile> javaFiles = new ArrayList<>();
                    FileFinderService.findJavaFiles(projectRoot, javaFiles);
                    
                    // 遍历所有Java文件，查找实现了该接口的类
                    for (VirtualFile file : javaFiles) {
                        // 检查文件是否实现了该接口
                        PsiFile implementationPsiFile = PsiManager.getInstance(project).findFile(file);
                        if (implementationPsiFile != null) {
                            for (PsiElement element : implementationPsiFile.getChildren()) {
                                if (element instanceof PsiClass) {
                                    PsiClass psiClass = (PsiClass) element;
                                    // 检查是否实现了该接口
                                    for (PsiClass implementedInterface : psiClass.getInterfaces()) {
                                        String implementedInterfaceName = implementedInterface.getName();
                                        // 确保精确匹配接口名称
                                        if (interfaceName.equals(implementedInterfaceName)) {
                                            implementationFiles.add(file.getPath());
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
        });
        
        return implementationFiles;
    }
}
