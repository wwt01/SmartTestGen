package com.smarttestgen.ideaplugin.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;

import javax.swing.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * 线程池管理服务类
 * 负责管理项目中的线程使用，确保后台操作在适当的线程中执行
 */
public class ThreadPoolService {
    
    private static ThreadPoolService instance;
    
    private final ExecutorService backgroundExecutor;
    
    private ThreadPoolService() {
        backgroundExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> {
                Thread thread = new Thread(r, "Test-Case-Generator-Background");
                thread.setDaemon(true);
                return thread;
            }
        );
    }
    
    /**
     * 获取单例实例
     * @return ThreadPoolService 实例
     */
    public static synchronized ThreadPoolService getInstance() {
        if (instance == null) {
            instance = new ThreadPoolService();
        }
        return instance;
    }
    
    /**
     * 在后台线程中执行任务
     * @param task 要执行的任务
     */
    public void runInBackground(Runnable task) {
        backgroundExecutor.submit(task);
    }
    
    /**
     * 在后台线程中执行任务，并在完成后回调
     * @param task 要执行的任务
     * @param onSuccess 成功回调（在 EDT 线程中执行）
     * @param onError 错误回调（在 EDT 线程中执行）
     */
    public void runInBackground(Runnable task, Consumer<Void> onSuccess, Consumer<Exception> onError) {
        CompletableFuture.runAsync(() -> {
            try {
                task.run();
                if (onSuccess != null) {
                    SwingUtilities.invokeLater(() -> onSuccess.accept(null));
                }
            } catch (Exception e) {
                if (onError != null) {
                    SwingUtilities.invokeLater(() -> onError.accept(e));
                } else {
                    e.printStackTrace();
                }
            }
        }, backgroundExecutor);
    }
    
    /**
     * 在后台线程中执行计算任务，并在完成后回调
     * @param task 要执行的计算任务
     * @param onSuccess 成功回调（在 EDT 线程中执行）
     * @param onError 错误回调（在 EDT 线程中执行）
     * @param <T> 结果类型
     */
    public <T> void computeInBackground(Computable<T> task, Consumer<T> onSuccess, Consumer<Exception> onError) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return task.compute();
            } catch (Exception e) {
                if (onError != null) {
                    SwingUtilities.invokeLater(() -> onError.accept(e));
                } else {
                    e.printStackTrace();
                }
                return null;
            }
        }, backgroundExecutor).thenAccept(result -> {
            if (result != null && onSuccess != null) {
                SwingUtilities.invokeLater(() -> onSuccess.accept(result));
            }
        });
    }
    
    /**
     * 在 EDT 线程中执行任务
     * @param task 要执行的任务
     */
    public void runInEdt(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }
    
    /**
     * 在读取操作中执行任务
     * @param task 要执行的任务
     */
    public void runInReadAction(Runnable task) {
        ApplicationManager.getApplication().runReadAction(task);
    }
    
    /**
     * 在读取操作中执行计算任务
     * @param task 要执行的计算任务
     * @param <T> 结果类型
     * @return 计算结果
     */
    public <T> T computeInReadAction(Computable<T> task) {
        return ApplicationManager.getApplication().runReadAction(task);
    }
    
    /**
     * 在写入操作中执行任务
     * @param task 要执行的任务
     */
    public void runInWriteAction(Runnable task) {
        ApplicationManager.getApplication().runWriteAction(task);
    }
    
    /**
     * 在写入操作中执行计算任务
     * @param task 要执行的计算任务
     * @param <T> 结果类型
     * @return 计算结果
     */
    public <T> T computeInWriteAction(Computable<T> task) {
        return ApplicationManager.getApplication().runWriteAction(task);
    }
    
    /**
     * 使用进度指示器执行后台任务
     * @param project 项目
     * @param title 任务标题
     * @param task 要执行的任务
     * @param onSuccess 成功回调（在 EDT 线程中执行）
     * @param onError 错误回调（在 EDT 线程中执行）
     */
    public void runWithProgress(Project project, String title, Runnable task, 
                               Consumer<Void> onSuccess, Consumer<Exception> onError) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, title) {
            @Override
            public void run(ProgressIndicator indicator) {
                try {
                    task.run();
                    if (onSuccess != null) {
                        SwingUtilities.invokeLater(() -> onSuccess.accept(null));
                    }
                } catch (Exception e) {
                    if (onError != null) {
                        SwingUtilities.invokeLater(() -> onError.accept(e));
                    } else {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
    
    /**
     * 关闭线程池
     */
    public void shutdown() {
        backgroundExecutor.shutdown();
    }
}
