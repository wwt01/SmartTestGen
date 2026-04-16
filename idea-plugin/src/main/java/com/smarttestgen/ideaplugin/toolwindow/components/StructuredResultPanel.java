package com.smarttestgen.ideaplugin.toolwindow.components;

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
    /** 按钮面板，包含所有操作按钮 */
    private final ButtonPanel buttonPanel;
    /** 方法名输入框 */
    private JTextField methodNameField;
    /** 返回类型输入框 */
    private JTextField returnTypeField;
    /** 期望信息输入框 */
    private JTextField expectationField;
    /** 静态方法勾选框 */
    private JCheckBox isStaticCheckBox;
    /** 参数名称输入框列表 */
    private List<JTextField> parameterNameFields = new ArrayList<>();
    /** 参数类型输入框列表 */
    private List<JTextField> parameterTypeFields = new ArrayList<>();
    /** 当前项目的代码结构信息 */
    private String codeStructure = "";
    /** 后端返回的原始JSON结果 */
    private String rawResult = "";
    /** 面板事件监听器 */
    private StructuredResultPanelListener listener;
    
    public interface StructuredResultPanelListener {
        void onGenerateTestCode();
        void onPrecompileCode();
        void onFixCompilationError();
        void onInsertEmptyMethod();
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
        
        boolean isStatic = false;
        if (rawResult != null && !rawResult.isEmpty()) {
            methodName = JsonUtils.extractField(rawResult, Constants.RESPONSE_METHOD_NAME_FIELD);
            isMethodNameGenerated = JsonUtils.extractBooleanField(rawResult, Constants.RESPONSE_METHOD_NAME_FIELD, Constants.RESPONSE_IS_CONSTRUCTED_FIELD);
            returnType = JsonUtils.extractField(rawResult, Constants.RESPONSE_RETURN_TYPE_FIELD);
            isReturnTypeGenerated = JsonUtils.extractBooleanField(rawResult, Constants.RESPONSE_RETURN_TYPE_FIELD, Constants.RESPONSE_IS_CONSTRUCTED_FIELD);
            isParametersGenerated = JsonUtils.extractBooleanField(rawResult, Constants.RESPONSE_PARAMETERS_FIELD, Constants.RESPONSE_IS_CONSTRUCTED_FIELD);
            isStatic = JsonUtils.extractBooleanField(rawResult, "is_static");
        }
        
        // 方法名
        methodNameField = new JTextField(methodName, 30);
        JPanel methodNamePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        methodNamePanel.add(new JLabel("Method Name: "));
        methodNamePanel.add(methodNameField);
        methodNamePanel.add(Box.createHorizontalStrut(10));
        methodNamePanel.add(new JLabel("(Generated: " + isMethodNameGenerated + ")"));
        
        // 静态方法勾选框
        isStaticCheckBox = new JCheckBox("Is Static Method");
        isStaticCheckBox.setSelected(isStatic);
        methodNamePanel.add(Box.createHorizontalStrut(20));
        methodNamePanel.add(isStaticCheckBox);
        
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
                    
                    // 参数限制标签
                    paramRowPanel.add(Box.createHorizontalStrut(10));
                    paramRowPanel.add(new JLabel("Constraints:"));
                    JTextField constraintsField = new JTextField(param.constraints, 30);
                    constraintsField.setEditable(false);
                    paramRowPanel.add(constraintsField);
                    
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
    
    public boolean isStaticMethod() {
        return isStaticCheckBox != null ? isStaticCheckBox.isSelected() : false;
    }
    
    public String buildParametersJson() {
        StringBuilder parametersBuilder = new StringBuilder("[");
        if (!parameterNameFields.isEmpty()) {
            // 重新解析参数信息，获取完整的参数数据（包括限制）
            List<JsonUtils.ParameterInfo> parameters = JsonUtils.extractParameters(rawResult, Constants.RESPONSE_PARAMETERS_FIELD);
            
            for (int i = 0; i < parameterNameFields.size(); i++) {
                if (i > 0) {
                    parametersBuilder.append(",");
                }
                String name = parameterNameFields.get(i).getText();
                String type = parameterTypeFields.get(i).getText();
                
                // 获取参数的限制信息
                String constraints = "[]";
                if (i < parameters.size()) {
                    String constraintsStr = parameters.get(i).constraints;
                    if (!constraintsStr.isEmpty()) {
                        constraints = "[" + constraintsStr + "]";
                    }
                }
                
                parametersBuilder.append("{\"name\":\"").append(escapeContent(name)).append("\",\"type\":\"").append(escapeContent(type)).append("\",\"constraints\":")
                        .append(constraints).append("}");
            }
        }
        parametersBuilder.append("]");
        return parametersBuilder.toString();
    }
    
    public void setGenerateTestButtonEnabled(boolean enabled) {
        buttonPanel.setGenerateTestButtonEnabled(enabled);
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
