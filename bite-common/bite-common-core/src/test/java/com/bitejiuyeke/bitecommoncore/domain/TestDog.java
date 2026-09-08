package com.bitejiuyeke.bitecommoncore.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
public class TestDog extends TestAnimal {

    private int age;

    @Override
    public String toString() {
        return "TestDog{" +
                "name=" + getName() +
                ", age=" + age +
                "}";
    }
}
