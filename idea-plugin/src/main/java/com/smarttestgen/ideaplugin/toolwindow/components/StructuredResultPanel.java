package com.smarttestgen.ideaplugin.toolwindow.components;

import com.smarttestgen.ideaplugin.dialog.UiComponents;
import com.smarttestgen.ideaplugin.dialog.components.ButtonPanel;
import com.smarttestgen.ideaplugin.util.Constants;
import com.smarttestgen.ideaplugin.util.JsonUtils;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 结构化结果面板
 * 显示需求的结构化解析结果
 */
public class StructuredResultPanel extends JPanel {
    private final ButtonPanel buttonPanel;
    private JTextField methodNameField;
    private JTextField returnTypeField;
    private JTextField expectationField;
    private List<JTextField> parameterNameFields = new ArrayList<>();
    private List<JTextField> parameterTypeFields = new ArrayList<>();
    private String codeStructure = "";
    private String rawResult = "";
    private String selectedText = "";
    private StructuredResultPanelListener listener;
    
    public interface StructuredResultPanelListener {
        void onGenerateTestCode();
        void onPrecompileCode();
        void onFixCompilationError();
        void onInsertEmptyMethod();
        void onInsertCodeToFile();
        void onCreateTestFile();
    }
    
    public StructuredResultPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Structured Result"));
        
        // 初始化按钮面板
        buttonPanel = new ButtonPanel();
        initButtonListeners();
        
