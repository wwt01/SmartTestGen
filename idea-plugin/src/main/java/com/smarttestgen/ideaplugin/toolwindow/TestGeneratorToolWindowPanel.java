package com.smarttestgen.ideaplugin.toolwindow;

import com.smarttestgen.ideaplugin.model.ClassContextInfo;
import com.smarttestgen.ideaplugin.model.FileLocationInfo;
import com.smarttestgen.ideaplugin.service.code.CodeStructureService;
import com.smarttestgen.ideaplugin.service.code.InsertionManager;
import com.smarttestgen.ideaplugin.service.code.TestCodeService;
import com.smarttestgen.ideaplugin.service.file.FileLocationService;
import com.smarttestgen.ideaplugin.service.util.TestFileCreatorService;
import com.smarttestgen.ideaplugin.service.util.ThreadPoolService;
import com.smarttestgen.ideaplugin.toolwindow.components.CodeEditorPanel;
import com.smarttestgen.ideaplugin.toolwindow.components.InfoPanel;
import com.smarttestgen.ideaplugin.toolwindow.components.StructuredResultPanel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 测试代码生成器 ToolWindow 面板
 * 包含 ResultDialog 的所有功能
 */
public class TestGeneratorToolWindowPanel extends JPanel implements StructuredResultPanel.StructuredResultPanelListener {
    /** 当前项目实例 */
    private final Project project;
    /** 后端返回的原始JSON结果 */
    private String rawResult = "";
    /** 当前项目的代码结构信息 */
    private String codeStructure = "";
    /** 用户选中的需求文本 */
    private String selectedText = "";
    
    /** 生成的空方法代码，用于后续插入到源文件 */
    private String generatedEmptyMethodCode = "";
    
    /** 选中文本所在文件的完整路径 */
    private String selectedFilePath = "";
    /** 选中文本所在的文件名 */
    private String selectedFileName = "";
    /** 选中文本所在的行号 */
    private int selectedLineNumber = -1;
    /** 选中文本所在文件是否为接口类 */
    private boolean isInterfaceFile = false;
    /** 选中文本所在的类名 */
    private String currentClassName = "";
    /** 实现该接口的类文件路径列表 */
    private List<String> implementationFiles = new ArrayList<>();
    
    /** 信息面板，显示文件位置信息 */
    private InfoPanel infoPanel;
    /** 代码编辑器面板，显示生成的测试代码 */
    private CodeEditorPanel codeEditorPanel;
    /** 结构化结果面板，显示解析后的需求信息 */
    private StructuredResultPanel structuredResultPanel;
    
    /** 初始化会话的 Future，用于异步等待 */
    private CompletableFuture<String> initSessionFuture = null;
    /** 初始化会话是否完成 */
    private boolean sessionInitialized = false;

