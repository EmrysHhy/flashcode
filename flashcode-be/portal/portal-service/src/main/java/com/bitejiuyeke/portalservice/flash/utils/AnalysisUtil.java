package com.bitejiuyeke.portalservice.flash.utils;

import com.bitejiuyeke.portalservice.flash.enums.AppTypesEnum;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Emrys
 * content:
 */
public class AnalysisUtil {

    private static final Pattern APP_TYPE_PATTERN = Pattern.compile(
            "APP_TYPE\\s*=\\s*(\\S+)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * 从第一行 APP_TYPE= 解析类型，仅作兜底。
     */
    public static AppTypesEnum getType(String result) {
        if (result == null || result.isBlank()) {
            return AppTypesEnum.Html;
        }
        Matcher matcher = APP_TYPE_PATTERN.matcher(result);
        if (matcher.find()) {
            return AppTypesEnum.of(matcher.group(1));
        }
        return AppTypesEnum.Html;
    }

    /**
     * 优先根据生成文件后缀判断类型，与课上 ModelOutputParser 规则一致。
     */
    public static AppTypesEnum determineTypeFromFiles(Map<String, String> files) {
        if (files == null || files.isEmpty()) {
            return null;
        }
        if (files.size() == 1) {
            String singleFile = files.keySet().iterator().next();
            if (singleFile.toLowerCase().endsWith(".html")) {
                return AppTypesEnum.Html;
            }
        }
        boolean hasJava = files.keySet().stream()
                .anyMatch(path -> path.toLowerCase().endsWith(".java"));
        boolean hasVue = files.keySet().stream()
                .anyMatch(path -> path.toLowerCase().endsWith(".vue"));
        if (hasJava && hasVue) {
            return AppTypesEnum.Spring_Vue3;
        }
        if (hasVue) {
            return AppTypesEnum.Vue3;
        }
        return null;
    }

    /**
     * 先按文件推断类型，推断不了再用 APP_TYPE 行。
     */
    public static AppTypesEnum resolveType(String appCode, Map<String, String> files) {
        AppTypesEnum fromFiles = determineTypeFromFiles(files);
        if (fromFiles != null) {
            return fromFiles;
        }
        return getType(appCode);
    }

    /**
     * 解析模型输出中的文件。
     * key 为相对路径，value 为文件内容。
     */
    public static Map<String, String> getFiles(String result) {
        Map<String, String> files = new LinkedHashMap<>();
        if (result == null || result.isBlank()) {
            return files;
        }
        String normalized = stripThinkingBlocks(result);
        List<String> lines = Arrays.asList(normalized.split("\r?\n"));

        String currentPath = null;
        StringBuilder buf = null;
        boolean inFence = false;
        for (String line : lines) {
            if (line.startsWith("FILE:")) {
                if (currentPath != null && buf != null) {
                    files.put(currentPath, buf.toString());
                }
                currentPath = line.substring("FILE:".length()).trim();
                buf = new StringBuilder();
                inFence = false;
                continue;
            }
            if (line.startsWith("```")) {
                inFence = !inFence;
                continue;
            }
            if (inFence && buf != null) {
                buf.append(line).append('\n');
            }
        }
        if (currentPath != null && buf != null) {
            files.put(currentPath, buf.toString());
        }
        return files;
    }

    private static String stripThinkingBlocks(String output) {
        return output.replaceAll("(?s)<think>.*?</think>", "");
    }
}
