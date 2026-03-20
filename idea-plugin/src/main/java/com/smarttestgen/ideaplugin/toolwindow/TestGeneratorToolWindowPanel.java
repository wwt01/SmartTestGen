package com.smarttestgen.ideaplugin.toolwindow;

import com.smarttestgen.ideaplugin.service.code.InsertionManager;
import com.smarttestgen.ideaplugin.toolwindow.components.CodeEditorPanel;
import com.smarttestgen.ideaplugin.toolwindow.components.InfoPanel;
import com.smarttestgen.ideaplugin.model.FileLocationInfo;
import com.smarttestgen.ideaplugin.service.code.CodeStructureService;
import com.smarttestgen.ideaplugin.service.file.FileLocationService;
import com.smarttestgen.ideaplugin.service.code.TestCodeService;
import com.smarttestgen.ideaplugin.service.util.TestFileCreatorService;
import com.smarttestgen.ideaplugin.service.util.ThreadPoolService;
import com.smarttestgen.ideaplugin.toolwindow.components.StructuredResultPanel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 测试代码生成器 ToolWindow 面板
 * 包含 ResultDialog 的所有功能
 */
public class TestGeneratorToolWindowPanel extends JPanel implements StructuredResultPanel.StructuredResultPanelListener {
    private final Project project;
    private String rawResult = "";
    private String codeStructure = "";
    private String selectedText = "";
    
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
    private StructuredResultPanel structuredResultPanel;
    
    // 选择范围
    private int selectionStart = -1;
    private int selectionEnd = -1;
    
    /**
     * 构造方法
     * @param project 当前项目
     */
    public TestGeneratorToolWindowPanel(Project project) {
        super(new BorderLayout());
        this.project = project;
        
        // 初始化UI
        initUI();
    }
    
    /**
     * 初始化UI
     */
    private void initUI() {
        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // 顶部模块：显示结构化结果
        structuredResultPanel = new StructuredResultPanel();
        structuredResultPanel.setListener(this);
        
        // 底部区域：显示生成的测试代码和空方法信息
        JPanel bottomPanel = createBottomPanel();
        
        mainPanel.add(structuredResultPanel, BorderLayout.NORTH);
        mainPanel.add(bottomPanel, BorderLayout.CENTER);
        
        // 添加主面板到滚动面板
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        add(scrollPane, BorderLayout.CENTER);
    }
    
    /**
     * 创建底部面板
     * @return 底部面板
     */
    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        
        // 创建代码编辑器面板
        codeEditorPanel = new CodeEditorPanel(project);
        
        // 创建信息面板
        infoPanel = new InfoPanel();
        
        // 底部按钮面板
        JPanel bottomButtonPanel = new JPanel(new BorderLayout());
        bottomButtonPanel.add(infoPanel, BorderLayout.WEST);
        
        bottomPanel.add(codeEditorPanel, BorderLayout.CENTER);
        bottomPanel.add(bottomButtonPanel, BorderLayout.SOUTH);
        
