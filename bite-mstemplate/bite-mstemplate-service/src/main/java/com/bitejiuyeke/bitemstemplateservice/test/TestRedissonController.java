package com.bitejiuyeke.bitemstemplateservice.test;

import com.bitejiuyeke.bitecommonredis.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
@RequestMapping("/test/redisson")
public class TestRedissonController {

    @Autowired
    private RedisService redisService;

    //问题1:锁未设置有效时间
    //问题2：减库存操作时非原子  （非主要矛盾）
    //问题3：A线程释放了B线程的锁
    //问题4；解锁逻辑是非原子操作  可以使用lua脚本处理
    //问题5：锁的有效时间很难设置一个合理有效的时间     看门狗


    @PostMapping("/delStock")
    public String delStock() {
        String proKey = "proKey";
        String uuid = UUID.randomUUID().toString();  //唯一  作为身份标识
        Boolean save = redisService.setCacheObjectIfAbsent(proKey, uuid, 30, TimeUnit.SECONDS); //加锁
        if (!save) {
            return "unlock";  //未获取到锁
        }
        try {
            //获取库存
            String stockKey = "stock";
            Integer stock = redisService.getCacheObject(stockKey, Integer.class);
            if (stock <= 0) {
                return "error";  //秒杀失败
            }
            stock--;
            redisService.setCacheObject(stockKey, stock);
        } finally {
            redisService.cad(proKey, uuid);
        }
        return "ok";   //秒杀成功

    }
}
