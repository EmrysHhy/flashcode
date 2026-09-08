package com.bitejiuyeke.bitemstemplateservice.test;

import com.bitejiuyeke.bitemstemplateservice.service.ThreadPoolServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/test/thread")
public class TestThreadPoolController {

    @Autowired
    private ThreadPoolServiceImpl threadPoolService;

    @GetMapping("/info")
    public void info() {
        log.info("TestThreadPoolController thread name: {}", Thread.currentThread().getName());
        threadPoolService.info();
    }
}
