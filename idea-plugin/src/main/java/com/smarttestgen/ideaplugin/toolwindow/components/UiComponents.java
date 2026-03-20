package com.smarttestgen.ideaplugin.dialog;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * UI 组件管理器，负责 UI 组件的创建和管理
 */
public class UiComponents {

    /**
     * 创建Java代码编辑器
     * @param project 项目
     * @return 编辑器实例
     */
    public static Editor createJavaEditor(Project project) {
        EditorFactory editorFactory = EditorFactory.getInstance();
        Document document = editorFactory.createDocument("Click 'Generate Test Code' button to generate test code");
        Editor editor = editorFactory.createEditor(document, project);
        return editor;
    }

    /**
     * 添加带来源标记的文本框
     * @param panel 面板
     * @param label 标签
     * @param value 值
     * @param isGenerated 是否生成
     * @param methodNameField 方法名文本框引用
     * @param returnTypeField 返回类型文本框引用
     * @param expectationField 期望文本框引用
     */
    public static void addTextFieldWithSource(JPanel panel, String label, String value, boolean isGenerated, 
                                             JTextField methodNameField, JTextField returnTypeField, 
                                             JTextField expectationField) {
        JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rowPanel.add(new JLabel(label + ": "));
        JTextField textField = new JTextField(value, 30);
        rowPanel.add(textField);
        rowPanel.add(Box.createHorizontalStrut(10));
        rowPanel.add(new JLabel("(Generated: " + isGenerated + ")"));
        panel.add(rowPanel);
        
        // 保存文本框引用到成员变量
        if (label.equals("Method Name")) {
            if (methodNameField != null) {
                methodNameField.setText(value);
            }
        } else if (label.equals("Return Type")) {
            if (returnTypeField != null) {
                returnTypeField.setText(value);
            }
        } else if (label.equals("Expectations")) {
            if (expectationField != null) {
                expectationField.setText(value);
            }
        }
    }

    /**
     * 显示代码库结构对话框
     * @param structure 代码库结构
     */
    public static void showStructureDialog(String structure) {
        JDialog dialog = new JDialog();
        dialog.setTitle("代码库结构");
        dialog.setModal(true);
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(null);
        
        JPanel panel = new JPanel(new BorderLayout());
        
        JBTextArea textArea = new JBTextArea(structure);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JBScrollPane scrollPane = new JBScrollPane(textArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }

    /**
     * 显示原始结果对话框
     * @param rawResult 原始结果
     */
    public static void showRawResultDialog(String rawResult) {
        JDialog dialog = new JDialog();
        dialog.setTitle("Raw Result");
        dialog.setModal(true);
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(null);
        
        JPanel panel = new JPanel(new BorderLayout());
        
        JBTextArea textArea = new JBTextArea(Utils.JsonUtils.formatJson(rawResult));
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JBScrollPane scrollPane = new JBScrollPane(textArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        dialog.add(panel);
        dialog.setVisible(true);
    }

    /**
     * 创建加载对话框
     * @param parent 父窗口
     * @param message 显示的消息
     * @return 加载对话框
     */
    public static JDialog createLoadingDialog(Frame parent, String message) {
        JDialog dialog = new JDialog(parent, "Processing", false);
        dialog.setModal(false);
        dialog.setSize(300, 100);
        dialog.setLocationRelativeTo(parent);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE); // 禁止用户关闭
        dialog.setAlwaysOnTop(true); // 确保显示在最前面
        
        JPanel panel = new JPanel();
        panel.add(new JLabel(message));
        dialog.add(panel);
        
        return dialog;
    }

    /**
     * 创建加载对话框（使用默认消息）
     * @param parent 父窗口
     * @return 加载对话框
     */
    public static JDialog createLoadingDialog(Frame parent) {
        return createLoadingDialog(parent, "Processing...");
    }

    /**
     * 创建参数面板
     * @param parameters 参数信息列表
     * @param parameterNameFields 参数名称文本框列表
     * @param parameterTypeFields 参数类型文本框列表
     * @return 参数面板
     */
    public static JPanel createParametersPanel(List<Utils.ParameterInfo> parameters, 
                                             List<JTextField> parameterNameFields, 
                                             List<JTextField> parameterTypeFields) {
        JPanel paramsPanel = new JPanel();
        paramsPanel.setLayout(new BoxLayout(paramsPanel, BoxLayout.Y_AXIS));
        paramsPanel.setBorder(BorderFactory.createTitledBorder("Parameters"));
        
        if (!parameters.isEmpty()) {
            // 为每个参数创建一行显示
            for (Utils.ParameterInfo param : parameters) {
                JPanel paramRowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                
                // 参数名称标签
                paramRowPanel.add(new JLabel("Name:"));
                JTextField nameField = new JTextField(param.name, 20);
                nameField.setEditable(false);
                paramRowPanel.add(nameField);
                paramRowPanel.add(Box.createHorizontalStrut(10));
                
                // 参数类型标签
                paramRowPanel.add(new JLabel("Type:"));
                JTextField typeField = new JTextField(param.type, 15);
                typeField.setEditable(false);
                paramRowPanel.add(typeField);
                
                paramsPanel.add(paramRowPanel);
                
                // 保存文本框引用到成员变量
                parameterNameFields.add(nameField);
                parameterTypeFields.add(typeField);
            }
            
            // 添加生成状态标签
            JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            statusPanel.add(new JLabel("(Generated: true)"));
            paramsPanel.add(statusPanel);
        } else {
            // 创建无参数面板
            JPanel noParamsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            noParamsPanel.add(new JLabel("No parameters"));
            noParamsPanel.add(Box.createHorizontalStrut(10));
            noParamsPanel.add(new JLabel("(Generated: true)"));
            paramsPanel.add(noParamsPanel);
        }
        
        return paramsPanel;
    }

    /**
     * 创建期望面板
     * @param expectationsStr 期望信息字符串
     * @param expectationField 期望文本框引用
     * @return 期望面板
     */
    public static JPanel createExpectationsPanel(String expectationsStr, JTextField expectationField) {
        JPanel expectationsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        expectationsPanel.setBorder(BorderFactory.createTitledBorder("Expectations"));
        
        if (expectationField == null) {
            expectationField = new JTextField(50);
        }
        
        if (!expectationsStr.isEmpty()) {
            // 简单处理，实际项目中可能需要更复杂的解析
            expectationField.setText(expectationsStr.replaceAll("\"", ""));
        } else {
            expectationField.setText("No expectations");
        }
        
        expectationsPanel.add(expectationField);
        return expectationsPanel;
    }
}
