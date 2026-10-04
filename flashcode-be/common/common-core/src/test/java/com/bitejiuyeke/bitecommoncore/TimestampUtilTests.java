package com.bitejiuyeke.bitecommoncore;

import com.bitejiuyeke.bitecommoncore.utils.TimestampUtil;
import org.junit.jupiter.api.Test;

public class TimestampUtilTests {

    @Test
    public void timeTest() {
//        System.out.println(TimestampUtil.getCurrentSeconds());
//        System.out.println(TimestampUtil.getCurrentMillis());


//        System.out.println(TimestampUtil.getYearLaterSeconds(1));
//        System.out.println(TimestampUtil.getYearLaterMillis(1));

//        System.out.println(TimestampUtil.calculateDifferenceSeconds(1735203429, 1735203489));

        System.out.println(TimestampUtil.calculateDifferenceMillis(1735203525000L, 1735203585000L));
    }
}
