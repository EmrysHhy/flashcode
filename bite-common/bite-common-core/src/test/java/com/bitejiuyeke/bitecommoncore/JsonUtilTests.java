package com.bitejiuyeke.bitecommoncore;

import com.bitejiuyeke.bitecommoncore.domain.TestRegion;
import com.bitejiuyeke.bitecommoncore.utils.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonUtilTests {

    // 对象转字符串测试
    @Test
    void obj2StringTest() {
        TestRegion sysRegion = new TestRegion();
        sysRegion.setFullName("北京市");
        sysRegion.setName("北京");
        sysRegion.setCode("110000");
        sysRegion.setId(1L);
        String res = JsonUtil.obj2String(sysRegion);

        //{"id":1,"name":"北京","fullName":"北京市","code":"110000"}
        Assertions.assertEquals(res,"{\"id\":1,\"name\":\"北京\",\"fullName\":\"北京市\",\"code\":\"110000\"}");
        System.out.println(res);
    }

    // 字符串转对象测试
    @Test
    void string2ObjTest() {

        //{"id":1,"name":"北京","fullName":"北京市","code":"110000"}
        TestRegion region = JsonUtil.string2Obj("{\"id\":1,\"name\":\"北京\",\"fullName\":\"北京市\",\"code\":\"110000\"}", TestRegion.class);

        Assertions.assertEquals(region.getName(),"北京");
    }

    // 字符串转List<基本对象>测试
    @Test
    void string2ListTest(){
        TestRegion sysRegion = new TestRegion();
        sysRegion.setFullName("北京市");
        sysRegion.setName("北京");
        sysRegion.setCode("110000");
        sysRegion.setId(1L);

        List<TestRegion> regions = new ArrayList<>();
        regions.add(sysRegion);

        String res = JsonUtil.obj2String(regions);
        Assertions.assertEquals(res,"[{\"id\":1,\"name\":\"北京\",\"fullName\":\"北京市\",\"code\":\"110000\"}]");
        System.out.println(res);

        // list使用obj转换
        //[{"id":1,"name":"北京","fullName":"北京市","code":"110000"}]
        List<TestRegion> sysRegions = JsonUtil.string2Obj("[{\"id\":1,\"name\":\"北京\",\"fullName\":\"北京市\",\"code\":\"110000\"}]",List.class);

        // 简单list对象转换
        //[{"id":1,"name":"北京","fullName":"北京市","code":"110000"}]
        List<TestRegion> list =JsonUtil.string2List("[{\"id\":1,\"name\":\"北京\",\"fullName\":\"北京市\",\"code\":\"110000\"}]", TestRegion.class);
        Assertions.assertEquals(list.get(0).getName(),"北京");
    }

    // 字符串转复杂模版对象测试
    @Test
    void string2ComplexList(){
        TestRegion sysRegion = new TestRegion();
        sysRegion.setFullName("北京市");
        sysRegion.setName("北京");
        sysRegion.setCode("110000");
        sysRegion.setId(1L);

        List<Map<Long, TestRegion>> regions = new ArrayList<>();
        Map<Long, TestRegion> idRegion = new LinkedHashMap<>();
        idRegion.put(sysRegion.getId(),sysRegion);
        regions.add(idRegion);

        String res = JsonUtil.obj2String(regions);
        System.out.println(res);

        //[{"1":{"id":1,"name":"北京","fullName":"北京市","code":"110000"}}]
        List<Map<Long, TestRegion>> regionmap =JsonUtil.string2Obj("[{\"1\":{\"id\":1,\"name\":\"北京\",\"fullName\":\"北京市\",\"code\":\"110000\"}}]",new TypeReference<List<Map<Long, TestRegion>>>(){});
        Assertions.assertEquals(regionmap.get(0).get(1L).getName(),"北京");
    }
}
