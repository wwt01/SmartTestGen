package com.smarttestgen.ideaplugin.toolwindow.components;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.smarttestgen.ideaplugin.service.util.ThreadPoolService;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Code editor panel, displaying generated test code and empty method code
 */
public class CodeEditorPanel extends JPanel {
    
    private final Editor testCodeEditor;
    private final Editor emptyMethodEditor;
    private final CardLayout cardLayout;
    private final JPanel codeCardPanel;
    private final JLabel testCodeTab;
    private final JLabel emptyMethodTab;
    private final Font selectedFont;
    private final Font normalFont;
    private boolean isTestCodeSelected = true;
    
    private JTextArea compilationErrorArea;
    private String lastCompilationError = "";
    
    private static final Color SELECTED_BG = JBColor.namedColor("EditorTabs.selectedBackground", JBColor.WHITE);
    private static final Color UNSELECTED_BG = JBColor.namedColor("EditorTabs.background", new JBColor(new Color(240, 240, 240), new Color(60, 63, 65)));
    private static final Color BORDER_COLOR = JBColor.namedColor("EditorTabs.borderColor", JBColor.GRAY);
    
    public CodeEditorPanel(Project project) {
        setLayout(new BorderLayout());
        
        cardLayout = new CardLayout();
        codeCardPanel = new JPanel(cardLayout);
        
        JPanel testCodePanel = new JPanel(new BorderLayout());
        testCodePanel.setBorder(BorderFactory.createTitledBorder("Generated Test Code"));
        
        JPanel emptyMethodPanel = new JPanel(new BorderLayout());
        emptyMethodPanel.setBorder(BorderFactory.createTitledBorder("Generated Empty Method"));
        
        testCodeEditor = createJavaEditor(project);
        emptyMethodEditor = createJavaEditor(project);
        
        if (testCodeEditor != null) {
            JComponent testEditorComponent = testCodeEditor.getComponent();
            JBScrollPane testScrollPane = new JBScrollPane(testEditorComponent);
            testScrollPane.setPreferredSize(new Dimension(800, 250));
            testCodePanel.add(testScrollPane, BorderLayout.CENTER);
        }
        
        if (emptyMethodEditor != null) {
            JComponent emptyMethodEditorComponent = emptyMethodEditor.getComponent();
            JBScrollPane emptyMethodScrollPane = new JBScrollPane(emptyMethodEditorComponent);
            emptyMethodScrollPane.setPreferredSize(new Dimension(800, 250));
            emptyMethodPanel.add(emptyMethodScrollPane, BorderLayout.CENTER);
        }
        
        codeCardPanel.add(testCodePanel, "TestCode");
        codeCardPanel.add(emptyMethodPanel, "EmptyMethod");
        
        normalFont = JBUI.Fonts.create("Dialog", 13);
        selectedFont = JBUI.Fonts.create("Dialog", 15).deriveFont(Font.BOLD);
        
        JPanel tabPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        tabPanel.setOpaque(false);
        
        testCodeTab = createTabLabel("Test Code", true);
        emptyMethodTab = createTabLabel("Empty Method", false);
        
        tabPanel.add(testCodeTab);
        tabPanel.add(Box.createHorizontalStrut(5));
        tabPanel.add(emptyMethodTab);
        
        JPanel errorPanel = createCompilationErrorPanel();
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(tabPanel, BorderLayout.NORTH);
        centerPanel.add(codeCardPanel, BorderLayout.CENTER);
        centerPanel.add(errorPanel, BorderLayout.SOUTH);
        
        add(centerPanel, BorderLayout.CENTER);
    }
    
