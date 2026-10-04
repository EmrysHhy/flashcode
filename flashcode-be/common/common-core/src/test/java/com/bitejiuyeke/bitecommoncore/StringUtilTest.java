package com.bitejiuyeke.bitecommoncore;

import com.bitejiuyeke.bitecommoncore.utils.StringUtil;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class StringUtilTest {

    @Test
    public void isMatchTests() {
//        精确匹配
//        String pattern = "/sys/bc";
//        System.out.println(StringUtil.isMatch(pattern, "/sys/bc"));  //true
//        System.out.println(StringUtil.isMatch(pattern,"/sys/abc"));  //false

//      匹配规则中包含 ?  表示单个任意字符;
//        String pattern = "/sys/?bc";
//        System.out.println(StringUtil.isMatch(pattern,"/sys/abc"));  //true
//        System.out.println(StringUtil.isMatch(pattern,"/sys/cbc"));  //true
//        System.out.println(StringUtil.isMatch(pattern,"/sys/（bc"));  //true
//        System.out.println(StringUtil.isMatch(pattern,"/sys/acbc"));  //false
//        System.out.println(StringUtil.isMatch(pattern,"/sdsa/abc"));  //false
//        System.out.println(StringUtil.isMatch(pattern,"/sys/abcw")); //false
//        System.out.println(StringUtil.isMatch(pattern,"/sys/abc/w")); //false

//        匹配规则中包含：通配符 ‘*‘ 表示一层路径内的任意字符串，不可跨层级;  在url中一个 / 就是一个层级
//        String pattern = "/sys/*/bc";
//        System.out.println(StringUtil.isMatch(pattern,"/sys/a/bc"));  //true
//        System.out.println(StringUtil.isMatch(pattern,"/sys/sdasdsadsad%……&*（￥%……&/bc"));  //true
//        System.out.println(StringUtil.isMatch(pattern,"/sys/a/b/bc"));  //false
//        System.out.println(StringUtil.isMatch(pattern,"/b/bc"));  //false
//        System.out.println(StringUtil.isMatch(pattern,"/sys/a"));  //false

//      匹配规则中包含 双通配符 ’**‘  表示任意层路径的任意字符，可跨层级
        String pattern = "/sys/**/bc";
//        System.out.println(StringUtil.isMatch(pattern, "/sys/a/bc"));   //true
//        System.out.println(StringUtil.isMatch(pattern, "/sys/sdasdsadsad/bc")); //true
//        System.out.println(StringUtil.isMatch(pattern, "/sys/a/b/bc"));  //true
//        System.out.println(StringUtil.isMatch(pattern, "/sys/a/b/s/23/sd//sda/sd/ad/sa//dsdd/////f////d/432/fdsf///bc"));  //true
//        System.out.println(StringUtil.isMatch(pattern, "/a/b/s/23/432/fdsf///bc"));  //false
//        System.out.println(StringUtil.isMatch(pattern, "/sys/a/b/s/23/432/fdsf///")); //false
        System.out.println(StringUtil.isMatch("", null));
    }

    @Test
    public void matchesTests() {
        String pattern1 = "/admin/**/bc";
        String pattern2 = "/file/**/bc";
        List<String> patternList = new ArrayList<>();   //匹配规则列表
        patternList.add(pattern1);
        patternList.add(pattern2);
        System.out.println(StringUtil.matches("/admin/ew/wqe/wqeqw/eqwe/bc", patternList));  //true
        System.out.println(StringUtil.matches("/file/sewq/ewq/ewqeqwe/eqwe23/323/bc", patternList)); //true
        System.out.println(StringUtil.matches("a/a/bc", patternList));  //false
    }
}
