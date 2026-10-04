package com.bitejiuyeke.bitecommoncore.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class TestUser {

    private String name;

//    private LocalDateTime time;

    private Date time;
//
//    private Map<Date, String> map;

    private LocalDateTime localDateTime;
}
