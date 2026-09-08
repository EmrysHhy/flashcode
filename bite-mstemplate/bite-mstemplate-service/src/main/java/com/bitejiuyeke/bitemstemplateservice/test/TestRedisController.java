package com.bitejiuyeke.bitemstemplateservice.test;

import com.bitejiuyeke.bitecommondomain.domain.R;
import com.bitejiuyeke.bitecommonredis.service.RedisService;
import com.bitejiuyeke.bitemstemplateservice.domain.RegionTest;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/test/redis")
public class TestRedisController {

    @Autowired
    private RedisService redisService;

    @PostMapping("/add")
    public R<Void> add() {
//        redisService.setCacheObject("test", "abc");
//        redisService.setCacheObject("testABC", "abc", 15l, TimeUnit.SECONDS);

//        Boolean aBoolean = redisService.setCacheObjectIfAbsent("demoefg", "efg", 15l, TimeUnit.SECONDS);
//        if (!aBoolean) {
//            return R.fail(ResultCode.FAILED.getCode(), ResultCode.FAILED.getMsg());
//        }

//        User user = new User();
//        user.setName("张三");
//        user.setAge(11);
//        redisService.setCacheObject("userKey", user);


        RegionTest testRegion = new RegionTest();
        testRegion.setId(1L);
        testRegion.setName("北京");
        testRegion.setFullName("北京市");
        testRegion.setCode("110000");

        List<Map<String, RegionTest>> list = new ArrayList<>();
        Map<String, RegionTest> map = new LinkedHashMap<>();
        map.put("beijing", testRegion);
        list.add(map);

        redisService.setCacheObject("testList",list);

        return R.ok();
    }

    @GetMapping("/get")
    public R<Void> get() {
//        String str = redisService.getCacheObject("test", String.class);
//        log.info(str);
//        User user = redisService.getCacheObject("userKey", User.class);
//        log.info("user:{}", user);

        //将redis中的数据获取出来  对象的类型不会产生泛型擦除问题
        List<Map<String, RegionTest>> testList = redisService.getCacheObject("testList", new TypeReference<List<Map<String, RegionTest>>>() {
        });
        System.out.println(testList);
        return R.ok();
    }

    @PostMapping("/list/add")
    public R<Void> listAdd() {
        String key = "listkey";
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        list.add("d");
        list.add("e");
        list.add("c");
        list.add("f");
        list.add("g");
        list.add("c");
        list.add("h");
        list.add("i");
        list.add("c");
        redisService.setCacheList(key, list);
//        redisService.leftPushForList(key, "p");
        return R.ok();
    }

    @DeleteMapping("/list/delete")
    public R<Void> listDel() {
        String key = "listkey";
//        redisService.rightPopForList(key);
//        redisService.removeAllForList(key, "c");
        redisService.removeForAllList(key);
        return R.ok();
    }

    @GetMapping("/list/get")
    public R<Void> listGet() {
        String key = "listkey";
//        List<String> cacheList = redisService.getCacheList(key, String.class);
        List<String> cacheList = redisService.getCacheListByRange(key,6, 2, String.class);
        System.out.println(cacheList);
        return R.ok();
    }

    @PostMapping("/type/add")
    public R<Void> typeAdd() {
//        String key = "setkey";
//        redisService.addMember(key, "a");
//        redisService.addMember(key, "a", "b", "c", "d");

//        String key = "zsetkey";
//        redisService.addMemberZSet(key, "a", 2.9);
//        redisService.addMemberZSet(key, "b", 3.9);
//        redisService.addMemberZSet(key, "c", 13.9);
//        redisService.addMemberZSet(key, "d", 1);

        String key = "user1";   //mapkey
//        User user1 = new User();
//        user1.setName("张三");
//        user1.setAge(77);
//        redisService.setCacheMap(key, JsonUtil.convertToMap(user1));

        redisService.setCacheMapValue(key, "address", "比特就业课");

        String mapKey2 = "user2";
        redisService.setCacheMapValue(mapKey2, "address", "比特就业课");
        return R.ok();
    }

    @DeleteMapping("/type/delete")
    public R<Void> typeDel(){
//        String key = "zsetkey";
//        redisService.removeZSetByScore(key, 1, 3.9);
//        redisService.delMemberZSet(key, "c");

//        String key = "setkey";
//        List<String> keyList = new ArrayList<>();
//        keyList.add("user1");
//        keyList.add("user2");
//        redisService.deleteObject(keyList);
        boolean cad = redisService.cad("delkey", "abc");
        System.out.println(cad);
        return R.ok();
    }

    @GetMapping("/type/get")
    public R<Void> typeGet(){
//        String key = "zsetkey";
//        Set<String> cacheZSet = redisService.getCacheZSetDesc(key, new TypeReference<LinkedHashSet<String>>() {
//        });
//        System.out.println(cacheZSet);
        String key = "user1";
        List<String> hkeys = new ArrayList<>();
        hkeys.add("name");
        hkeys.add("address");
        List<String> multiCacheMapValue = redisService.getMultiCacheMapValue(key, hkeys, new TypeReference<List<String>>() {
        });
        System.out.println(multiCacheMapValue);
        return R.ok();
    }
}
