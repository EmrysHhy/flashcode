package com.bitejiuyeke.biteportalservice.flash.config;

import com.bitejiuyeke.bitecommondomain.exception.ServiceException;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 创建可注入的 DockerClient，连接虚拟机上的 Docker（TLS 2376）。
 */
@Slf4j
@Configuration
public class DockerClientConfig {

    @Value("${docker.host}")
    private String dockerHost;

    @Value("${docker.cert-path}")
    private String dockerCertPath;

    @Bean(destroyMethod = "close")
    public DockerClient dockerClient() {
        log.info("Docker host: {}", dockerHost);
        if (StringUtils.isBlank(dockerHost)) {
            throw new ServiceException("Docker host is not set");
        }

        DefaultDockerClientConfig clientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost)
                .withDockerTlsVerify(true)
                .withDockerCertPath(dockerCertPath)
                .build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(clientConfig.getDockerHost())
                .sslConfig(clientConfig.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofMinutes(5))
                .build();

        return DockerClientImpl.getInstance(clientConfig, httpClient);
    }
}