    private JPanel createCompilationErrorPanel() {
        JPanel errorPanel = new JPanel(new BorderLayout());
        errorPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(JBColor.RED, 1),
            "Compilation Result"
        ));
        
        compilationErrorArea = new JTextArea();
        compilationErrorArea.setEditable(false);
        compilationErrorArea.setFont(JBUI.Fonts.create("Monospaced", 12));
        compilationErrorArea.setBackground(new JBColor(new Color(255, 245, 245), new Color(45, 45, 48)));
        compilationErrorArea.setForeground(new JBColor(JBColor.RED, new Color(255, 100, 100)));
        compilationErrorArea.setRows(5);
        compilationErrorArea.setText("No compilation result yet. Click 'Pre-compile' to check.");
        
        JBScrollPane errorScrollPane = new JBScrollPane(compilationErrorArea);
        errorScrollPane.setPreferredSize(new Dimension(800, 100));
        
        errorPanel.add(errorScrollPane, BorderLayout.CENTER);
        
        return errorPanel;
    }
    
    public void setCompilationResult(boolean success, String errorMessage) {
        if (success) {
            compilationErrorArea.setForeground(new JBColor(new Color(0, 128, 0), new Color(100, 200, 100)));
            compilationErrorArea.setText("✓ Compilation successful! No errors found.");
            lastCompilationError = "";
        } else {
            compilationErrorArea.setForeground(new JBColor(JBColor.RED, new Color(255, 100, 100)));
            compilationErrorArea.setText(errorMessage != null ? errorMessage : "Compilation failed.");
            lastCompilationError = errorMessage != null ? errorMessage : "";
        }
    }
    
    public String getLastCompilationError() {
        return lastCompilationError;
    }
    
    public boolean hasCompilationError() {
        return lastCompilationError != null && !lastCompilationError.isEmpty();
    }
    
    private JLabel createTabLabel(String text, boolean selected) {
        JLabel label = new JLabel(text);
        label.setOpaque(true);
        label.setBackground(selected ? SELECTED_BG : UNSELECTED_BG);
        label.setFont(selected ? selectedFont : normalFont);
        label.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 1, selected ? 2 : 1, 1, BORDER_COLOR),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        label.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (label == testCodeTab && !isTestCodeSelected) {
                    selectTestCodeTab();
                } else if (label == emptyMethodTab && isTestCodeSelected) {
                    selectEmptyMethodTab();
                }
            }
        });
        
        return label;
    }
    
    private void selectTestCodeTab() {
        isTestCodeSelected = true;
        cardLayout.show(codeCardPanel, "TestCode");
        
        testCodeTab.setBackground(SELECTED_BG);
        testCodeTab.setFont(selectedFont);
        testCodeTab.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 1, 2, 1, BORDER_COLOR),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        
        emptyMethodTab.setBackground(UNSELECTED_BG);
        emptyMethodTab.setFont(normalFont);
        emptyMethodTab.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 1, 1, 1, BORDER_COLOR),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
    }
    
    private void selectEmptyMethodTab() {
        isTestCodeSelected = false;
        cardLayout.show(codeCardPanel, "EmptyMethod");
        
        emptyMethodTab.setBackground(SELECTED_BG);
        emptyMethodTab.setFont(selectedFont);
        emptyMethodTab.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 1, 2, 1, BORDER_COLOR),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        
        testCodeTab.setBackground(UNSELECTED_BG);
        testCodeTab.setFont(normalFont);
        testCodeTab.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 1, 1, 1, BORDER_COLOR),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
    }
    
    private Editor createJavaEditor(Project project) {
        if (project == null) {
            return null;
        }
        
        try {
            com.intellij.openapi.editor.EditorFactory editorFactory = 
                com.intellij.openapi.editor.EditorFactory.getInstance();
            com.intellij.openapi.editor.Document document = 
                editorFactory.createDocument("");
            
            com.intellij.openapi.fileTypes.FileType javaFileType = 
                com.intellij.openapi.fileTypes.FileTypeManager.getInstance().getFileTypeByExtension("java");
            
            return editorFactory.createEditor(document, project, javaFileType, false);
        } catch (Exception e) {
            System.out.println("[Test Case Generator] Error creating editor: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    public Editor getTestCodeEditor() {
        return testCodeEditor;
    }
    
    public Editor getEmptyMethodEditor() {
        return emptyMethodEditor;
    }
    
    public void setTestCode(String code) {
        if (testCodeEditor != null) {
            String normalizedCode = normalizeLineSeparators(code);
            ThreadPoolService.getInstance().runInWriteAction(() -> {
                testCodeEditor.getDocument().setText(normalizedCode != null ? normalizedCode : "");
            });
        }
    }
    
    public void setEmptyMethodCode(String code) {
        if (emptyMethodEditor != null) {
            String normalizedCode = normalizeLineSeparators(code);
            ThreadPoolService.getInstance().runInWriteAction(() -> {
                emptyMethodEditor.getDocument().setText(normalizedCode != null ? normalizedCode : "");
            });
        }
    }
    
    private String normalizeLineSeparators(String text) {
        if (text == null) {
            return null;
        }
        return text.replace("\r\n", "\n").replace("\r", "\n");
    }
    
    public String getTestCode() {
        if (testCodeEditor != null) {
            return testCodeEditor.getDocument().getText();
        }
        return "";
    }
    
    public String getEmptyMethodCode() {
        if (emptyMethodEditor != null) {
            return emptyMethodEditor.getDocument().getText();
        }
        return "";
    }
}