    /**
     * 构造方法
     * @param project 当前项目
     */
    public TestGeneratorToolWindowPanel(Project project) {
        super(new BorderLayout());
        this.project = project;
        
        // 初始化 UI
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
        
        structuredResultPanel.setGenerateTestButtonEnabled(false);
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
        // 选择范围
        this.selectedText = selectedText;
        
        // 重置文件信息变量
        selectedFilePath = "";
        selectedFileName = "";
        selectedLineNumber = -1;
        isInterfaceFile = false;
        implementationFiles.clear();
        
        // 重置代码区，填充提示注释
        resetCodeEditor();
        
        // 禁用生成测试代码按钮，等待会话初始化完成
        structuredResultPanel.setGenerateTestButtonEnabled(false);
        sessionInitialized = false;
        
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
     * 重置代码编辑器，填充提示注释
     */
    private void resetCodeEditor() {
        if (codeEditorPanel != null) {
            String testCodeHint = "// Click 'Generate Test Code' button to generate test code";
            String emptyMethodHint = "// Click 'Generate Test Code' button to generate empty method code";
            
            codeEditorPanel.setTestCode(testCodeHint);
            codeEditorPanel.setEmptyMethodCode(emptyMethodHint);
            
            generatedEmptyMethodCode = null;
        }
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
            
            // 异步初始化会话
            initSessionAsync();
        });
    }
    
    /**
     * 异步初始化会话
     */
    private void initSessionAsync() {
        ClassContextInfo classInfo = CodeStructureService.getCurrentClassInfo();
        if (classInfo == null) {
            System.out.println("[Init Session] No class context info available");
            return;
        }
        
        initSessionFuture = new CompletableFuture<>();
        sessionInitialized = false;
        
        System.out.println("[Init Session] Starting async session initialization...");
        
        TestCodeService.initSession(
            classInfo,
            result -> {
                if (result.isSuccess()) {
                    sessionInitialized = true;
                    initSessionFuture.complete(result.getSessionId());
                    System.out.println("[Init Session] Session initialized successfully: " + result.getSessionId());
                    
                    ApplicationManager.getApplication().invokeLater(() -> 
                        structuredResultPanel.setGenerateTestButtonEnabled(true),
                        ModalityState.nonModal());
                } else {
                    initSessionFuture.completeExceptionally(new Exception(result.getErrorMessage()));
                    System.out.println("[Init Session] Session initialization failed: " + result.getErrorMessage());
                }
            },
            error -> {
                initSessionFuture.completeExceptionally(new Exception(error));
                System.out.println("[Init Session] Session initialization error: " + error);
            }
        );
    }
    
    /**
     * 异步等待会话初始化完成，支持重试
     * @param maxRetries 最大重试次数
     * @param callback 回调函数，参数为 session_id 或 null（如果失败）
     */
    private void waitForSessionInitAsync(int maxRetries, java.util.function.Consumer<String> callback) {
        if (sessionInitialized && TestCodeService.getCurrentSessionId() != null) {
            callback.accept(TestCodeService.getCurrentSessionId());
            return;
        }
        
        waitForSessionInitRetry(0, maxRetries, callback);
    }
    
    /**
     * 递归重试等待会话初始化
     */
    private void waitForSessionInitRetry(int currentRetry, int maxRetries, java.util.function.Consumer<String> callback) {
        if (currentRetry > 0) {
            System.out.println("[Init Session] Retry " + currentRetry + "/" + maxRetries);
            initSessionAsync();
        }
        
        CompletableFuture.runAsync(() -> {
            try {
                String sessionId = initSessionFuture.get(10, TimeUnit.SECONDS);
                if (sessionId != null && !sessionId.isEmpty()) {
                    SwingUtilities.invokeLater(() -> callback.accept(sessionId));
                } else if (currentRetry < maxRetries) {
                    SwingUtilities.invokeLater(() -> waitForSessionInitRetry(currentRetry + 1, maxRetries, callback));
                } else {
                    SwingUtilities.invokeLater(() -> callback.accept(null));
                }
            } catch (Exception e) {
                System.out.println("[Init Session] Wait failed: " + e.getMessage());
                if (currentRetry < maxRetries) {
                    SwingUtilities.invokeLater(() -> waitForSessionInitRetry(currentRetry + 1, maxRetries, callback));
                } else {
                    SwingUtilities.invokeLater(() -> callback.accept(null));
                }
            }
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
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        
        final JDialog loadingDialog = createLoadingDialog(parentWindow, "Initializing session...");
        loadingDialog.setVisible(true);
        
        String methodName = structuredResultPanel.getMethodName();
        String returnType = structuredResultPanel.getReturnType();
        String expectationsStr = structuredResultPanel.getExpectations();
        String parametersStr = structuredResultPanel.buildParametersJson();
        
        System.out.println("[Test Case Generator] Method Name: " + methodName);
        System.out.println("[Test Case Generator] Return Type: " + returnType);
        System.out.println("[Test Case Generator] Parameters: " + parametersStr);
        System.out.println("[Test Case Generator] Expectations: " + expectationsStr);
        
        waitForSessionInitAsync(2, sessionId -> {
            if (sessionId == null) {
                loadingDialog.dispose();
                ApplicationManager.getApplication().invokeLater(() -> 
                    Messages.showErrorDialog("Failed to initialize session after retries. Please try again.", "Error"),
                    ModalityState.nonModal());
                return;
            }
            
            loadingDialog.setTitle("Generating test code...");
            
            System.out.println("[Test Case Generator] Using session_id: " + sessionId);
            
            TestCodeService.generateTestCodeWithSession(
                sessionId,
                methodName,
                returnType,
                parametersStr,
                expectationsStr,
                result -> {
                    loadingDialog.dispose();
                    
                    if (codeEditorPanel == null) {
                        System.out.println("[Test Case Generator] ERROR: codeEditorPanel is null");
                        ApplicationManager.getApplication().invokeLater(() -> 
                            Messages.showErrorDialog("Error: Code editor not initialized", "Error"),
                            ModalityState.nonModal());
                        return;
                    }
                    
                    String testCode = result.getTestCode();
                    String emptyMethodCode = result.getEmptyMethodCode();
                    
                    try {
                        codeEditorPanel.setTestCode(testCode != null ? testCode : "No test code generated");
                        
                        String emptyMethodText = emptyMethodCode != null && !emptyMethodCode.isEmpty() ? 
                            emptyMethodCode : "No empty method generated";
                        codeEditorPanel.setEmptyMethodCode(emptyMethodText);
                        
                        System.out.println("[Test Case Generator] Editor content updated successfully");
                        
                        if (emptyMethodCode != null && !emptyMethodCode.isEmpty()) {
                            generatedEmptyMethodCode = emptyMethodCode;
                        }
                        
                        ApplicationManager.getApplication().invokeLater(() -> 
                            Messages.showInfoMessage("Test code generated successfully!", "Success"),
                            ModalityState.nonModal());
                    } catch (Exception ex) {
                        System.out.println("[Test Case Generator] ERROR updating editor: " + ex.getMessage());
                        ex.printStackTrace();
                        ApplicationManager.getApplication().invokeLater(() -> 
                            Messages.showErrorDialog("Error updating code editor: " + ex.getMessage(), "Error"),
                            ModalityState.nonModal());
                    }
                },
                errorMessage -> {
                    loadingDialog.dispose();
                    ApplicationManager.getApplication().invokeLater(() -> 
                        Messages.showErrorDialog("Error generating test code: " + errorMessage, "Error"),
                        ModalityState.nonModal());
                }
            );
        });
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
        
        if (codeEditorPanel == null) {
            System.out.println("[Test Case Generator] Code editor not initialized");
            Messages.showErrorDialog("Code editor not initialized", "Error");
            return;
        }
        
        String testCode = codeEditorPanel.getTestCode();
        String emptyMethod = codeEditorPanel.getEmptyMethodCode();
        
        System.out.println("[Test Case Generator] Test code length: " + (testCode != null ? testCode.length() : 0));
        System.out.println("[Test Case Generator] Empty method length: " + (emptyMethod != null ? emptyMethod.length() : 0));
        
        if (testCode == null || testCode.isEmpty()) {
            System.out.println("[Test Case Generator] No test code to compile");
            Messages.showInfoMessage("No test code to compile", "Information");
            return;
        }
        
        if (testCode.contains("Click 'Generate Test Code' button") || 
            testCode.equals("// Click 'Generate Test Code' button to generate test code")) {
            System.out.println("[Test Case Generator] Default text found, no code to compile");
            Messages.showInfoMessage("Please generate test code first before pre-compiling.", "Information");
            return;
        }
        
        String packageName = extractPackageName(testCode);
        String className = extractClassName(testCode);
        
        if (packageName == null || packageName.isEmpty()) {
            packageName = "com.example";
        }
        if (className == null || className.isEmpty()) {
            className = "TestClass";
        }
        
        final String finalPackageName = packageName;
        final String finalClassName = className;
        
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        JDialog loadingDialog = createLoadingDialog(parentWindow, "Pre-compiling test code...");
        loadingDialog.setVisible(true);
        
        ThreadPoolService.getInstance().runInBackground(() -> {
            try {
                String requestBody = buildPreCompileRequest(finalPackageName, finalClassName, emptyMethod, testCode);
                System.out.println("[Test Case Generator] Pre-compile request: " + requestBody.substring(0, Math.min(200, requestBody.length())));
                
                String response = com.smarttestgen.ideaplugin.service.api.ApiService.preCompile(requestBody);
                System.out.println("[Test Case Generator] Pre-compile response: " + response);
                
                com.google.gson.JsonObject responseJson = com.google.gson.JsonParser.parseString(response).getAsJsonObject();
                com.google.gson.JsonObject data = responseJson.getAsJsonObject("data");
                
                boolean success = data.get("success").getAsBoolean();
                String errorMessage = data.has("error_message") && !data.get("error_message").isJsonNull() 
                    ? data.get("error_message").getAsString() : "";
                
                ApplicationManager.getApplication().invokeLater(() -> {
                    loadingDialog.dispose();
                    codeEditorPanel.setCompilationResult(success, errorMessage);
                    
                    if (success) {
                        System.out.println("[Test Case Generator] Pre-compile successful");
                    } else {
                        System.out.println("[Test Case Generator] Pre-compile failed: " + errorMessage);
                    }
                }, ModalityState.nonModal());
                
            } catch (Exception e) {
                System.out.println("[Test Case Generator] Pre-compile error: " + e.getMessage());
                e.printStackTrace();
                ApplicationManager.getApplication().invokeLater(() -> {
                    loadingDialog.dispose();
                    codeEditorPanel.setCompilationResult(false, "Error: " + e.getMessage());
                }, ModalityState.nonModal());
            }
        });
    }
    
    private String extractPackageName(String code) {
        if (code == null) return null;
        for (String line : code.split("\n")) {
            line = line.trim();
            if (line.startsWith("package ")) {
                return line.substring(8).replace(";", "").trim();
            }
        }
        return null;
    }
    
    private String extractClassName(String code) {
        if (code == null) return null;
        for (String line : code.split("\n")) {
            line = line.trim();
            if (line.contains("class ") && line.endsWith("{")) {
                int classIndex = line.indexOf("class ");
                String afterClass = line.substring(classIndex + 6).trim();
                int spaceIndex = afterClass.indexOf(" ");
                int braceIndex = afterClass.indexOf("{");
                int endIndex = Math.min(
                    spaceIndex >= 0 ? spaceIndex : Integer.MAX_VALUE,
                    braceIndex >= 0 ? braceIndex : Integer.MAX_VALUE
                );
                if (endIndex == Integer.MAX_VALUE) {
                    return afterClass.replace("Test", "");
                }
                String className = afterClass.substring(0, endIndex).trim();
                if (className.endsWith("Test")) {
                    className = className.substring(0, className.length() - 4);
                }
                return className;
            }
        }
        return null;
    }
    
    private String buildPreCompileRequest(String packageName, String className, String emptyMethod, String testCode) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"package_name\":\"").append(escapeContent(packageName)).append("\",");
        sb.append("\"class_name\":\"").append(escapeContent(className)).append("\",");
        sb.append("\"empty_method\":\"").append(escapeContent(emptyMethod != null ? emptyMethod : "")).append("\",");
        sb.append("\"test_code\":\"").append(escapeContent(testCode)).append("\"");
        sb.append("}");
        return sb.toString();
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
     * 修复编译错误
     */
    private void fixCompilationError() {
        System.out.println("[Test Case Generator] Fix compilation error button clicked");
        
        if (codeEditorPanel == null) {
            System.out.println("[Test Case Generator] Code editor not initialized");
            ApplicationManager.getApplication().invokeLater(() -> 
                Messages.showErrorDialog("Code editor not initialized", "Error"),
                ModalityState.nonModal());
            return;
        }
        
        String code = codeEditorPanel.getTestCode();
        
        if (code == null || code.isEmpty()) {
            System.out.println("[Test Case Generator] No code to fix");
            ApplicationManager.getApplication().invokeLater(() -> 
                Messages.showWarningDialog("No code in the editor. Please generate test code first.", "Warning"),
                ModalityState.nonModal());
            return;
        }
        
        if (isOnlyComments(code)) {
            System.out.println("[Test Case Generator] Only comments found, no actual code to fix");
            ApplicationManager.getApplication().invokeLater(() -> 
                Messages.showWarningDialog("No actual code in the editor. Please generate test code first.", "Warning"),
                ModalityState.nonModal());
            return;
        }
        
        String errorMessage = codeEditorPanel.getLastCompilationError();
        
        if (errorMessage == null || errorMessage.isEmpty()) {
            System.out.println("[Test Case Generator] No compilation error found");
            ApplicationManager.getApplication().invokeLater(() -> 
                Messages.showWarningDialog("No compilation error found. Please run 'Pre-compile' first to check for errors.", "Warning"),
                ModalityState.nonModal());
            return;
        }
        
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        JDialog loadingDialog = createLoadingDialog(parentWindow, "Fixing compilation errors...");
        loadingDialog.setVisible(true);
        
        String existingSessionId = TestCodeService.getCurrentSessionId();
        if (existingSessionId != null) {
            executeFixCompilationError(existingSessionId, code, errorMessage, loadingDialog);
        } else {
            waitForSessionInitAsync(2, sessionId -> {
                if (sessionId == null) {
                    loadingDialog.dispose();
                    ApplicationManager.getApplication().invokeLater(() -> 
                        Messages.showErrorDialog("Failed to initialize session. Please try again.", "Error"),
                        ModalityState.nonModal());
                    return;
                }
                executeFixCompilationError(sessionId, code, errorMessage, loadingDialog);
            });
        }
    }
    
    private boolean isOnlyComments(String code) {
        if (code == null || code.trim().isEmpty()) {
            return true;
        }
        
        String[] lines = code.split("\n");
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) {
                continue;
            }
            if (!trimmedLine.startsWith("//") && !trimmedLine.startsWith("/*") && !trimmedLine.startsWith("*") && !trimmedLine.endsWith("*/")) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 执行修复编译错误
     */
    private void executeFixCompilationError(String sessionId, String code, String consoleOutput, JDialog loadingDialog) {
        String requestBody = buildFixCompilationErrorRequest(code, consoleOutput, sessionId);
        
        TestCodeService.fixCompilationErrorWithSession(
            requestBody,
            result -> {
                loadingDialog.dispose();
                
                codeEditorPanel.setTestCode("");
                
                String fixedCode = result.getTestCode();
                codeEditorPanel.setTestCode(fixedCode != null ? fixedCode : "Failed to fix compilation error");
                
                ApplicationManager.getApplication().invokeLater(() -> 
                    Messages.showInfoMessage("Compilation error fixed successfully!", "Success"),
                    ModalityState.nonModal());
            },
            errorMessage -> {
                loadingDialog.dispose();
                ApplicationManager.getApplication().invokeLater(() -> 
                    Messages.showErrorDialog("Error fixing compilation error: " + errorMessage, "Error"),
                    ModalityState.nonModal());
            }
        );
    }
    
    /**
     * 构建修复编译错误的请求体（精简版，使用 session_id）
     * @param code 测试代码
     * @param errorMessage 错误信息
     * @param sessionId 会话ID
     * @return 请求体
     */
    private String buildFixCompilationErrorRequest(String code, String errorMessage, String sessionId) {
        StringBuilder requestBody = new StringBuilder();
        requestBody.append("{");
        requestBody.append("\"code\":\"").append(escapeContent(code)).append("\",");
        requestBody.append("\"error_message\":\"").append(escapeContent(errorMessage)).append("\",");
        requestBody.append("\"session_id\":\"").append(escapeContent(sessionId)).append("\"");
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
