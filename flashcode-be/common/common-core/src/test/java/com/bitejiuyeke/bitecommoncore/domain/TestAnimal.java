package com.bitejiuyeke.bitecommoncore.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TestDog.class, name = "dog"),
        @JsonSubTypes.Type(value = TestCat.class, name = "cat")
})
public class TestAnimal {

    @JsonProperty("nickName")
    private String name;
}
