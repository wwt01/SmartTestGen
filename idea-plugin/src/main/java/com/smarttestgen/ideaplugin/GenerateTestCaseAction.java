package com.smarttestgen.ideaplugin;

import com.smarttestgen.ideaplugin.dialog.ResultDialog;
import com.smarttestgen.ideaplugin.service.ApiService;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.concurrent.CompletableFuture;

public class GenerateTestCaseAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        System.out.println("[Test Case Generator] Action started");
        
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
        
        // 显示加载中提示
        final JDialog loadingDialog = createLoadingDialog();
        SwingUtilities.invokeLater(() -> loadingDialog.setVisible(true));
        
        // 异步调用后端接口
        CompletableFuture.runAsync(() -> {
            try {
                System.out.println("[Test Case Generator] Starting async processing");
                
                // 调用 需求文本结构化信息提取API服务处理文本
                String responseBody = ApiService.processText(selectedText);
                System.out.println("[Test Case Generator] Response received");

                // 处理响应
                System.out.println("[Test Case Generator] Response successful, showing result dialog");
                SwingUtilities.invokeLater(() -> {
                    loadingDialog.dispose();
                    new ResultDialog(responseBody, startOffset, endOffset).show();
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
    public void update(AnActionEvent e) {
        // 只有在编辑器中有文本选择时才启用此动作
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        boolean enabled = editor != null && editor.getSelectionModel().hasSelection();
        e.getPresentation().setEnabled(enabled);
    }
    
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // 返回EDT线程，因为update方法只涉及轻量级UI操作
        return ActionUpdateThread.EDT;
    }
}
