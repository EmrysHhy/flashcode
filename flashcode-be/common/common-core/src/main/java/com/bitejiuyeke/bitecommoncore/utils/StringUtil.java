package com.bitejiuyeke.bitecommoncore.utils;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.AntPathMatcher;

import java.util.List;

/**
 * 字符串工具类
 */
public class StringUtil {

    /**
     * 判断指定字符串是否与指定匹配规则链表中的任意一个匹配规则匹配
     *
     * @param str 指定字符串
     * @param patternList 匹配规则链表
     * @return 是否匹配
     */
    public static boolean matches(String str, List<String> patternList) {
        if (StringUtils.isEmpty(str) || CollectionUtils.isEmpty(patternList)) {
            return false;
        }
        for (String pattern : patternList) {
            if (isMatch(pattern, str)) {
                return true;
            }
        }
        return false;
    }


    /**
     * 判断url是否与规则匹配
     *
     * 匹配规则：
     * 精确匹配
     * 匹配规则中包含 ? 表示任意单个字符;
     * 匹配规则中包含 * 表示一层路径内的任意字符串，不可跨层级;
     * 匹配规则中包含 ** 表示任意层路径的任意字符，可跨层级
     *
     * @param pattern 匹配规则
     * @param url 需要匹配的url
     * @return 是否匹配
     */
    public static boolean isMatch(String pattern, String url) {
        if (StringUtils.isEmpty(url) || StringUtils.isEmpty(pattern)) {
            return false;
        }
        AntPathMatcher matcher = new AntPathMatcher();
        return matcher.match(pattern, url);
    }
}
