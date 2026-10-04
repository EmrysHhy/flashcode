package com.bitejiuyeke.biteportalservice.flash.config;

import com.bitejiuyeke.bitecommondomain.exception.ServiceException;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
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

        String certPath = resolveCertPath(dockerCertPath);
        log.info("Docker cert path: {}", certPath);

        DefaultDockerClientConfig clientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost)
                .withDockerTlsVerify(true)
                .withDockerCertPath(certPath)
                .build();

        DockerHttpClient httpClient = new ZerodepDockerHttpClient.Builder()
                .dockerHost(clientConfig.getDockerHost())
                .sslConfig(clientConfig.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofMinutes(5))
                .build();

        return DockerClientImpl.getInstance(clientConfig, httpClient);
    }

    /**
     * Nacos 里一般是容器路径 /workspace/cert。IDEA 本地启动没有该目录，回退到仓库里的证书。
     */
    private String resolveCertPath(String configured) {
        if (isValidCertDir(configured)) {
            return configured;
        }
        Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (int i = 0; i < 8 && dir != null; i++) {
            Path candidate = dir.resolve("deploy/dev/app/config/cert");
            if (isValidCertDir(candidate.toString())) {
                log.warn("docker.cert-path [{}] 不存在，改用本地证书目录 {}", configured, candidate);
                return candidate.toString();
            }
            dir = dir.getParent();
        }
        throw new ServiceException(
                "Docker 证书目录不存在: " + configured
                        + "。容器部署需挂载 /root/emrys-java/flashcode/deploy/dev/app/config/cert -> /workspace/cert；"
                        + "IDEA 本地启动请确认仓库 deploy/dev/app/config/cert 下有 ca.pem、cert.pem、key.pem"
        );
    }

    private static boolean isValidCertDir(String path) {
        if (StringUtils.isBlank(path)) {
            return false;
        }
        Path dir = Path.of(path);
        return Files.isDirectory(dir)
                && Files.isRegularFile(dir.resolve("ca.pem"))
                && Files.isRegularFile(dir.resolve("cert.pem"))
                && Files.isRegularFile(dir.resolve("key.pem"));
    }
}