        return bottomPanel;
    }
    
    /**
     * 设置数据 - 这是关键方法，用于接收解析结果并更新UI
     */
    public void setData(String rawResult, int selectionStart, int selectionEnd, String selectedText) {
        System.out.println("[Test Case Generator] Setting data in ToolWindowPanel");
        
        this.rawResult = rawResult;
        this.selectionStart = selectionStart;
        this.selectionEnd = selectionEnd;
        this.selectedText = selectedText;
        
        // 重置文件信息变量
        selectedFilePath = "";
        selectedFileName = "";
        selectedLineNumber = -1;
        isInterfaceFile = false;
        implementationFiles.clear();
        
        // 获取文件位置信息
        System.out.println("[Test Case Generator] Getting file location info...");
        FileLocationInfo fileInfo = FileLocationService.getFileLocationInfo(selectionEnd);
        
        selectedFilePath = fileInfo.getFilePath();
        selectedFileName = fileInfo.getFileName();
        selectedLineNumber = fileInfo.getLineNumber();
        isInterfaceFile = fileInfo.isInterface();
        currentClassName = fileInfo.getClassName();
        implementationFiles = fileInfo.getImplementationFiles();
        
        System.out.println("[Test Case Generator] File info: " + selectedFileName + ", Line: " + selectedLineNumber);
        
        // 更新UI
        updateComponents();
        
        // 解析代码结构
        parseCodeStructure();
        
        System.out.println("[Test Case Generator] Data set successfully");
    }
    
    /**
     * 更新UI组件
     */
    private void updateComponents() {
        // 更新结构化结果面板
        structuredResultPanel.setData(rawResult, selectedText, codeStructure);
        
        // 更新信息面板
        if (infoPanel != null) {
            infoPanel.updateFileInfo(selectedFileName, selectedFilePath, selectedLineNumber, 
                                     isInterfaceFile, implementationFiles);
        }
    }
    
    /**
     * 解析项目代码结构
     */
    private void parseCodeStructure() {
        System.out.println("[Test Case Generator] Starting to parse code structure...");
        
        CodeStructureService.parseCodeStructureAsync(structureInfo -> {
            codeStructure = CodeStructureService.toFormattedString(structureInfo);
            System.out.println("[Test Case Generator] Code structure parsing completed. Length: " + codeStructure.length());
            
            // 更新结构化结果面板
            structuredResultPanel.setData(rawResult, selectedText, codeStructure);
        });
    }
    
    @Override
    public void onGenerateTestCode() {
        generateTestCode();
    }
    
    @Override
    public void onPrecompileCode() {
        precompileCode();
    }
    
    @Override
    public void onInsertEmptyMethod() {
        insertEmptyMethod();
    }
    
    @Override
    public void onInsertCodeToFile() {
        insertCodeToFile();
    }
    
    @Override
    public void onCreateTestFile() {
        createTestFile();
    }
    
    @Override
    public void onFixCompilationError() {
        fixCompilationError();
    }
    
    /**
     * 生成测试代码
     */
    private void generateTestCode() {
        // 获取父窗口
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        Frame parentFrame = parentWindow instanceof Frame ? (Frame) parentWindow : null;
        
        // 显示加载中提示
        final JDialog loadingDialog = createLoadingDialog(parentWindow, "Generating test code...");
        loadingDialog.setVisible(true);
        
        // 从面板中获取最新的结构化信息
        String methodName = structuredResultPanel.getMethodName();
        String returnType = structuredResultPanel.getReturnType();
        String expectationsStr = structuredResultPanel.getExpectations();
        String parametersStr = structuredResultPanel.buildParametersJson();
        
        // 打印提取的信息
        System.out.println("[Test Case Generator] Method Name: " + methodName);
        System.out.println("[Test Case Generator] Return Type: " + returnType);
        System.out.println("[Test Case Generator] Parameters: " + parametersStr);
        System.out.println("[Test Case Generator] Expectations: " + expectationsStr);
        
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
                    Messages.showErrorDialog("Error: Code editor not initialized", "Error");
                    return;
                }
                
                String testCode = result.getTestCode();
                String emptyMethodCode = result.getEmptyMethodCode();
                
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
                    Messages.showInfoMessage("Test code generated successfully!", "Success");
                } catch (Exception ex) {
                    System.out.println("[Test Case Generator] ERROR updating editor: " + ex.getMessage());
                    ex.printStackTrace();
                    Messages.showErrorDialog("Error updating code editor: " + ex.getMessage(), "Error");
                }
            },
            errorMessage -> {
                // 错误回调
                loadingDialog.dispose();
                Messages.showErrorDialog("Error generating test code: " + errorMessage, "Error");
            }
        );
    }
    
    /**
     * 插入空方法代码
     */
    private void insertEmptyMethod() {
        if (generatedEmptyMethodCode == null || generatedEmptyMethodCode.isEmpty()) {
            Messages.showWarningDialog("No empty method code generated yet. Please generate test code first.", "Warning");
            return;
        }
        
        // 使用 InsertionManager 插入空方法
        InsertionManager.insertEmptyMethodToFile(
            generatedEmptyMethodCode,
            this
        );
    }
    
    /**
     * 将代码插入到文件
     */
    private void insertCodeToFile() {
        if (codeEditorPanel == null) {
            Messages.showErrorDialog("Code editor not initialized", "Error");
            return;
        }
        
        String testCode = codeEditorPanel.getTestCode();
        if (testCode == null || testCode.isEmpty() || testCode.equals("Click 'Generate Test Code' button to generate test code")) {
            Messages.showWarningDialog("No test code to insert. Please generate test code first.", "Warning");
            return;
        }
        
        // 使用 InsertionManager 插入测试代码
        InsertionManager.insertCodeToFile(testCode, this);
    }
    
    /**
     * 创建测试文件
     */
    private void createTestFile() {
        if (codeEditorPanel == null) {
            Messages.showErrorDialog("Code editor not initialized", "Error");
            return;
        }
        
        String testCode = codeEditorPanel.getTestCode();
        if (testCode == null || testCode.isEmpty() || testCode.equals("Click 'Generate Test Code' button to generate test code")) {
            Messages.showWarningDialog("No test code to create file. Please generate test code first.", "Warning");
            return;
        }
        
        // 使用 TestFileCreatorService 创建测试文件
        TestFileCreatorService.createTestFile(testCode, this, selectedFilePath);
    }
    
    /**
     * 预编译代码
     */
    private void precompileCode() {
        System.out.println("[Test Case Generator] Pre-compile button clicked");
        
        // 获取编辑器中的代码
        if (codeEditorPanel == null) {
            System.out.println("[Test Case Generator] Code editor not initialized");
            Messages.showErrorDialog("Code editor not initialized", "Error");
            return;
        }
        
        String code = codeEditorPanel.getTestCode();
        
        System.out.println("[Test Case Generator] Code to compile length: " + (code != null ? code.length() : 0));
        
        // 检查代码是否为空
        if (code == null || code.isEmpty()) {
            System.out.println("[Test Case Generator] No code to compile");
            Messages.showInfoMessage("No code to compile", "Information");
            return;
        }
        
        // 检查是否是默认提示文本
        if (code.equals("Click 'Generate Test Code' button to generate test code")) {
            System.out.println("[Test Case Generator] Default text found, no code to compile");
            Messages.showInfoMessage("Please generate test code first before pre-compiling.", "Information");
            return;
        }
        
        // 显示加载中提示
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        System.out.println("[Test Case Generator] Parent window: " + (parentWindow != null ? parentWindow.getClass().getName() : "null"));
        
        JDialog loadingDialog = createLoadingDialog(parentWindow, "Checking code compilation...");
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
                loadingDialog.dispose();
                
                if (!compilationErrors.isEmpty()) {
                    StringBuilder errorMessage = new StringBuilder("Compilation errors found:\n\n");
                    for (String error : compilationErrors) {
                        errorMessage.append("- ").append(error).append("\n");
                    }
                    errorMessage.append("\nPlease review and fix the errors in the code.");
                    
                    Messages.showErrorDialog(errorMessage.toString(), "Compilation Errors");
                } else {
                    Messages.showInfoMessage("No compilation errors found!\n\nThe code appears to be syntactically correct.", "Success");
                }
            },
            e -> {
                System.out.println("[Test Case Generator] Error in precompileCode: " + e.getMessage());
                e.printStackTrace();
                loadingDialog.dispose();
                Messages.showErrorDialog("Error checking code: " + e.getMessage(), "Error");
            }
        );
    }
    
    /**
     * 创建加载对话框
     */
    private JDialog createLoadingDialog(Window parentWindow, String message) {
        JDialog loadingDialog;
        if (parentWindow instanceof Frame) {
            loadingDialog = new JDialog((Frame) parentWindow, "Processing", false);
        } else if (parentWindow instanceof Dialog) {
            loadingDialog = new JDialog((Dialog) parentWindow, "Processing", false);
        } else {
            loadingDialog = new JDialog();
        }
        loadingDialog.setModal(false);
        loadingDialog.setSize(300, 100);
        loadingDialog.setLocationRelativeTo(parentWindow);
        loadingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        loadingDialog.setAlwaysOnTop(true);
        
        JPanel panel = new JPanel();
        panel.add(new JLabel(message));
        loadingDialog.add(panel);
        
        return loadingDialog;
    }
    
    /**
     * 检查代码编译正确性
     * @param code 要检查的代码
     * @return 编译错误列表
     */
    private java.util.List<String> checkCodeCompilation(String code) {
        System.out.println("[Test Case Generator] Starting checkCodeCompilation");
        java.util.List<String> errors = new ArrayList<>();
        
        try {
            // 获取当前项目
            com.intellij.openapi.project.Project[] projects = com.intellij.openapi.project.ProjectManager.getInstance().getOpenProjects();
            com.intellij.openapi.project.Project project = projects.length > 0 ? projects[0] : null;
            
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
                com.intellij.psi.PsiFileFactory psiFileFactory = com.intellij.psi.PsiFileFactory.getInstance(project);
                com.intellij.psi.PsiFile psiFile = psiFileFactory.createFileFromText(
                    "TempTest.java",
                    com.intellij.openapi.fileTypes.StdFileTypes.JAVA,
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
    private int checkPsiErrors(com.intellij.psi.PsiElement element, java.util.List<String> errors) {
        int errorCount = 0;
        
        // 检查当前元素是否为错误元素
        if (element instanceof com.intellij.psi.PsiErrorElement) {
            com.intellij.psi.PsiErrorElement errorElement = (com.intellij.psi.PsiErrorElement) element;
            String errorDescription = errorElement.getErrorDescription();
            System.out.println("[Test Case Generator] PSI Error: " + errorDescription);
            errors.add("Syntax error: " + errorDescription);
            errorCount++;
        }
        
        // 递归检查所有子元素
        for (com.intellij.psi.PsiElement child : element.getChildren()) {
            errorCount += checkPsiErrors(child, errors);
        }
        
        return errorCount;
    }
    
    /**
     * 修复编译错误
     */
    private void fixCompilationError() {
        System.out.println("[Test Case Generator] Fix compilation error button clicked");
        
        // 获取编辑器中的代码
        if (codeEditorPanel == null) {
            System.out.println("[Test Case Generator] Code editor not initialized");
            Messages.showErrorDialog("Code editor not initialized", "Error");
            return;
        }
        
        String code = codeEditorPanel.getTestCode();
        
        // 检查代码是否为空
        if (code == null || code.isEmpty()) {
            System.out.println("[Test Case Generator] No code to fix");
            Messages.showInfoMessage("No code to fix", "Information");
            return;
        }
        
        // 检查是否是默认提示文本
        if (code.equals("Click 'Generate Test Code' button to generate test code")) {
            System.out.println("[Test Case Generator] Default text found, no code to fix");
            Messages.showInfoMessage("Please generate test code first before fixing compilation errors.", "Information");
            return;
        }
        
        // 获取控制台信息
        String consoleOutput = getConsoleOutput();
        
        // 检查控制台信息
        if (consoleOutput == null || consoleOutput.isEmpty()) {
            System.out.println("[Test Case Generator] No console output found");
            Messages.showWarningDialog("No console output found. Please run the project first.", "Warning");
            return;
        }
        
        // 判断是否是报错信息
        boolean hasError = consoleOutput.contains("error:") || consoleOutput.contains("Exception in thread") || consoleOutput.contains("Compilation failed");
        
        if (!hasError) {
            System.out.println("[Test Case Generator] No compilation errors found in console output");
            Messages.showInfoMessage("No compilation errors found in console output.", "Information");
            return;
        }
        
        // 显示加载中提示
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        JDialog loadingDialog = createLoadingDialog(parentWindow, "Fixing compilation errors...");
        loadingDialog.setVisible(true);
        
        // 构建请求体
        String requestBody = buildFixCompilationErrorRequest(code, consoleOutput);
        
        // 调用后端接口
        TestCodeService.fixCompilationError(
            requestBody,
            result -> {
                loadingDialog.dispose();
                
                // 清空文件内容
                codeEditorPanel.setTestCode("");
                
                // 填充修复后的代码
                String fixedCode = result.getTestCode();
                codeEditorPanel.setTestCode(fixedCode != null ? fixedCode : "Failed to fix compilation error");
                
                // 显示成功消息
                Messages.showInfoMessage("Compilation error fixed successfully!", "Success");
            },
            errorMessage -> {
                loadingDialog.dispose();
                Messages.showErrorDialog("Error fixing compilation error: " + errorMessage, "Error");
            }
        );
    }
    
    /**
     * 获取控制台输出信息
     * @return 控制台输出信息
     */
    private String getConsoleOutput() {
        try {
            // 暂时返回模拟数据，实际获取控制台输出需要更复杂的实现
            // 这里使用模拟数据来测试功能
            return "error: cannot find symbol\n  symbol:   class Calculator\n  location: class CalculatorTest\nerror: cannot find symbol\n  symbol:   method add(int,int)\n  location: class Calculator\nerror: cannot find symbol\n  symbol:   method assertEquals(int,int)\n  location: class CalculatorTest";
        } catch (Exception e) {
            System.out.println("[Test Case Generator] Error getting console output: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 构建修复编译错误的请求体
     * @param code 测试代码
     * @param errorMessage 错误信息
     * @return 请求体
     */
    private String buildFixCompilationErrorRequest(String code, String errorMessage) {
        StringBuilder requestBody = new StringBuilder();
        requestBody.append("{");
        requestBody.append("\"code\":\"").append(escapeContent(code)).append("\",");
        requestBody.append("\"error_message\":\"").append(escapeContent(errorMessage)).append("\",");
        requestBody.append("\"code_structure\":\"").append(escapeContent(codeStructure)).append("\",");
        requestBody.append("\"current_class_name\":\"").append(escapeContent(currentClassName)).append("\",");
        requestBody.append("\"is_interface_file\":").append(isInterfaceFile);
        requestBody.append("}");
        return requestBody.toString();
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
