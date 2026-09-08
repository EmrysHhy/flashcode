package com.bitejiuyeke.bitecommoncore;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;


public class BeanUtilTests {

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

    //1. 属性类型不一致导致拷贝失败
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

    //2. 底层实现为反射拷贝效率低
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

    //3. BeanUtils.copyProperties是浅拷贝
    @Test
    void copylight(){
        Person sourcePerson = new Person("bite",new Card("123456"));
        Person targetPerson = new Person();
        BeanUtils.copyProperties(sourcePerson, targetPerson);
        sourcePerson.getCard().setNum("654321");
        System.out.println(targetPerson);

    }
    //4. 内部类数据无法成功拷贝

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
    void copyInnerClass(){
        SourcePoJoInner sourcePoJo = new SourcePoJoInner();
        sourcePoJo.setUsername("bite");
        SourcePoJoInner.InnerClass innerClass = new SourcePoJoInner.InnerClass("sourceInner");
        sourcePoJo.innerClass=innerClass;
        System.out.println(sourcePoJo.toString());
        TargetPoJoInner targetPoJo = new TargetPoJoInner();
        BeanUtils.copyProperties(sourcePoJo,targetPoJo);
        System.out.println(targetPoJo.toString());
    }
}