        // 初始化UI
        updateComponents();
    }
    
    private void initButtonListeners() {
        buttonPanel.setViewStructureListener(e -> {
            if (codeStructure != null) {
                UiComponents.showStructureDialog(codeStructure);
            }
        });
        
        buttonPanel.setViewRawResultListener(e -> {
            if (rawResult != null) {
                UiComponents.showRawResultDialog(rawResult);
            }
        });
        
        buttonPanel.setGenerateTestListener(e -> {
            if (listener != null) {
                listener.onGenerateTestCode();
            }
        });
        
        buttonPanel.setPrecompileListener(e -> {
            if (listener != null) {
                listener.onPrecompileCode();
            }
        });
        
        buttonPanel.setFixCompilationListener(e -> {
            if (listener != null) {
                listener.onFixCompilationError();
            }
        });
        
        buttonPanel.setInsertEmptyMethodListener(e -> {
            if (listener != null) {
                listener.onInsertEmptyMethod();
            }
        });
        
        buttonPanel.setInsertToFileListener(e -> {
            if (listener != null) {
                listener.onInsertCodeToFile();
            }
        });
        
        buttonPanel.setCreateTestFileListener(e -> {
            if (listener != null) {
                listener.onCreateTestFile();
            }
        });
    }
    
    public void setListener(StructuredResultPanelListener listener) {
        this.listener = listener;
    }
    
    public void setData(String rawResult, String selectedText, String codeStructure) {
        this.rawResult = rawResult;
        this.selectedText = selectedText;
        this.codeStructure = codeStructure;
        updateComponents();
    }
    
    private void updateComponents() {
        removeAll();
        
        // 结构化结果内容面板
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        
        // 移除选中文本信息面板，为底部展示区腾出空间
        
        // 提取结构化结果
        String methodName = "";
        boolean isMethodNameGenerated = false;
        String returnType = "";
        boolean isReturnTypeGenerated = false;
        boolean isParametersGenerated = false;
        
        if (rawResult != null && !rawResult.isEmpty()) {
            methodName = JsonUtils.extractField(rawResult, Constants.RESPONSE_METHOD_NAME_FIELD);
            isMethodNameGenerated = JsonUtils.extractBooleanField(rawResult, Constants.RESPONSE_METHOD_NAME_FIELD, Constants.RESPONSE_IS_CONSTRUCTED_FIELD);
            returnType = JsonUtils.extractField(rawResult, Constants.RESPONSE_RETURN_TYPE_FIELD);
            isReturnTypeGenerated = JsonUtils.extractBooleanField(rawResult, Constants.RESPONSE_RETURN_TYPE_FIELD, Constants.RESPONSE_IS_CONSTRUCTED_FIELD);
            isParametersGenerated = JsonUtils.extractBooleanField(rawResult, Constants.RESPONSE_PARAMETERS_FIELD, Constants.RESPONSE_IS_CONSTRUCTED_FIELD);
        }
        
        // 方法名
        methodNameField = new JTextField(methodName, 30);
        JPanel methodNamePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        methodNamePanel.add(new JLabel("Method Name: "));
        methodNamePanel.add(methodNameField);
        methodNamePanel.add(Box.createHorizontalStrut(10));
        methodNamePanel.add(new JLabel("(Generated: " + isMethodNameGenerated + ")"));
        contentPanel.add(methodNamePanel);
        
        // 参数
        JPanel paramsPanel = createParametersPanel(isParametersGenerated);
        contentPanel.add(paramsPanel);
        
        // 返回类型
        returnTypeField = new JTextField(returnType, 30);
        JPanel returnTypePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        returnTypePanel.add(new JLabel("Return Type: "));
        returnTypePanel.add(returnTypeField);
        returnTypePanel.add(Box.createHorizontalStrut(10));
        returnTypePanel.add(new JLabel("(Generated: " + isReturnTypeGenerated + ")"));
        contentPanel.add(returnTypePanel);
        
        // 期望
        JPanel expectationsPanel = createExpectationsPanel();
        contentPanel.add(expectationsPanel);
        
        add(contentPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        revalidate();
        repaint();
    }
    
    private JPanel createParametersPanel(boolean isParametersGenerated) {
        JPanel paramsPanel = new JPanel();
        paramsPanel.setLayout(new BoxLayout(paramsPanel, BoxLayout.Y_AXIS));
        paramsPanel.setBorder(BorderFactory.createTitledBorder("Parameters"));
        
        // 清空之前的参数字段
        parameterNameFields.clear();
        parameterTypeFields.clear();
        
        if (rawResult != null && !rawResult.isEmpty()) {
            // 解析参数信息
            List<JsonUtils.ParameterInfo> parameters = JsonUtils.extractParameters(rawResult, Constants.RESPONSE_PARAMETERS_FIELD);
            
            if (!parameters.isEmpty()) {
                // 为每个参数创建一行显示
                for (JsonUtils.ParameterInfo param : parameters) {
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
                statusPanel.add(new JLabel("(Generated: " + isParametersGenerated + ")"));
                paramsPanel.add(statusPanel);
            } else {
                // 创建无参数面板
                JPanel noParamsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                noParamsPanel.add(new JLabel("No parameters"));
                noParamsPanel.add(Box.createHorizontalStrut(10));
                noParamsPanel.add(new JLabel("(Generated: " + isParametersGenerated + ")"));
                paramsPanel.add(noParamsPanel);
            }
        } else {
            // 创建无参数面板
            JPanel noParamsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            noParamsPanel.add(new JLabel("No parameters"));
            paramsPanel.add(noParamsPanel);
        }
        
        return paramsPanel;
    }
    
    private JPanel createExpectationsPanel() {
        JPanel expectationsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        expectationsPanel.setBorder(BorderFactory.createTitledBorder("Expectations"));
        expectationField = new JTextField(50);
        
        if (rawResult != null && !rawResult.isEmpty()) {
            // 提取期望信息
            String expectationsStr = JsonUtils.extractArrayField(rawResult, Constants.RESPONSE_EXPECTATIONS_FIELD);
            if (!expectationsStr.isEmpty()) {
                expectationField.setText(expectationsStr);
            } else {
                expectationField.setText("No expectations");
            }
        } else {
            expectationField.setText("No expectations");
        }
        
        expectationsPanel.add(expectationField);
        return expectationsPanel;
    }
    
    public String getMethodName() {
        return methodNameField != null ? methodNameField.getText() : "";
    }
    
    public String getReturnType() {
        return returnTypeField != null ? returnTypeField.getText() : "";
    }
    
    public String getExpectations() {
        return expectationField != null ? expectationField.getText() : "";
    }
    
    public String buildParametersJson() {
        StringBuilder parametersBuilder = new StringBuilder("[");
        if (!parameterNameFields.isEmpty()) {
            for (int i = 0; i < parameterNameFields.size(); i++) {
                if (i > 0) {
                    parametersBuilder.append(",");
                }
                String name = parameterNameFields.get(i).getText();
                String type = parameterTypeFields.get(i).getText();
                parametersBuilder.append("{\"name\":\"").append(escapeContent(name)).append("\",\"type\":\"").append(escapeContent(type)).append("\",\"constraints\":[]}");
            }
        }
        parametersBuilder.append("]");
        return parametersBuilder.toString();
    }
    
    /**
     * 转义内容中的特殊字符
     * @param content 原始内容
     * @return 转义后的内容
     */
    private String escapeContent(String content) {
        if (content == null) return "";
        
        // 转义反斜杠
        content = content.replace("\\", "\\\\");
        
        // 转义双引号
        content = content.replace("\"", "\\\"");
        
        // 转义换行符
        content = content.replace("\n", "\\n");
        
        // 转义回车符
        content = content.replace("\r", "\\r");
        
        // 转义制表符
        content = content.replace("\t", "\\t");
        
        return content;
    }
}
