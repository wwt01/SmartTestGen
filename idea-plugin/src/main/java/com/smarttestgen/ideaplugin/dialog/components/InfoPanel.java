package com.smarttestgen.ideaplugin.dialog.components;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * 信息面板，显示选中文本的文件位置信息
 */
public class InfoPanel extends JPanel {
    
    private final JLabel fileNameLabel;
    private final JLabel filePathLabel;
    private final JLabel lineNumberLabel;
    private final JLabel fileTypeLabel;
    private final JPanel implementationPanel;
    
    /**
     * 构造方法
     */
    public InfoPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createTitledBorder("Selected Text Location"));
        
        // 创建标签
        fileNameLabel = new JLabel("File: Loading...");
        filePathLabel = new JLabel("Path: Loading...");
        lineNumberLabel = new JLabel("Line: Loading...");
        fileTypeLabel = new JLabel("Type: Loading...");
        
        // 添加标签到面板
        add(fileNameLabel);
        add(filePathLabel);
        add(lineNumberLabel);
        add(fileTypeLabel);
        
        // 实现类信息面板
        implementationPanel = new JPanel();
        implementationPanel.setLayout(new BoxLayout(implementationPanel, BoxLayout.Y_AXIS));
        implementationPanel.setVisible(false);
        add(implementationPanel);
    }
    
    /**
     * 更新文件信息
     * @param fileName 文件名
     * @param filePath 文件路径
     * @param lineNumber 行号
     * @param isInterface 是否为接口类
     * @param implementationFiles 实现类文件列表
     */
    public void updateFileInfo(String fileName, String filePath, int lineNumber, 
                               boolean isInterface, List<String> implementationFiles) {
        if (fileName != null && !fileName.isEmpty()) {
            fileNameLabel.setText("File: " + fileName);
            filePathLabel.setText("Path: " + filePath);
            lineNumberLabel.setText("Line: " + (lineNumber != -1 ? lineNumber : "N/A"));
            fileTypeLabel.setText("Type: " + (isInterface ? "Interface class" : "Regular class"));
            
            // 更新实现类信息
            implementationPanel.removeAll();
            if (isInterface && implementationFiles != null && !implementationFiles.isEmpty()) {
                implementationPanel.add(new JLabel("Implementation classes:"));
                for (String implFile : implementationFiles) {
                    String fileNameOnly = implFile.substring(implFile.lastIndexOf('\\') + 1);
                    implementationPanel.add(new JLabel("- " + fileNameOnly));
                }
                implementationPanel.setVisible(true);
            } else {
                implementationPanel.setVisible(false);
            }
            
            revalidate();
            repaint();
        }
    }
    
    /**
     * 显示加载中状态
     */
    public void showLoading() {
        fileNameLabel.setText("File: Loading...");
        filePathLabel.setText("Path: Loading...");
        lineNumberLabel.setText("Line: Loading...");
        fileTypeLabel.setText("Type: Loading...");
        implementationPanel.setVisible(false);
        
        revalidate();
        repaint();
    }
    
    /**
     * 显示错误信息
     * @param errorMessage 错误信息
     */
    public void showError(String errorMessage) {
        fileNameLabel.setText("File: Error");
        filePathLabel.setText("Path: " + errorMessage);
        lineNumberLabel.setText("Line: N/A");
        fileTypeLabel.setText("Type: Unknown");
        implementationPanel.setVisible(false);
        
        revalidate();
        repaint();
    }
}
