package com.smarttestgen.ideaplugin.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

/**
 * 测试代码生成器的 ToolWindow 工厂类
 */
public class TestGeneratorToolWindowFactory implements ToolWindowFactory {
    
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // 创建 ToolWindow 面板
        TestGeneratorToolWindowPanel panel = new TestGeneratorToolWindowPanel(project);
        
        // 创建内容并添加到 ToolWindow
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "测试代码生成器", false);
        toolWindow.getContentManager().addContent(content);
    }
    
    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        // 所有项目都可用
        return true;
    }
}