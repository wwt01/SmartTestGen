package com.smarttestgen.ideaplugin.service;

import com.smarttestgen.ideaplugin.model.CodeStructureInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 代码结构服务，负责解析项目代码结构
 */
public class CodeStructureService {

    /**
     * 解析项目代码结构（异步）
     * @param onComplete 完成回调，接收解析结果
     */
    public static void parseCodeStructureAsync(Consumer<CodeStructureInfo> onComplete) {
        System.out.println("[Test Case Generator] Starting to parse code structure...");
        
        CompletableFuture.runAsync(() -> {
            CodeStructureInfo structureInfo = parseCodeStructure();
            if (onComplete != null) {
                onComplete.accept(structureInfo);
            }
        });
    }

    /**
     * 解析项目代码结构（同步）
     * @return 代码结构信息
     */
    public static CodeStructureInfo parseCodeStructure() {
        System.out.println("[Test Case Generator] Parsing code structure...");
        
        CodeStructureInfo structureInfo = new CodeStructureInfo();
        
        try {
            System.out.println("[Test Case Generator] Getting open projects...");
            // 获取当前项目
            Project[] projects = ProjectManager.getInstance().getOpenProjects();
            if (projects.length == 0) {
                System.out.println("[Test Case Generator] No open projects found");
                return structureInfo;
            }
            
            Project project = projects[0];
            structureInfo.setProjectName(project.getName());
            
            System.out.println("[Test Case Generator] Project name: " + project.getName());
            System.out.println("[Test Case Generator] Project base path: " + project.getBasePath());
            
            // 在后台线程中使用 readAction 安全地访问 PSI
            ApplicationManager.getApplication().runReadAction(() -> {
                try {
                    System.out.println("[Test Case Generator] Getting Java files...");
                    
                    // 获取项目根目录
                    VirtualFile projectRoot = project.getBaseDir();
                    System.out.println("[Test Case Generator] Project root: " + projectRoot.getPath());
                    
                    // 递归查找所有Java文件
                    List<VirtualFile> javaFiles = new ArrayList<>();
                    FileFinderService.findJavaFiles(projectRoot, javaFiles);
                    
                    System.out.println("[Test Case Generator] Found " + javaFiles.size() + " Java files");
                    
                    // 遍历所有 Java 文件
                    int classCount = 0;
                    for (VirtualFile virtualFile : javaFiles) {
                        System.out.println("[Test Case Generator] Processing file: " + virtualFile.getPath());
                        
                        // 将 VirtualFile 转换为 PsiFile
                        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                        if (psiFile != null) {
                            // 获取文件中的所有类
                            for (PsiElement element : psiFile.getChildren()) {
                                if (element instanceof PsiClass) {
                                    PsiClass psiClass = (PsiClass) element;
                                    CodeStructureInfo.ClassInfo classInfo = new CodeStructureInfo.ClassInfo();
                                    classInfo.setName(psiClass.getQualifiedName());
                                    
                                    // 获取类的方法
                                    PsiMethod[] methods = psiClass.getMethods();
                                    for (PsiMethod method : methods) {
                                        CodeStructureInfo.MethodInfo methodInfo = new CodeStructureInfo.MethodInfo();
                                        methodInfo.setName(method.getName());
                                        methodInfo.setReturnType(method.getReturnType() != null ? 
                                            method.getReturnType().getPresentableText() : "void");
                                        
                                        // 获取方法参数
                                        PsiParameter[] parameters = method.getParameterList().getParameters();
                                        for (PsiParameter parameter : parameters) {
                                            CodeStructureInfo.ParameterInfo paramInfo = new CodeStructureInfo.ParameterInfo(
                                                parameter.getName(),
                                                parameter.getType().getPresentableText()
                                            );
                                            methodInfo.addParameter(paramInfo);
                                        }
                                        
                                        classInfo.addMethod(methodInfo);
                                    }
                                    
                                    structureInfo.addClass(classInfo);
                                    classCount++;
                                }
                            }
                        }
                    }
                    
                    System.out.println("[Test Case Generator] Found " + classCount + " classes");
                    
                } catch (Exception e) {
                    System.out.println("[Test Case Generator] Error parsing code structure: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            
        } catch (Exception e) {
            System.out.println("[Test Case Generator] Error in parseCodeStructure: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("[Test Case Generator] Code structure parsing completed. Found " + 
            structureInfo.getClasses().size() + " classes");
        return structureInfo;
    }
    
    /**
     * 将代码结构信息转换为字符串
     * @param structureInfo 代码结构信息
     * @return 格式化的字符串
     */
    public static String toFormattedString(CodeStructureInfo structureInfo) {
        if (structureInfo == null) {
            return "代码库结构：\n\n没有找到 Java 类";
        }
        return structureInfo.toFormattedString();
    }
}
