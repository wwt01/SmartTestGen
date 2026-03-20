package com.smarttestgen.ideaplugin.dialog.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * 按钮面板，包含所有操作按钮
 */
public class ButtonPanel extends JPanel {
    
    private final JButton viewStructureButton;
    private final JButton viewRawResultButton;
    private final JButton generateTestButton;
    private final JButton precompileButton;
    private final JButton fixCompilationButton;
    private final JButton insertEmptyMethodButton;
    private final JButton insertToFileButton;
    private final JButton createTestFileButton;
    
    /**
     * 构造方法
     */
    public ButtonPanel() {
        setLayout(new FlowLayout(FlowLayout.RIGHT));
        
        // 查看代码结构按钮
        viewStructureButton = new JButton("查看代码结构");
        add(viewStructureButton);
        
        // 查看需求返回信息按钮
        viewRawResultButton = new JButton("查看需求返回信息");
        add(viewRawResultButton);
        
        // 生成测试代码按钮
        generateTestButton = new JButton("生成测试代码");
        add(generateTestButton);
        
        // 预编译按钮
        precompileButton = new JButton("Pre-compile");
        add(precompileButton);
        
        // 编译修复按钮
        fixCompilationButton = new JButton("Fix Compilation Error");
        add(fixCompilationButton);
        
        // 插入空方法按钮
        insertEmptyMethodButton = new JButton("Insert Empty Method");
        add(insertEmptyMethodButton);
        
        // 插入到文件按钮
        insertToFileButton = new JButton("Insert to File");
        add(insertToFileButton);
        
        // 创建测试文件按钮
        createTestFileButton = new JButton("Create Test File");
        add(createTestFileButton);
    }
    
    /**
     * 设置查看代码结构按钮的监听器
     * @param listener 监听器
     */
    public void setViewStructureListener(ActionListener listener) {
        viewStructureButton.addActionListener(listener);
    }
    
    /**
     * 设置查看需求返回信息按钮的监听器
     * @param listener 监听器
     */
    public void setViewRawResultListener(ActionListener listener) {
        viewRawResultButton.addActionListener(listener);
    }
    
    /**
     * 设置生成测试代码按钮的监听器
     * @param listener 监听器
     */
    public void setGenerateTestListener(ActionListener listener) {
        // 移除所有现有的监听器
        for (ActionListener al : generateTestButton.getActionListeners()) {
            generateTestButton.removeActionListener(al);
        }
        generateTestButton.addActionListener(listener);
    }
    
    /**
     * 设置预编译按钮的监听器
     * @param listener 监听器
     */
    public void setPrecompileListener(ActionListener listener) {
        // 移除所有现有的监听器
        for (ActionListener al : precompileButton.getActionListeners()) {
            precompileButton.removeActionListener(al);
        }
        precompileButton.addActionListener(listener);
    }
    
    /**
     * 设置编译修复按钮的监听器
     * @param listener 监听器
     */
    public void setFixCompilationListener(ActionListener listener) {
        // 移除所有现有的监听器
        for (ActionListener al : fixCompilationButton.getActionListeners()) {
            fixCompilationButton.removeActionListener(al);
        }
        fixCompilationButton.addActionListener(listener);
    }
    
    /**
     * 设置插入空方法按钮的监听器
     * @param listener 监听器
     */
    public void setInsertEmptyMethodListener(ActionListener listener) {
        // 移除所有现有的监听器
        for (ActionListener al : insertEmptyMethodButton.getActionListeners()) {
            insertEmptyMethodButton.removeActionListener(al);
        }
        insertEmptyMethodButton.addActionListener(listener);
    }
    
    /**
     * 设置插入到文件按钮的监听器
     * @param listener 监听器
     */
    public void setInsertToFileListener(ActionListener listener) {
        // 移除所有现有的监听器
        for (ActionListener al : insertToFileButton.getActionListeners()) {
            insertToFileButton.removeActionListener(al);
        }
        insertToFileButton.addActionListener(listener);
    }
    
    /**
     * 设置创建测试文件按钮的监听器
     * @param listener 监听器
     */
    public void setCreateTestFileListener(ActionListener listener) {
        // 移除所有现有的监听器
        for (ActionListener al : createTestFileButton.getActionListeners()) {
            createTestFileButton.removeActionListener(al);
        }
        createTestFileButton.addActionListener(listener);
    }
    
    /**
     * 启用或禁用所有按钮
     * @param enabled 是否启用
     */
    public void setAllButtonsEnabled(boolean enabled) {
        viewStructureButton.setEnabled(enabled);
        viewRawResultButton.setEnabled(enabled);
        generateTestButton.setEnabled(enabled);
        precompileButton.setEnabled(enabled);
        fixCompilationButton.setEnabled(enabled);
        insertEmptyMethodButton.setEnabled(enabled);
        insertToFileButton.setEnabled(enabled);
        createTestFileButton.setEnabled(enabled);
    }
    
    /**
     * 启用或禁用生成相关按钮
     * @param enabled 是否启用
     */
    public void setGenerationButtonsEnabled(boolean enabled) {
        generateTestButton.setEnabled(enabled);
        precompileButton.setEnabled(enabled);
        fixCompilationButton.setEnabled(enabled);
        insertEmptyMethodButton.setEnabled(enabled);
        insertToFileButton.setEnabled(enabled);
        createTestFileButton.setEnabled(enabled);
    }
}
