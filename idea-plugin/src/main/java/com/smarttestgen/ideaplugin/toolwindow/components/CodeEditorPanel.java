package com.smarttestgen.ideaplugin.dialog.components;

import com.smarttestgen.ideaplugin.service.ThreadPoolService;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import java.awt.*;

/**
 * 代码编辑器面板，显示生成的测试代码和空方法代码
 */
public class CodeEditorPanel extends JPanel {
    
    private final Editor testCodeEditor;
    private final Editor emptyMethodEditor;
    private final CardLayout cardLayout;
    private final JPanel codeCardPanel;
    private final JToggleButton toggleButton;
    
    /**
     * 构造方法
     * @param project 当前项目
     */
    public CodeEditorPanel(Project project) {
        setLayout(new BorderLayout());
        
        // 创建卡片布局面板
        cardLayout = new CardLayout();
        codeCardPanel = new JPanel(cardLayout);
        
        // 测试代码面板
        JPanel testCodePanel = new JPanel(new BorderLayout());
        testCodePanel.setBorder(BorderFactory.createTitledBorder("Generated Test Code"));
        
        // 空方法面板
        JPanel emptyMethodPanel = new JPanel(new BorderLayout());
        emptyMethodPanel.setBorder(BorderFactory.createTitledBorder("Generated Empty Method"));
        
        // 创建编辑器
        testCodeEditor = createJavaEditor(project);
        emptyMethodEditor = createJavaEditor(project);
        
        // 添加测试代码编辑器到面板
        if (testCodeEditor != null) {
            JComponent testEditorComponent = testCodeEditor.getComponent();
            JBScrollPane testScrollPane = new JBScrollPane(testEditorComponent);
            testScrollPane.setPreferredSize(new Dimension(800, 250));
            testCodePanel.add(testScrollPane, BorderLayout.CENTER);
        }
        
        // 添加空方法编辑器到面板
        if (emptyMethodEditor != null) {
            JComponent emptyMethodEditorComponent = emptyMethodEditor.getComponent();
            JBScrollPane emptyMethodScrollPane = new JBScrollPane(emptyMethodEditorComponent);
            emptyMethodScrollPane.setPreferredSize(new Dimension(800, 250));
            emptyMethodPanel.add(emptyMethodScrollPane, BorderLayout.CENTER);
        }
        
        // 添加面板到卡片布局
        codeCardPanel.add(testCodePanel, "TestCode");
        codeCardPanel.add(emptyMethodPanel, "EmptyMethod");
        
        // 创建切换按钮
        toggleButton = new JToggleButton("Show Test Code");
        toggleButton.setSelected(true);
        toggleButton.addActionListener(e -> {
            if (toggleButton.isSelected()) {
                cardLayout.show(codeCardPanel, "TestCode");
                toggleButton.setText("Show Test Code");
            } else {
                cardLayout.show(codeCardPanel, "EmptyMethod");
                toggleButton.setText("Show Empty Method");
            }
        });
        
        add(codeCardPanel, BorderLayout.CENTER);
    }
    
    /**
     * 创建 Java 代码编辑器
     * @param project 当前项目
     * @return 编辑器实例
     */
    private Editor createJavaEditor(Project project) {
        if (project == null) {
            return null;
        }
        
        try {
            // 使用 EditorFactory 创建编辑器
            com.intellij.openapi.editor.EditorFactory editorFactory = 
                com.intellij.openapi.editor.EditorFactory.getInstance();
            com.intellij.openapi.editor.Document document = 
                editorFactory.createDocument("");
            
            // 获取 Java 文件类型
            com.intellij.openapi.fileTypes.FileType javaFileType = 
                com.intellij.openapi.fileTypes.FileTypeManager.getInstance().getFileTypeByExtension("java");
            
            return editorFactory.createEditor(document, project, javaFileType, false);
        } catch (Exception e) {
            System.out.println("[Test Case Generator] Error creating editor: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 获取测试代码编辑器
     * @return 测试代码编辑器
     */
    public Editor getTestCodeEditor() {
        return testCodeEditor;
    }
    
    /**
     * 获取空方法编辑器
     * @return 空方法编辑器
     */
    public Editor getEmptyMethodEditor() {
        return emptyMethodEditor;
    }
    
    /**
     * 获取切换按钮
     * @return 切换按钮
     */
    public JToggleButton getToggleButton() {
        return toggleButton;
    }
    
    /**
     * 设置测试代码
     * @param code 测试代码
     */
    public void setTestCode(String code) {
        if (testCodeEditor != null) {
            ThreadPoolService.getInstance().runInWriteAction(() -> {
                testCodeEditor.getDocument().setText(code != null ? code : "");
            });
        }
    }
    
    /**
     * 设置空方法代码
     * @param code 空方法代码
     */
    public void setEmptyMethodCode(String code) {
        if (emptyMethodEditor != null) {
            ThreadPoolService.getInstance().runInWriteAction(() -> {
                emptyMethodEditor.getDocument().setText(code != null ? code : "");
            });
        }
    }
    
    /**
     * 获取测试代码
     * @return 测试代码
     */
    public String getTestCode() {
        if (testCodeEditor != null) {
            return testCodeEditor.getDocument().getText();
        }
        return "";
    }
    
    /**
     * 获取空方法代码
     * @return 空方法代码
     */
    public String getEmptyMethodCode() {
        if (emptyMethodEditor != null) {
            return emptyMethodEditor.getDocument().getText();
        }
        return "";
    }
}
