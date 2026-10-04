package com.bitejiuyeke.bitecommoncore;

import com.bitejiuyeke.bitecommoncore.domain.TestUser1;
import com.bitejiuyeke.bitecommoncore.domain.TestUserVO;
import com.bitejiuyeke.bitecommoncore.utils.BeanCopyUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

public class BeanCopyUtilTests {

    @Test
    public void copyTest() {
//        TestUser1 user1 = new TestUser1();
//        user1.setName("张三");
//        user1.setAge(29);
//        user1.setSchool("比特就业课");
//        user1.setSex("男");
//        user1.setQQ("67888");
//        user1.setWeChat("899999");
//        TestUserVO userVO = new TestUserVO();
//        BeanUtils.copyProperties(user1, userVO);
//        System.out.println(user1);
//        System.out.println(userVO);


        TestUser1 user1 = new TestUser1();
        user1.setName("张三");
        user1.setAge(29);
        user1.setSchool("比特就业课");
        user1.setSex("男");
        user1.setQQ("67888");
        user1.setWeChat("899999");
        System.out.println(user1);
        System.out.println(user1.convertToVO());
    }

    @Test
    public void copyListTest() {
        TestUser1 user1 = new TestUser1();
        user1.setName("张三");
        user1.setAge(29);
        user1.setSchool("比特就业课");
        user1.setSex("男");
        user1.setQQ("67888");
        user1.setWeChat("899999");

        TestUser1 user2 = new TestUser1();
        user2.setName("李四");
        user2.setAge(56);
        user2.setSchool("比特就业课");
        user2.setSex("女");
        user2.setQQ("111111");
        user2.setWeChat("44444");

        List<TestUser1> sourceList = new ArrayList<>();
        sourceList.add(user1);
        sourceList.add(user2);
        System.out.println(sourceList);

        List<TestUserVO> testUserVOList = BeanCopyUtil.copyListProperties(sourceList, TestUserVO::new);
        System.out.println(testUserVOList);

        List<TestUserVO> testUserVOList1 = TestUser1.convertToVOList(sourceList);
        System.out.println(testUserVOList1);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    class SourcePoJo{
        private String username;
        private Long id;
    }

    @Data
    class TargetPoJo{
        private String username;
        private String id;
    }

    @Data
    class TargetPoJo2{
        private String username;
        private long id;
    }

    @Test
    void copyfail1() {
        SourcePoJo sourcePoJo = new SourcePoJo("bite", (long) 1000000);
        TargetPoJo targetPoJo = new TargetPoJo();
        BeanUtils.copyProperties(sourcePoJo,targetPoJo);
        System.out.println(targetPoJo);
    }

    @Test
    void copyfail2() {
        SourcePoJo sourcePoJo = new SourcePoJo();
        sourcePoJo.setUsername("bite");
        TargetPoJo2 targetPoJo2 = new TargetPoJo2();
        BeanUtils.copyProperties(sourcePoJo,targetPoJo2);
        System.out.println(targetPoJo2);
    }


    @Test
    void copyslow(){
        long copyStartTime = System.currentTimeMillis();
        SourcePoJo source = new SourcePoJo("bite",1000000L);
        TargetPoJo2 target = new TargetPoJo2();
        for(int i = 0; i < 10000; i++) {
            BeanUtils.copyProperties(source, target);
        }
        System.out.println("copy方式："+(System.currentTimeMillis()-copyStartTime));

        long setStartTime = System.currentTimeMillis();
        for(int i = 0; i < 10000; i++) {
            target.setUsername(source.getUsername());
            target.setId(source.getId());
        }
        System.out.println("set方式："+(System.currentTimeMillis()-setStartTime));

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    class Card {
        private String num;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    class Person {
        private String name;
        private Card card;
    }

    @Test
    void copylight() {
//        Person sourcePerson = new Person("bite", new Card("123456"));  //源bean
//        Person targetPerson = new Person();   //目标bean
//        BeanUtils.copyProperties(sourcePerson, targetPerson);  //浅拷贝
//        System.out.println(targetPerson);  //第一次打印目标bean
//
//        sourcePerson.getCard().setNum("654321");
//        System.out.println(targetPerson); //第二次打印目标bean

        Person sourcePerson = new Person("bite",new Card("123456"));
        Person targetPerson = new Person();
        BeanUtils.copyProperties(sourcePerson, targetPerson);
        Card targetCard = new Card();
        BeanUtils.copyProperties(sourcePerson.getCard(), targetCard);
        targetPerson.setCard(targetCard);
        System.out.println(targetPerson);
        sourcePerson.getCard().setNum("654321");
        System.out.println(targetPerson);
    }

    @Data
    @ToString
    public class SourcePoJoInner{
        private String username;
        private Long id;
        public InnerClass innerClass;
        @Data
        @ToString
        @AllArgsConstructor
        public static class InnerClass{
            public String innerName;
        }
    }

    @Data
    @ToString
    public class TargetPoJoInner{
        private String username;
        private Long id;
        public InnerClass innerClass;
        @Data
        @ToString
        public static class InnerClass{
            public String innerName;
        }
    }

    @Test
    void copyInnerClass() {
//        SourcePoJoInner sourcePoJo = new SourcePoJoInner();
//        sourcePoJo.setUsername("bite");
//        SourcePoJoInner.InnerClass innerClass = new SourcePoJoInner.InnerClass("sourceInner");
//        sourcePoJo.innerClass = innerClass;
//        System.out.println(sourcePoJo);
//
//        TargetPoJoInner targetPoJo = new TargetPoJoInner();
//        BeanUtils.copyProperties(sourcePoJo, targetPoJo);
//        System.out.println(targetPoJo);

        SourcePoJoInner sourcePoJo = new SourcePoJoInner();
        sourcePoJo.setUsername("bite");
        SourcePoJoInner.InnerClass innerClass = new SourcePoJoInner.InnerClass("sourceInner");
        sourcePoJo.innerClass=innerClass;
        System.out.println(sourcePoJo);
        TargetPoJoInner targetPoJo = new TargetPoJoInner();
        TargetPoJoInner.InnerClass targetInnerClass = new TargetPoJoInner.InnerClass();
        BeanUtils.copyProperties(sourcePoJo,targetPoJo);
        BeanUtils.copyProperties(innerClass,targetInnerClass);
        targetPoJo.innerClass = targetInnerClass;
        System.out.println(targetPoJo);
    }

}
