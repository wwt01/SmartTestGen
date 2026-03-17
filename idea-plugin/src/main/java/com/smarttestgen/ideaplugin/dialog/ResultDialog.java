package com.smarttestgen.ideaplugin.dialog;

import com.smarttestgen.ideaplugin.dialog.components.ButtonPanel;
import com.smarttestgen.ideaplugin.dialog.components.CodeEditorPanel;
import com.smarttestgen.ideaplugin.dialog.components.InfoPanel;
import com.smarttestgen.ideaplugin.model.FileLocationInfo;
import com.smarttestgen.ideaplugin.service.CodeStructureService;
import com.smarttestgen.ideaplugin.service.FileLocationService;
import com.smarttestgen.ideaplugin.service.TestCodeService;
import com.smarttestgen.ideaplugin.service.ThreadPoolService;
import com.smarttestgen.ideaplugin.service.TestFileCreatorService;
import com.smarttestgen.ideaplugin.util.Constants;
import com.smarttestgen.ideaplugin.util.JsonUtils;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileTypes.StdFileTypes;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 结果对话框类
 */
public class ResultDialog extends DialogWrapper {
    private final String rawResult;
    private String codeStructure;
    
    // 文本框成员变量，用于获取用户修改后的最新值
    private JTextField methodNameField;
    private JTextField returnTypeField;
    private JTextField expectationField;

    private List<JTextField> parameterNameFields = new ArrayList<>();
    private List<JTextField> parameterTypeFields = new ArrayList<>();
    
    // 保存生成的空方法代码，用于后续插入
    private String generatedEmptyMethodCode = "";
    
    // 保存选中文本所在文件的信息
    private String selectedFilePath = "";
    private String selectedFileName = "";
    private int selectedLineNumber = -1;
    private boolean isInterfaceFile = false;
    private String currentClassName = "";
    private List<String> implementationFiles = new ArrayList<>();
    
    // UI 组件
    private InfoPanel infoPanel;
    private CodeEditorPanel codeEditorPanel;
    private ButtonPanel buttonPanel;
    
    /**
     * 构造方法
     * @param result 结果字符串
     */
    public ResultDialog(String result) {
        super(true);
        this.rawResult = result;
        this.codeStructure = "";
        setTitle("Structured Information");
        init();
        
        // 初始化时解析项目代码结构
        parseCodeStructure();
    }
    
    /**
     * 构造方法（带选择范围）
     * @param result 结果字符串
     * @param selectionStart 选择范围的起始位置
     * @param selectionEnd 选择范围的结束位置
     */
    public ResultDialog(String result, int selectionStart, int selectionEnd) {
        super(true);
        this.rawResult = result;
        this.codeStructure = "";
        setTitle("Structured Information");
        
        System.out.println("[Test Case Generator] Creating ResultDialog with selectionStart: " + selectionStart + ", selectionEnd: " + selectionEnd);
        
        // 重置文件信息变量
        selectedFilePath = "";
        selectedFileName = "";
        selectedLineNumber = -1;
        isInterfaceFile = false;
        implementationFiles.clear();
        
        // 在主线程中获取文件位置信息，这些操作比较快，不会阻塞主线程
        System.out.println("[Test Case Generator] Getting file location info in main thread...");
        FileLocationInfo fileInfo = FileLocationService.getFileLocationInfo(selectionEnd);
        
        // 更新文件信息变量
        selectedFilePath = fileInfo.getFilePath();
        selectedFileName = fileInfo.getFileName();
        selectedLineNumber = fileInfo.getLineNumber();
        isInterfaceFile = fileInfo.isInterface();
        currentClassName = fileInfo.getClassName();
        implementationFiles = fileInfo.getImplementationFiles();
        
        System.out.println("[Test Case Generator] File location info obtained: File: " + selectedFileName + ", Path: " + selectedFilePath + ", Line: " + selectedLineNumber + ", Is interface: " + isInterfaceFile);
        
        // 如果没有获取到文件信息，使用默认值
        if (selectedFileName.isEmpty()) {
            System.out.println("[Test Case Generator] No file info obtained, using default values");
            selectedFileName = "Unknown file";
            selectedFilePath = "Unknown path";
            selectedLineNumber = -1;
        }
        
        // 调用 init 方法，初始化对话框，确保对话框能够及时显示
        System.out.println("[Test Case Generator] Initializing dialog...");
        init();
        System.out.println("[Test Case Generator] Dialog initialized");
        
        // 在后台线程中执行耗时操作，避免阻塞主线程
        System.out.println("[Test Case Generator] Parsing code structure in background...");
        ThreadPoolService.getInstance().runInBackground(
            () -> parseCodeStructure(),
            null,
            e -> {
                System.out.println("[Test Case Generator] Error in background thread: " + e.getMessage());
                e.printStackTrace();
            }
        );
    }
    

    
    /**
     * 更新信息面板，显示详细的文件信息
     */
    private void updateInfoPanel() {
        System.out.println("[Test Case Generator] Updating info panel...");
        // 重新创建中心面板，更新信息面板
        JComponent centerPanel = createCenterPanel();
        // 获取对话框的内容面板
        JComponent contentPanel = getContentPanel();
        // 移除旧的中心面板
        contentPanel.removeAll();
        // 添加新的中心面板
        contentPanel.add(centerPanel, BorderLayout.CENTER);
        // 重新布局内容面板
        contentPanel.revalidate();
        contentPanel.repaint();
        // 调整对话框大小以适应新的内容
        pack();
        System.out.println("[Test Case Generator] Info panel updated");
    }
    
