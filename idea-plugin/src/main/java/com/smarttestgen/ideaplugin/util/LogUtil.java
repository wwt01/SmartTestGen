package com.smarttestgen.ideaplugin.util;

public class LogUtil {
    
    private static final String PREFIX = "╔══════════════════════════════════════════════════════════════";
    private static final String SUFFIX = "╚══════════════════════════════════════════════════════════════";
    private static final String INFO_PREFIX = "[SmartTestGen]";
    
    public static void info(String tag, String message) {
        System.out.println(INFO_PREFIX + " [" + tag + "] " + message);
    }
    
    public static void success(String tag, String message) {
        System.out.println(INFO_PREFIX + " [✓ " + tag + "] " + message);
    }
    
    public static void error(String tag, String message) {
        System.err.println(INFO_PREFIX + " [✗ " + tag + "] " + message);
    }
    
    public static void warn(String tag, String message) {
        System.out.println(INFO_PREFIX + " [! " + tag + "] " + message);
    }
    
    public static void section(String title) {
        System.out.println();
        System.out.println(PREFIX);
        System.out.println("║  " + title);
        System.out.println(SUFFIX);
    }
    
    public static void request(String api, String body) {
        System.out.println(INFO_PREFIX + " [→ " + api + "] 请求长度: " + body.length());
        if (body.length() <= 300) {
            System.out.println("    " + body);
        } else {
            System.out.println("    " + body.substring(0, 300) + "...");
        }
    }
    
    public static void response(String api, String body) {
        System.out.println(INFO_PREFIX + " [← " + api + "] 响应长度: " + body.length());
        if (body.length() <= 300) {
            System.out.println("    " + body);
        } else {
            System.out.println("    " + body.substring(0, 300) + "...");
        }
    }
    
    public static void debug(String tag, String message) {
        System.out.println(INFO_PREFIX + " [D " + tag + "] " + message);
    }
}
