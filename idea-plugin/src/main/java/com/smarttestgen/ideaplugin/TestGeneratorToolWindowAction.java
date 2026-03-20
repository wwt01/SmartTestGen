package com.smarttestgen.ideaplugin;

import com.smarttestgen.ideaplugin.service.api.ApiService;
import com.smarttestgen.ideaplugin.toolwindow.TestGeneratorToolWindowPanel;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.concurrent.CompletableFuture;

/**
 * 测试代码生成器 ToolWindow 动作类
 * 用于唤醒侧边栏并执行完整的测试代码生成流程
 */
public class TestGeneratorToolWindowAction extends AnAction {
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        System.out.println("[Test Case Generator] TestGeneratorToolWindowAction triggered");
        
        // 获取当前项目
        Project project = e.getProject();
        if (project == null) {
            System.out.println("[Test Case Generator] No project found");
            return;
        }
        
        // 获取当前编辑器
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            System.out.println("[Test Case Generator] No editor found");
            Messages.showInfoMessage("No editor found", "Information");
            return;
        }
        
        // 获取选择模型
        SelectionModel selectionModel = editor.getSelectionModel();
        String selectedText = selectionModel.getSelectedText();
        
        if (selectedText == null || selectedText.isEmpty()) {
            System.out.println("[Test Case Generator] No text selected");
            Messages.showInfoMessage("No text selected", "Information");
            return;
        }

        // 获取选择范围（在EDT线程中）
        final int startOffset = selectionModel.getSelectionStart();
        final int endOffset = selectionModel.getSelectionEnd();
        
        System.out.println("[Test Case Generator] Project found: " + project.getName());
        
        // 显示加载中提示
        final JDialog loadingDialog = createLoadingDialog();
        SwingUtilities.invokeLater(() -> loadingDialog.setVisible(true));
        
        // 异步调用后端接口
        CompletableFuture.runAsync(() -> {
            try {
                System.out.println("[Test Case Generator] Starting async processing");
                
                // 调用 需求文本结构化信息提取API服务处理文本
                String responseBody = ApiService.processText(selectedText);
                System.out.println("[Test Case Generator] Response received: " + responseBody);

                // 在EDT线程中更新UI
                SwingUtilities.invokeLater(() -> {
                    loadingDialog.dispose();
                    
                    // 获取ToolWindow管理器
                    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
                    ToolWindow toolWindow = toolWindowManager.getToolWindow("TestGeneratorToolWindow");
                    
                    System.out.println("[Test Case Generator] ToolWindow: " + (toolWindow != null ? "found" : "not found"));
                    
                    // 如果ToolWindow不存在，动态创建
                    if (toolWindow == null) {
                        System.out.println("[Test Case Generator] Creating ToolWindow dynamically");
                        toolWindow = toolWindowManager.registerToolWindow("TestGeneratorToolWindow", true, ToolWindowAnchor.RIGHT, project);
                        
                        // 创建面板
                        TestGeneratorToolWindowPanel panel = new TestGeneratorToolWindowPanel(project);
                        
                        // 创建内容并添加到 ToolWindow
                        ContentFactory contentFactory = ContentFactory.getInstance();
                        Content content = contentFactory.createContent(panel, "测试代码生成器", false);
                        toolWindow.getContentManager().addContent(content);
                        
                        System.out.println("[Test Case Generator] ToolWindow created dynamically");
                    } else {
                        System.out.println("[Test Case Generator] ToolWindow already exists, checking content");
                        
                        // 检查Content是否存在
                        Content existingContent = toolWindow.getContentManager().getContent(0);
                        if (existingContent == null) {
                            System.out.println("[Test Case Generator] Content is null, recreating panel");
                            
                            // 创建新面板
                            TestGeneratorToolWindowPanel panel = new TestGeneratorToolWindowPanel(project);
                            
                            // 创建内容并添加到 ToolWindow
                            ContentFactory contentFactory = ContentFactory.getInstance();
                            Content content = contentFactory.createContent(panel, "测试代码生成器", false);
                            toolWindow.getContentManager().addContent(content);
                        } else {
                            System.out.println("[Test Case Generator] Content exists, reusing it");
                        }
                    }
                    
                    // 获取面板并设置数据
                    Content content = toolWindow.getContentManager().getContent(0);
                    if (content != null) {
                        TestGeneratorToolWindowPanel panel = (TestGeneratorToolWindowPanel) content.getComponent();
                        panel.setData(responseBody, startOffset, endOffset, selectedText);
                    }
                    
                    // 显示 ToolWindow
                    toolWindow.show();
                    System.out.println("[Test Case Generator] ToolWindow shown successfully with data");
                });
            } catch (Exception ex) {
                System.out.println("[Test Case Generator] Exception occurred: " + ex.getMessage());
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    loadingDialog.dispose();
                    Messages.showErrorDialog("Error processing text: " + ex.getMessage(), "Error");
                });
            }
        });
    }
    
    private static JDialog createLoadingDialog() {
        JDialog dialog = new JDialog();
        dialog.setTitle("Processing");
        dialog.setModal(false);
        dialog.setSize(300, 100);
        dialog.setLocationRelativeTo(null);
        
        JPanel panel = new JPanel();
        panel.add(new JLabel("Processing selected text..."));
        dialog.add(panel);
        
        return dialog;
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // 只有在编辑器中有文本选择时才启用此动作
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        boolean enabled = editor != null && editor.getSelectionModel().hasSelection();
        e.getPresentation().setEnabled(enabled);
    }
}
