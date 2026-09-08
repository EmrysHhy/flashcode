package com.bitejiuyeke.bitemstemplateservice.test;

import com.bitejiuyeke.bitecommoncache.utils.CacheUtil;
import com.bitejiuyeke.bitecommondomain.domain.R;
import com.bitejiuyeke.bitecommonredis.service.RedisService;
import com.bitejiuyeke.bitemstemplateservice.domain.RegionTest;
import com.bitejiuyeke.bitemstemplateservice.service.IClothService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/test/cache")
public class TestCacheController {

    @Autowired
    private RedisService redisService;

    @Autowired
    private Cache<String, Object> caffeineCache;

    @Autowired
    private IClothService clothService;


    @GetMapping("/get")
    public R<Void> get() {
        String key = "testKey";
        CacheUtil.getL2Cache(redisService, key,  new TypeReference<List<Map<String, RegionTest>>>() {}, caffeineCache);
        return R.ok();
    }

    @GetMapping("/cloth/get")
    public R<Integer> clothGet(Long proId) {
        return R.ok(clothService.clothPriceGet(proId));
    }
}
