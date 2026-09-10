package com.bitejiuyeke.biteportalservice.flash.utils;

import com.bitejiuyeke.biteportalservice.flash.enums.AppTypesEnum;

import java.util.LinkedHashMap;
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

    private static final Pattern FILE_PATTERN = Pattern.compile(
            "FILE:\\s*(.+?)\\r?\\n```[^\\n]*\\r?\\n(.*?)\\r?\\n```",
            Pattern.DOTALL
    );

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
     * 解析模型输出中的文件。
     * key 为相对路径，value 为文件内容。
     */
    public static Map<String, String> getFiles(String result) {
        Map<String, String> files = new LinkedHashMap<>();
        if (result == null || result.isBlank()) {
            return files;
        }
        Matcher matcher = FILE_PATTERN.matcher(result);
        while (matcher.find()) {
            String path = matcher.group(1).trim();
            String content = matcher.group(2);
            if (path.isEmpty()) {
                continue;
            }
            files.put(path, content);
        }
        return files;
    }
}
