package com.smarttestgen.ideaplugin.service;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;

/**
 * 文件内容服务类
 * 负责获取和操作文件内容
 */
public class FileContentService {
    
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
        Editor currentEditor = findEditorForProject(editors, project);
        
        if (currentEditor == null) {
            return "";
        }
        
        // 获取文件内容
        Document document = currentEditor.getDocument();
        return document.getText();
    }
    
    /**
     * 查找指定项目的编辑器
     * @param editors 所有编辑器
     * @param project 项目
     * @return 匹配的编辑器
     */
    private static Editor findEditorForProject(Editor[] editors, Project project) {
        for (Editor editor : editors) {
            if (editor.getProject() == project) {
                return editor;
            }
        }
        return null;
    }
}
