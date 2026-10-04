package com.bitejiuyeke.bitefileservice.config;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyuncs.exceptions.ClientException;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云OSS auto config
 *
 */
@Configuration
@ConditionalOnProperty(value = "storage.type", havingValue = "oss")
// 当括号中条件成立时，微服务启动的时候就会加载当前类。
public class OSSAutoConfiguration {

    /**
     * oss客户端
     */
    public OSSClient ossClient;

    /**
     * 初始化客户端
     * @param prop oss配置
     * @return ossclient
     * @throws ClientException  客户端异常
     */
    @Bean
    public OSSClient ossClient(OSSProperties prop) throws ClientException {
        // ref: https://help.aliyun.com/document_detail/32011.html?spm=a2c4g.32010.0.0.33386a03cVRCNW
//        EnvironmentVariableCredentialsProvider credentialsProvider =
//                CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();

        DefaultCredentialProvider credentialsProvider = CredentialsProviderFactory.newDefaultCredentialProvider(
                prop.getAccessKeyId(), prop.getAccessKeySecret());

        // 创建ClientBuilderConfiguration
        ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
        conf.setSignatureVersion(SignVersion.V4);

        // 使用内网endpoint进行上传 prop.getIntEndpoint()
        if (prop.getInternal()){
            ossClient = (OSSClient) OSSClientBuilder.create()
                    .endpoint(prop.getIntEndpoint())
                    .region(prop.getRegion())
                    .credentialsProvider(credentialsProvider)
                    .clientConfiguration(conf)
                    .build();
        } else {
            ossClient = (OSSClient) OSSClientBuilder.create()
                    .endpoint(prop.getEndpoint())
                    .region(prop.getRegion())
                    .credentialsProvider(credentialsProvider)
                    .clientConfiguration(conf)
                    .build();
        }

        return ossClient;
    }

    /**
     * 关闭客户端
     */
    @PreDestroy
    public void closeOSSClient() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }
}
