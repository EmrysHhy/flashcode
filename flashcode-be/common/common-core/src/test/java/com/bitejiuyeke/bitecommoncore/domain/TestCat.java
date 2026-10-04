package com.bitejiuyeke.bitecommoncore.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
public class TestCat extends TestAnimal {

    private String sex;

    @Override
    public String toString() {
        return "TestCat{" +
                "name=" + getName() +
                ", sex='" + sex + '\'' +
                "}";
    }
}
