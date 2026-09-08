package com.bitejiuyeke.bitemstemplateservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ThreadPoolServiceImpl {

    @Async("threadPoolTaskExecutor")
    public void info() {
        log.info("ThreadPoolServiceImpl thread name: {}", Thread.currentThread().getName());
    }
}
