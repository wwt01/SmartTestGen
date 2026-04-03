package com.smarttestgen.ideaplugin.service.code;

import com.intellij.openapi.project.ProjectUtil;
import com.smarttestgen.ideaplugin.model.ClassContextInfo;
import com.smarttestgen.ideaplugin.model.CodeStructureInfo;
import com.smarttestgen.ideaplugin.model.FieldInfo;
import com.smarttestgen.ideaplugin.model.MethodInfo;
import com.smarttestgen.ideaplugin.model.ParameterInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.smarttestgen.ideaplugin.service.file.FileFinderService;
import com.smarttestgen.ideaplugin.util.LogUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        LogUtil.section("解析代码结构");
        
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
        LogUtil.info("Parser", "开始解析代码结构...");
        
        CodeStructureInfo structureInfo = new CodeStructureInfo();
        
        try {
            Project[] projects = ProjectManager.getInstance().getOpenProjects();
            if (projects.length == 0) {
                LogUtil.warn("Parser", "未找到打开的项目");
                return structureInfo;
            }
            
            Project project = projects[0];
            structureInfo.setProjectName(project.getName());
            
            LogUtil.info("Parser", "项目: " + project.getName());
            
            ApplicationManager.getApplication().runReadAction(() -> {
                try {
                    VirtualFile projectRoot = ProjectUtil.guessProjectDir(project);
                    
                    List<VirtualFile> javaFiles = new ArrayList<>();
                    FileFinderService.findJavaFiles(projectRoot, javaFiles);
                    
                    LogUtil.info("Parser", "找到 " + javaFiles.size() + " 个Java文件");
                    
                    int classCount = 0;
                    for (VirtualFile virtualFile : javaFiles) {
                        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                        if (psiFile != null) {
                            for (PsiElement element : psiFile.getChildren()) {
                                if (element instanceof PsiClass) {
                                    CodeStructureInfo.ClassInfo classInfo = getClassInfo((PsiClass) element);

                                    structureInfo.addClass(classInfo);
                                    classCount++;
                                }
                            }
                        }
                    }
                    
                    LogUtil.success("Parser", "解析完成: " + classCount + " 个类");
                    
                } catch (Exception e) {
                    LogUtil.error("Parser", "解析失败: " + e.getMessage());
                }
            });
            
        } catch (Exception e) {
            LogUtil.error("Parser", "解析异常: " + e.getMessage());
        }
        
        return structureInfo;
    }

    private static CodeStructureInfo.@NotNull ClassInfo getClassInfo(PsiClass psiClass) {
        CodeStructureInfo.ClassInfo classInfo = new CodeStructureInfo.ClassInfo();
        classInfo.setName(psiClass.getQualifiedName());

        PsiMethod[] methods = psiClass.getMethods();
        for (PsiMethod method : methods) {
            CodeStructureInfo.MethodInfo methodInfo = new CodeStructureInfo.MethodInfo();
            methodInfo.setName(method.getName());
            methodInfo.setReturnType(method.getReturnType() != null ?
                method.getReturnType().getPresentableText() : "void");

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
        return classInfo;
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

    /**
     * 获取当前类的完整上下文信息
     * @return 当前类上下文信息，如果获取失败返回null
     */
    public static ClassContextInfo getCurrentClassInfo() {
        LogUtil.section("获取类信息");
        
        try {
            Project[] projects = ProjectManager.getInstance().getOpenProjects();
            if (projects.length == 0) {
                LogUtil.warn("ClassInfo", "未找到打开的项目");
                return null;
            }
            
            Project project = projects[0];
            
            final ClassContextInfo[] result = new ClassContextInfo[1];
            
            ApplicationManager.getApplication().runReadAction(() -> {
                try {
                    Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
                    if (editor == null) {
                        LogUtil.warn("ClassInfo", "未找到编辑器");
                        return;
                    }
                    
                    VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(editor.getDocument());
                    if (virtualFile == null) {
                        LogUtil.warn("ClassInfo", "未找到虚拟文件");
                        return;
                    }
                    
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                    if (psiFile == null) {
                        LogUtil.warn("ClassInfo", "未找到 PSI 文件");
                        return;
                    }
                    
                    PsiClass psiClass = null;
                    for (PsiElement element : psiFile.getChildren()) {
                        if (element instanceof PsiClass) {
                            psiClass = (PsiClass) element;
                            break;
                        }
                    }
                    
                    if (psiClass == null) {
                        LogUtil.warn("ClassInfo", "文件中未找到类");
                        return;
                    }
                    
                    result[0] = extractClassInfo(psiClass);
                    LogUtil.success("ClassInfo", "提取成功: " + result[0].getClassName());
                    
                } catch (Exception e) {
                    LogUtil.error("ClassInfo", "提取失败: " + e.getMessage());
                }
            });
            
            return result[0];
            
        } catch (Exception e) {
            LogUtil.error("ClassInfo", "获取异常: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * 从 PsiClass提取类信息
     * @param psiClass PSI 类对象
     * @return 类上下文信息
     */
    private static ClassContextInfo extractClassInfo(PsiClass psiClass) {
        ClassContextInfo info = new ClassContextInfo();
        
        String qualifiedName = psiClass.getQualifiedName();
        info.setQualifiedName(qualifiedName != null ? qualifiedName : "");
        info.setClassName(psiClass.getName() != null ? psiClass.getName() : "");
        info.setInterface(psiClass.isInterface());
        
        if (qualifiedName != null && qualifiedName.contains(".")) {
            info.setPackageName(qualifiedName.substring(0, qualifiedName.lastIndexOf(".")));
        }
        
        info.setClassType(detectClassType(psiClass.getName()));
        
        Set<String> dependencies = new HashSet<>();
        
        PsiField[] fields = psiClass.getFields();
        for (PsiField field : fields) {
            FieldInfo fieldInfo = new FieldInfo();
            fieldInfo.setName(field.getName());
            fieldInfo.setType(field.getType().getPresentableText());
            fieldInfo.setPrivate(field.getModifierList() != null && 
                field.getModifierList().hasModifierProperty("private"));
            info.getFields().add(fieldInfo);
            
            extractDependencyFromType(field.getType(), dependencies);
        }
        
        PsiMethod[] methods = psiClass.getMethods();
        for (PsiMethod method : methods) {
            MethodInfo methodInfo = new MethodInfo();
            methodInfo.setName(method.getName());
            methodInfo.setReturnType(method.getReturnType() != null ? 
                method.getReturnType().getPresentableText() : "void");
            
            PsiParameter[] parameters = method.getParameterList().getParameters();
            for (PsiParameter parameter : parameters) {
                ParameterInfo paramInfo = new ParameterInfo();
                paramInfo.setName(parameter.getName());
                paramInfo.setType(parameter.getType().getPresentableText());
                methodInfo.getParameters().add(paramInfo);
                
                extractDependencyFromType(parameter.getType(), dependencies);
            }
            
            info.getMethods().add(methodInfo);
        }
        
        for (String dep : dependencies) {
            if (!dep.startsWith("java.") && !dep.equals(qualifiedName)) {
                info.getDependencies().add(dep);
            }
        }
        
        return info;
    }

    /**
     * 从类型中提取依赖
     * @param type PSI类型
     * @param dependencies 依赖集合
     */
    private static void extractDependencyFromType(PsiType type, Set<String> dependencies) {
        if (type instanceof PsiClassType classType) {
            PsiClass resolvedClass = classType.resolve();
            if (resolvedClass != null && resolvedClass.getQualifiedName() != null) {
                dependencies.add(resolvedClass.getQualifiedName());
            }
        }
    }

    /**
     * 根据类名检测类类型
     * @param className 类名
     * @return 类类型
     */
    private static String detectClassType(String className) {
        if (className == null || className.isEmpty()) {
            return "Unknown";
        }
        
        if (className.endsWith("DTO")) return "DTO";
        if (className.endsWith("Entity")) return "Entity";
        if (className.endsWith("VO")) return "VO";
        if (className.endsWith("Service")) return "Service";
        if (className.endsWith("Controller")) return "Controller";
        if (className.endsWith("Repository")) return "Repository";
        if (className.endsWith("Mapper")) return "Mapper";
        if (className.endsWith("Util") || className.endsWith("Utils")) return "Util";
        if (className.endsWith("Config")) return "Config";
        if (className.endsWith("Exception")) return "Exception";
        
        return "Unknown";
    }
}
