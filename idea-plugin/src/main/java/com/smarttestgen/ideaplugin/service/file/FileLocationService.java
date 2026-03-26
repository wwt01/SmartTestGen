package com.smarttestgen.ideaplugin.service.file;

import com.smarttestgen.ideaplugin.model.FileLocationInfo;
import com.smarttestgen.ideaplugin.util.LogUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class FileLocationService {

    public static FileLocationInfo getFileLocationInfo(int selectionEnd) {
        FileLocationInfo info = new FileLocationInfo();
        
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        if (projects.length == 0) {
            LogUtil.warn("FileLocation", "没有打开的项目");
            return info;
        }
        
        Project project = projects[0];
        
        try {
            com.intellij.openapi.fileEditor.FileEditorManager fileEditorManager = 
                com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project);
            VirtualFile[] selectedFiles = fileEditorManager.getSelectedFiles();
            
            if (selectedFiles.length > 0) {
                VirtualFile selectedFile = selectedFiles[0];
                info.setFilePath(selectedFile.getPath());
                info.setFileName(selectedFile.getName());
                
                if (selectionEnd != -1) {
                    for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
                        if (editor.getVirtualFile() != null && editor.getVirtualFile().equals(selectedFile)) {
                            Document doc = editor.getDocument();
                            if (doc != null) {
                                info.setLineNumber(doc.getLineNumber(selectionEnd) + 1);
                            }
                            break;
                        }
                    }
                }
                
                ApplicationManager.getApplication().runReadAction(() -> {
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(selectedFile);
                    if (psiFile != null) {
                        extractClassInfo(psiFile, info, selectionEnd, project);
                    }
                });
                
                LogUtil.info("FileLocation", "文件: " + selectedFile.getName() + 
                           ", 类: " + info.getClassName() + 
                           ", 接口: " + info.isInterface());
                return info;
            }
        } catch (Exception e) {
            LogUtil.error("FileLocation", "获取文件信息失败: " + e.getMessage());
        }
        
        Editor[] editors = EditorFactory.getInstance().getAllEditors();
        for (Editor editor : editors) {
            if (editor.getProject() == project) {
                VirtualFile currentFile = editor.getVirtualFile();
                if (currentFile != null) {
                    info.setFilePath(currentFile.getPath());
                    info.setFileName(currentFile.getName());
                    
                    Document doc = editor.getDocument();
                    if (doc != null) {
                        if (selectionEnd != -1) {
                            info.setLineNumber(doc.getLineNumber(selectionEnd) + 1);
                        } else {
                            CaretModel caretModel = editor.getCaretModel();
                            info.setLineNumber(doc.getLineNumber(caretModel.getOffset()) + 1);
                        }
                    }
                    
                    ApplicationManager.getApplication().runReadAction(() -> {
                        PsiFile psiFile = PsiManager.getInstance(project).findFile(currentFile);
                        if (psiFile != null) {
                            extractClassInfo(psiFile, info, selectionEnd, project);
                        }
                    });
                    
                    LogUtil.info("FileLocation", "文件: " + currentFile.getName() + 
                               ", 类: " + info.getClassName());
                    return info;
                }
                break;
            }
        }
        
        return info;
    }
    
    private static void extractClassInfo(PsiFile psiFile, FileLocationInfo info, int selectionEnd, Project project) {
        for (PsiElement element : psiFile.getChildren()) {
            if (element instanceof PsiClass) {
                PsiClass psiClass = (PsiClass) element;
                if (selectionEnd >= element.getTextOffset() && 
                    selectionEnd <= element.getTextOffset() + element.getTextLength()) {
                    info.setInterface(psiClass.isInterface());
                    info.setClassName(psiClass.getName());
                    if (info.isInterface()) {
                        findImplementationClassesAsync(project, psiClass.getName(), info::setImplementationFiles);
                    }
                    return;
                }
            }
        }
        
        if (info.getClassName() == null || info.getClassName().isEmpty()) {
            for (PsiElement element : psiFile.getChildren()) {
                if (element instanceof PsiClass) {
                    PsiClass psiClass = (PsiClass) element;
                    info.setInterface(psiClass.isInterface());
                    info.setClassName(psiClass.getName());
                    if (info.isInterface()) {
                        findImplementationClassesAsync(project, psiClass.getName(), info::setImplementationFiles);
                    }
                    return;
                }
            }
        }
    }

    public static void findImplementationClassesAsync(Project project, String interfaceName, Consumer<List<String>> callback) {
        if (interfaceName == null) {
            callback.accept(new ArrayList<>());
            return;
        }
        
        ExecutorService executor = Executors.newSingleThreadExecutor();
        ReadAction.nonBlocking(() -> {
            List<String> implementationFiles = new ArrayList<>();
            try {
                VirtualFile projectRoot = project.getBaseDir();
                if (projectRoot != null) {
                    List<VirtualFile> javaFiles = new ArrayList<>();
                    FileFinderService.findJavaFiles(projectRoot, javaFiles);
                    
                    for (VirtualFile file : javaFiles) {
                        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                        if (psiFile != null) {
                            for (PsiElement element : psiFile.getChildren()) {
                                if (element instanceof PsiClass) {
                                    PsiClass psiClass = (PsiClass) element;
                                    for (PsiClass implInterface : psiClass.getInterfaces()) {
                                        if (interfaceName.equals(implInterface.getName())) {
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
                LogUtil.error("FileLocation", "查找实现类失败: " + e.getMessage());
            }
            return implementationFiles;
        }).finishOnUiThread(com.intellij.openapi.application.ModalityState.defaultModalityState(), result -> {
            callback.accept(result);
            executor.shutdown();
        }).submit(executor);
    }
}
