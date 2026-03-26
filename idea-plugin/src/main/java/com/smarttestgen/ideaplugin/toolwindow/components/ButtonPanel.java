package com.smarttestgen.ideaplugin.toolwindow.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * 按钮面板，包含所有操作按钮
 */
public class ButtonPanel extends JPanel {
    
    /** 查看代码结构按钮 */
    private final JButton viewStructureButton;
    /** 查看原始返回结果按钮 */
    private final JButton viewRawResultButton;
    /** 生成测试代码按钮 */
    private final JButton generateTestButton;
    /** 预编译按钮 */
    private final JButton precompileButton;
    /** 修复编译错误按钮 */
    private final JButton fixCompilationButton;
    /** 插入空方法按钮 */
    private final JButton insertEmptyMethodButton;
    /** 插入到文件按钮 */
    private final JButton insertToFileButton;
    /** 创建测试文件按钮 */
    private final JButton createTestFileButton;
    
    /**
     * 构造方法
     */
    public ButtonPanel() {
        setLayout(new GridLayout(2, 1, 5, 5));
        
        JPanel firstRowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel secondRowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        viewStructureButton = new JButton("查看代码结构");
        firstRowPanel.add(viewStructureButton);
        
        viewRawResultButton = new JButton("查看需求返回信息");
        firstRowPanel.add(viewRawResultButton);
        
        generateTestButton = new JButton("生成测试代码");
        firstRowPanel.add(generateTestButton);
        
        precompileButton = new JButton("Pre-compile");
        secondRowPanel.add(precompileButton);
        
        fixCompilationButton = new JButton("Fix Compilation Error");
        secondRowPanel.add(fixCompilationButton);
        
        insertEmptyMethodButton = new JButton("Insert Empty Method");
        secondRowPanel.add(insertEmptyMethodButton);
        
        insertToFileButton = new JButton("Insert to File");
        secondRowPanel.add(insertToFileButton);
        
        createTestFileButton = new JButton("Create Test File");
        secondRowPanel.add(createTestFileButton);
        
        add(firstRowPanel);
        add(secondRowPanel);
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
    
    public void setGenerateTestButtonEnabled(boolean enabled) {
        generateTestButton.setEnabled(enabled);
    }
}
