package com.bitejiuyeke.bitecommoncore.domain;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
public class TestUser1 {

    private String name;

    private int age;

    private String sex;

    private String school;

    private String weChat;

    private String QQ;

    public TestUserVO convertToVO() {
        TestUserVO userVO = new TestUserVO();
        BeanUtils.copyProperties(this, userVO);
        userVO.setNickName(this.name);
        userVO.setContactInfo(this.weChat + "--" + this.QQ);
        return userVO ;
    }

    public static List<TestUserVO> convertToVOList(List<TestUser1> testUser1List) {
        if (testUser1List == null || testUser1List.size() <= 0) {
            return null;
        }
        List<TestUserVO> testUserVOList = new ArrayList<>(testUser1List.size());
        for (TestUser1 user1 : testUser1List) {
            TestUserVO userVO = new TestUserVO();
            BeanUtils.copyProperties(user1,userVO);
            userVO.setNickName(user1.getName());
            userVO.setContactInfo(user1.getWeChat() + "--" + user1.getQQ());
            testUserVOList.add(userVO);
        }
        return testUserVOList;
    }
}