    /**
     * 解析项目代码结构
     */
    private void parseCodeStructure() {
        System.out.println("[Test Case Generator] Starting to parse code structure...");
        
        // 使用 CodeStructureService 解析代码结构
        CodeStructureService.parseCodeStructureAsync(structureInfo -> {
            codeStructure = CodeStructureService.toFormattedString(structureInfo);
            System.out.println("[Test Case Generator] Code structure parsing completed. Length: " + codeStructure.length());
        });
    }
    

    
    /**
     * 创建中心面板
     * @return 中心面板
     */
    @Override
    protected JComponent createCenterPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // 顶部模块：显示结构化结果
        JPanel topPanel = createTopPanel();
        
        // 底部区域：显示生成的测试代码和空方法信息
        JPanel bottomPanel = createBottomPanel();
        
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(bottomPanel, BorderLayout.CENTER);
        
        return mainPanel;
    }
    
    /**
     * 创建顶部面板
     * @return 顶部面板
     */
    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createTitledBorder("Structured Result"));
        
        // 结构化结果内容面板
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        
        // 提取结构化结果
        String methodName = JsonUtils.extractField(rawResult, Constants.RESPONSE_METHOD_NAME_FIELD);
        boolean isMethodNameGenerated = JsonUtils.extractBooleanField(rawResult, Constants.RESPONSE_METHOD_NAME_FIELD, Constants.RESPONSE_IS_CONSTRUCTED_FIELD);
        
        String returnType = JsonUtils.extractField(rawResult, Constants.RESPONSE_RETURN_TYPE_FIELD);
        boolean isReturnTypeGenerated = JsonUtils.extractBooleanField(rawResult, Constants.RESPONSE_RETURN_TYPE_FIELD, Constants.RESPONSE_IS_CONSTRUCTED_FIELD);
        
        boolean isParametersGenerated = JsonUtils.extractBooleanField(rawResult, Constants.RESPONSE_PARAMETERS_FIELD, Constants.RESPONSE_IS_CONSTRUCTED_FIELD);
        
        // 方法名
        UiComponents.addTextFieldWithSource(contentPanel, "Method Name", methodName, isMethodNameGenerated, methodNameField, returnTypeField, expectationField);
        
        // 参数
        JPanel paramsPanel = createParametersPanel(isParametersGenerated);
        contentPanel.add(paramsPanel);
        
        // 返回类型
        UiComponents.addTextFieldWithSource(contentPanel, "Return Type", returnType, isReturnTypeGenerated, methodNameField, returnTypeField, expectationField);
        
        // 期望
        JPanel expectationsPanel = createExpectationsPanel();
        contentPanel.add(expectationsPanel);
        
        // 按钮面板
        buttonPanel = new ButtonPanel();
        buttonPanel.setViewStructureListener(e -> UiComponents.showStructureDialog(codeStructure));
        buttonPanel.setViewRawResultListener(e -> UiComponents.showRawResultDialog(rawResult));
        buttonPanel.setGenerateTestListener(e -> generateTestCode());
        buttonPanel.setPrecompileListener(e -> precompileCode());
        buttonPanel.setInsertEmptyMethodListener(e -> insertEmptyMethod());
        buttonPanel.setInsertToFileListener(e -> insertCodeToFile());
        buttonPanel.setCreateTestFileListener(e -> createTestFile());
        
        topPanel.add(contentPanel, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        return topPanel;
    }
    
    /**
     * 创建参数面板
     * @param isParametersGenerated 参数是否生成
     * @return 参数面板
     */
    private JPanel createParametersPanel(boolean isParametersGenerated) {
        JPanel paramsPanel = new JPanel();
        paramsPanel.setLayout(new BoxLayout(paramsPanel, BoxLayout.Y_AXIS));
        paramsPanel.setBorder(BorderFactory.createTitledBorder("Parameters"));
        
        // 解析参数信息
        java.util.List<JsonUtils.ParameterInfo> parameters = 
            JsonUtils.extractParameters(rawResult, Constants.RESPONSE_PARAMETERS_FIELD);
        
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
        
        return paramsPanel;
    }
    
    /**
     * 创建期望面板
     * @return 期望面板
     */
    private JPanel createExpectationsPanel() {
        JPanel expectationsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        expectationsPanel.setBorder(BorderFactory.createTitledBorder("Expectations"));
        JTextField expectationField = new JTextField(50);
        
        // 提取期望信息
        String expectationsStr = JsonUtils.extractArrayField(rawResult, Constants.RESPONSE_EXPECTATIONS_FIELD);
        if (!expectationsStr.isEmpty()) {
            // 简单处理，实际项目中可能需要更复杂的解析
            expectationField.setText(expectationsStr.replaceAll("\"", ""));
        } else {
            expectationField.setText("No expectations");
        }
        
        expectationsPanel.add(expectationField);
        return expectationsPanel;
    }
    
    /**
     * 创建底部面板
     * @return 底部面板
     */
    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        
        // 获取当前项目
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        Project project = projects.length > 0 ? projects[0] : null;
        
        // 创建代码编辑器面板
        codeEditorPanel = new CodeEditorPanel(project);
        
        // 设置按钮监听器
        buttonPanel.setPrecompileListener(e -> precompileCode());
        buttonPanel.setInsertEmptyMethodListener(e -> insertEmptyMethod());
        buttonPanel.setInsertToFileListener(e -> insertCodeToFile());
        buttonPanel.setCreateTestFileListener(e -> createTestFile());
        
        // 创建信息面板
        infoPanel = new InfoPanel();
        infoPanel.updateFileInfo(selectedFileName, selectedFilePath, selectedLineNumber, 
                                 isInterfaceFile, implementationFiles);
        
        // 底部按钮面板
        JPanel bottomButtonPanel = new JPanel(new BorderLayout());
        bottomButtonPanel.add(infoPanel, BorderLayout.WEST);
        bottomButtonPanel.add(buttonPanel, BorderLayout.EAST);
        
        bottomPanel.add(codeEditorPanel, BorderLayout.CENTER);
        bottomPanel.add(bottomButtonPanel, BorderLayout.SOUTH);
        
        return bottomPanel;
    }
    


    
    /**
     * 生成测试代码
     */
    private void generateTestCode() {
        // 获取父窗口
        Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this.getWindow());
        
        // 显示加载中提示
        final JDialog loadingDialog = UiComponents.createLoadingDialog(parentFrame);
        ThreadPoolService.getInstance().runInEdt(() -> loadingDialog.setVisible(true));
        
        // 从文本框中获取最新的结构化信息（用户可能修改过）
        String methodName = methodNameField != null ? methodNameField.getText() : 
            JsonUtils.extractField(rawResult, Constants.RESPONSE_METHOD_NAME_FIELD);
        String returnType = returnTypeField != null ? returnTypeField.getText() : 
            JsonUtils.extractField(rawResult, Constants.RESPONSE_RETURN_TYPE_FIELD);
        String expectationsStr = expectationField != null ? expectationField.getText() : 
            JsonUtils.extractArrayField(rawResult, Constants.RESPONSE_EXPECTATIONS_FIELD);
        
        // 从参数文本框中获取最新的参数信息
        String parametersStr = buildParametersJson();
        
        // 打印提取的信息
        System.out.println("[Test Case Generator] Method Name: " + methodName);
        System.out.println("[Test Case Generator] Return Type: " + returnType);
        System.out.println("[Test Case Generator] Parameters: " + parametersStr);
        System.out.println("[Test Case Generator] Expectations: " + expectationsStr);
        System.out.println("[Test Case Generator] Code Structure: " + 
            (codeStructure.length() > 100 ? codeStructure.substring(0, 100) + "..." : codeStructure));
        
        // 使用 TestCodeService 生成测试代码
        TestCodeService.generateTestCode(
            methodName,
            returnType,
            parametersStr,
            expectationsStr,
            codeStructure,
            currentClassName,
            isInterfaceFile,
            result -> {
                // 成功回调
                loadingDialog.dispose();
                
                // 检查编辑器状态
                if (codeEditorPanel == null) {
                    System.out.println("[Test Case Generator] ERROR: codeEditorPanel is null");
                    JOptionPane.showMessageDialog(parentFrame, "Error: Code editor not initialized", 
                        "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                String testCode = result.getTestCode();
                String emptyMethodCode = result.getEmptyMethodCode();
                
                System.out.println("[Test Case Generator] Unescaped code length: " + 
                    (testCode != null ? testCode.length() : 0));
                
                try {
                    // 使用 CodeEditorPanel 设置代码
                    codeEditorPanel.setTestCode(testCode != null ? testCode : "No test code generated");
                    
                    // 更新空方法编辑器内容
                    String emptyMethodText = emptyMethodCode != null && !emptyMethodCode.isEmpty() ? 
                        emptyMethodCode : "No empty method generated";
                    codeEditorPanel.setEmptyMethodCode(emptyMethodText);
                    
                    System.out.println("[Test Case Generator] Editor content updated successfully");
                    
                    // 保存生成的空方法代码，用于后续插入
                    if (emptyMethodCode != null && !emptyMethodCode.isEmpty()) {
                        generatedEmptyMethodCode = emptyMethodCode;
                    }
                    
                    // 显示成功消息
                    JOptionPane.showMessageDialog(parentFrame, 
                        "Test code generated and filled into editor!\n\n" +
                        "Use the 'Pre-compile' button to check for compilation errors.\n" +
                        "Use the 'Insert Empty Method' button to insert the empty method implementation.", 
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    System.out.println("[Test Case Generator] ERROR updating editor: " + ex.getMessage());
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(parentFrame, "Error updating code editor: " + ex.getMessage(), 
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            },
            errorMessage -> {
                // 错误回调
                loadingDialog.dispose();
                JOptionPane.showMessageDialog(parentFrame, "Error generating test code: " + errorMessage, 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        );
    }
    
    /**
     * 构建参数字符串（JSON格式）
     * @return 参数字符串
     */
    private String buildParametersJson() {
        StringBuilder parametersBuilder = new StringBuilder("[");
        if (!parameterNameFields.isEmpty()) {
            for (int i = 0; i < parameterNameFields.size(); i++) {
                if (i > 0) {
                    parametersBuilder.append(",");
                }
                String name = parameterNameFields.get(i).getText();
                String type = parameterTypeFields.get(i).getText();
                parametersBuilder.append("{\"name\":\"").append(name).append("\",\"type\":\"").append(type).append("\",\"constraints\":[]}");
            }
        }
        parametersBuilder.append("]");
        return parametersBuilder.toString();
    }
    
    /**
     * 插入空方法代码（从保存的变量中获取）
     */
    private void insertEmptyMethod() {
        if (generatedEmptyMethodCode == null || generatedEmptyMethodCode.isEmpty()) {
            JOptionPane.showMessageDialog(getWindow(), "No empty method code generated yet. Please generate test code first.", "Information", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        InsertionManager.insertEmptyMethodToFile(generatedEmptyMethodCode, getContentPanel());
    }
    
    /**
     * 创建测试文件
     */
    private void createTestFile() {
        // 获取编辑器中的代码
        if (codeEditorPanel == null) {
            JOptionPane.showMessageDialog(getWindow(), "Code editor not initialized", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String code = codeEditorPanel.getTestCode();
        
        // 检查代码是否为空
        if (code == null || code.isEmpty() || code.equals("Click 'Generate Test Code' button to generate test code")) {
            JOptionPane.showMessageDialog(getWindow(), "No code to create test file", "Information", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // 调用 TestFileCreatorService.createTestFile 方法创建测试文件
        TestFileCreatorService.createTestFile(code, getContentPanel(), selectedFilePath);
    }
    

    
    /**
     * 插入代码到文件
     */
    private void insertCodeToFile() {
        // 获取编辑器中的代码
        if (codeEditorPanel == null) {
            JOptionPane.showMessageDialog(getWindow(), "Code editor not initialized", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String code = codeEditorPanel.getTestCode();
        
        // 检查代码是否为空
        if (code == null || code.isEmpty() || code.equals("Click 'Generate Test Code' button to generate test code")) {
            JOptionPane.showMessageDialog(getWindow(), "No code to insert", "Information", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // 调用 InsertionManager.insertCodeToFile 方法插入代码到文件
        InsertionManager.insertCodeToFile(code, getContentPanel());
    }
    

    
    /**
     * 预编译代码，检查编译正确性
     */
    private void precompileCode() {
        System.out.println("[Test Case Generator] Pre-compile button clicked");
        
        // 获取编辑器中的代码
        if (codeEditorPanel == null) {
            System.out.println("[Test Case Generator] Code editor not initialized");
            JOptionPane.showMessageDialog(getWindow(), "Code editor not initialized", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        String code = codeEditorPanel.getTestCode();
        
        System.out.println("[Test Case Generator] Code to compile length: " + (code != null ? code.length() : 0));
        System.out.println("[Test Case Generator] Code to compile (first 100 chars): " + (code != null && code.length() > 100 ? code.substring(0, 100) + "..." : code));
        
        // 检查代码是否为空
        if (code == null || code.isEmpty()) {
            System.out.println("[Test Case Generator] No code to compile");
            JOptionPane.showMessageDialog(getWindow(), "No code to compile", "Information", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // 检查是否是默认提示文本
        if (code.equals("Click 'Generate Test Code' button to generate test code")) {
            System.out.println("[Test Case Generator] Default text found, no code to compile");
            JOptionPane.showMessageDialog(getWindow(), "Please generate test code first before pre-compiling.", "Information", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // 显示加载中提示
        Window parentWindow = SwingUtilities.getWindowAncestor(getContentPanel());
        System.out.println("[Test Case Generator] Parent window: " + (parentWindow != null ? parentWindow.getClass().getName() : "null"));
        
        JDialog loadingDialog;
        if (parentWindow instanceof Frame) {
            loadingDialog = UiComponents.createLoadingDialog((Frame) parentWindow, "Checking code compilation...");
        } else if (parentWindow instanceof Dialog) {
            loadingDialog = new JDialog((Dialog) parentWindow, "Processing", false);
            loadingDialog.setModal(false);
            loadingDialog.setSize(300, 100);
            loadingDialog.setLocationRelativeTo(parentWindow);
            loadingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            loadingDialog.setAlwaysOnTop(true);
            JPanel panel = new JPanel();
            panel.add(new JLabel("Checking code compilation..."));
            loadingDialog.add(panel);
        } else {
            loadingDialog = new JDialog();
            loadingDialog.setModal(false);
            loadingDialog.setSize(300, 100);
            loadingDialog.setLocationRelativeTo(null);
            loadingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            loadingDialog.setAlwaysOnTop(true);
            JPanel panel = new JPanel();
            panel.add(new JLabel("Checking code compilation..."));
            loadingDialog.add(panel);
        }
        loadingDialog.setVisible(true);
        
        System.out.println("[Test Case Generator] Loading dialog shown");
        
        // 异步检查代码编译
        ThreadPoolService.getInstance().computeInBackground(
            () -> {
                System.out.println("[Test Case Generator] Starting code compilation check");
                return checkCodeCompilation(code);
            },
            compilationErrors -> {
                System.out.println("[Test Case Generator] Compilation check completed with " + compilationErrors.size() + " errors");
                System.out.println("[Test Case Generator] Showing compilation results");
                loadingDialog.dispose();
                
                if (!compilationErrors.isEmpty()) {
                    StringBuilder errorMessage = new StringBuilder("Compilation errors found:\n\n");
                    for (String error : compilationErrors) {
                        errorMessage.append("- " + error).append("\n");
                    }
                    errorMessage.append("\nPlease review and fix the errors in the code.");
                    
                    JOptionPane.showMessageDialog(getWindow(), errorMessage.toString(), "Compilation Errors", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(getWindow(), "No compilation errors found!\n\nThe code appears to be syntactically correct.", "Success", JOptionPane.INFORMATION_MESSAGE);
                }
            },
            e -> {
                System.out.println("[Test Case Generator] Error in precompileCode: " + e.getMessage());
                e.printStackTrace();
                loadingDialog.dispose();
                JOptionPane.showMessageDialog(getWindow(), "Error checking code: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        );
    }
    
    /**
     * 检查代码编译正确性
     * @param code 要检查的代码
     * @return 编译错误列表
     */
    private List<String> checkCodeCompilation(String code) {
        System.out.println("[Test Case Generator] Starting checkCodeCompilation");
        List<String> errors = new ArrayList<>();
        
        try {
            // 获取当前项目
            Project[] projects = ProjectManager.getInstance().getOpenProjects();
            Project project = projects.length > 0 ? projects[0] : null;
            
            if (project == null) {
                System.out.println("[Test Case Generator] No open project found");
                errors.add("No open project found");
                return errors;
            }
            
            System.out.println("[Test Case Generator] Project found: " + project.getName());
            
            // 检查代码是否为空
            if (code == null || code.isEmpty()) {
                System.out.println("[Test Case Generator] Code is empty");
                errors.add("Generated code is empty");
                return errors;
            }
            
            System.out.println("[Test Case Generator] Code length: " + code.length());
            
            // 使用 PSI 进行快速语法检查
            System.out.println("[Test Case Generator] Starting PSI syntax check");
            try {
                // 使用 PsiFileFactory 创建临时文件并解析
                PsiFileFactory psiFileFactory = PsiFileFactory.getInstance(project);
                PsiFile psiFile = psiFileFactory.createFileFromText(
                    "TempTest.java",
                    StdFileTypes.JAVA,
                    code
                );
                
                // 递归检查所有 PSI 元素中的错误
                int psiErrorCount = checkPsiErrors(psiFile, errors);
                
                System.out.println("[Test Case Generator] PSI check completed with " + psiErrorCount + " errors");
                
            } catch (Exception e) {
                System.out.println("[Test Case Generator] Error in PSI check: " + e.getMessage());
                e.printStackTrace();
                errors.add("Error checking syntax: " + e.getMessage());
            }
            
        } catch (Exception e) {
            System.out.println("[Test Case Generator] Error in checkCodeCompilation: " + e.getMessage());
            e.printStackTrace();
            errors.add("Error checking code: " + e.getMessage());
        }
        
        return errors;
    }
    
    /**
     * 递归检查 PSI 元素及其子元素中的错误
     * @param element PSI 元素
     * @param errors 错误列表
     * @return 错误数量
     */
    private int checkPsiErrors(PsiElement element, List<String> errors) {
        int errorCount = 0;
        
        // 检查当前元素是否为错误元素
        if (element instanceof PsiErrorElement) {
            PsiErrorElement errorElement = (PsiErrorElement) element;
            String errorDescription = errorElement.getErrorDescription();
            System.out.println("[Test Case Generator] PSI Error: " + errorDescription);
            errors.add("Syntax error: " + errorDescription);
            errorCount++;
        }
        
        // 递归检查所有子元素
        for (PsiElement child : element.getChildren()) {
            errorCount += checkPsiErrors(child, errors);
        }
        
        return errorCount;
    }
    
    /**
     * 释放资源
     */
    @Override
    public void dispose() {
        super.dispose();
        // 释放编辑器资源
        if (codeEditorPanel != null) {
            Editor testCodeEditor = codeEditorPanel.getTestCodeEditor();
            Editor emptyMethodEditor = codeEditorPanel.getEmptyMethodEditor();
            if (testCodeEditor != null) {
                EditorFactory.getInstance().releaseEditor(testCodeEditor);
            }
            if (emptyMethodEditor != null) {
                EditorFactory.getInstance().releaseEditor(emptyMethodEditor);
            }
        }
    }
}
