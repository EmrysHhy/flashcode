package com.bitejiuyeke.biteportalservice.flash.config;

import com.bitejiuyeke.bitefileapi.file.feign.FileFeignClient;
import com.bitejiuyeke.biteportalservice.flash.agent.MultiAgentWorkFlow;
import com.bitejiuyeke.biteportalservice.flash.mapper.AppMapper;
import com.bitejiuyeke.biteportalservice.flash.service.IGiteeService;
import com.github.dockerjava.api.DockerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 *
 * @author Emrys
 * content:
 */
@Configuration
@Slf4j
public class MultiAgentConfig {

    @Value("${flashcode.delete-code-expire:12}")
    private Integer deleteCodeExpire;

    /**
     * 启动时组图并 compile，之后各请求复用同一份 CompiledGraph。
     */
    @Bean(name = "multiAgentWorkFlow")
    public MultiAgentWorkFlow multiAgentWorkFlow(ChatClient chatClient,
                                                 VectorStore vectorStore,
                                                 AppMapper appMapper,
                                                 IGiteeService giteeService,
                                                 FileFeignClient fileFeignClient,
                                                 DockerClient dockerClient,
                                                 ScheduledExecutorService scheduledExecutorService) {
        return new MultiAgentWorkFlow(chatClient,
                vectorStore,
                appMapper,
                giteeService,
                fileFeignClient,
                dockerClient,
                deleteCodeExpire,
                scheduledExecutorService);
    }

    /**
     * 延迟删本地代码 / 截图用的共享调度器。
     * 1 个线程即可；destroyMethod 保证进程退出时关掉，不要在业务里 shutdown。
     */
    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService scheduledExecutorService() {
        return Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "flashcode-delay-delete");
            thread.setDaemon(true);
            return thread;
        });
    }
}
