# 1. 初始化nacos配置数据
# 注意： 此前如果修改过nacos外接数据库名称，此处需确保名称一致

use `flashcode_nacos_dev`;
INSERT INTO config_info (data_id,group_id,content,md5,gmt_create,gmt_modified,src_user,src_ip,app_name,tenant_id,c_desc,c_use,effect,`type`,c_schema,encrypted_data_key) VALUES
                                                                                                                                                                             ('share-common-dev.yaml','DEFAULT_GROUP','# feign 配置
feign:
  okhttp:
    enabled: true
  httpclient:
    enabled: false
  client:
    config:
      default:
        connectTimeout: 10000
        readTimeout: 10000
  compression:
    request:
      enabled: true
    response:
      enabled: true','aa24e89d40f144f6d7ea2a34a9f868b9',now(),now(),'nacos','112.46.64.96','通用公共配置','flashcode-dev','','','','yaml','',''),
                                                                                                                                                                             ('share-redis-dev.yaml','DEFAULT_GROUP','spring:
  cache:
    type: redis
  data:
    redis:
      host: 192.168.56.107
      port: 6379
      password: bite@123','2f99b238a0dc181c2cca4c1c7e5cf738',now(),now(),'nacos','172.19.0.1','通用Redis公共配置','flashcode-dev','','','','yaml','',''),
                                                                                                                                                                             ('share-mysql-dev.yaml','DEFAULT_GROUP','spring:
  datasource:
    url: jdbc:mysql://192.168.56.107:3306/flashcode_dev?useSSL=false&autoReconnect=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&serverTimezone=GMT%2B8
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: bitedev
    password: bite@123
    # 指定为HikariDataSource
    type: com.zaxxer.hikari.HikariDataSource
    # hikari连接池配置
    hikari:
      #连接池名
      pool-name: HikariCP
      #最小空闲连接数
      minimum-idle: 5
      # 空闲连接存活最大时间，默认10分钟
      idle-timeout: 600000
      # 连接池最大连接数，默认是10
      maximum-pool-size: 10
      # 此属性控制从池返回的连接的默认自动提交行为,默认值：true
      auto-commit: true
      # 此属性控制池中连接的最长生命周期，值0表示无限生命周期，默认30分钟
      max-lifetime: 1800000
      # 数据库连接超时时间,默认30秒
      connection-timeout: 30000
      # 连接测试query
      connection-test-query: SELECT 1
mybatis-plus:
    # 搜索指定包别名
    typeAliasesPackage: com.bitejiuyeke.**.domain
    # 配置mapper的扫描，找到所有的mapper.xml映射文件
    mapperLocations: classpath*:mapper/**.xml
    configuration:
      log-impl: org.apache.ibatis.logging.stdout.StdOutImpl','a4ca7eb86104d59ec129be53c3ad805d',now(),now(),'nacos','172.18.0.1','通用mysql公共配置','flashcode-dev','','','','yaml','',''),
                                                                                                                                                                             ('bite-gateway-dev.yaml','DEFAULT_GROUP','spring:
  cloud:
    gateway:
      discovery:
        locator:
          lowerCaseServiceId: true
          enabled: true
      routes:
        # 用户端服务
        - id: bite-mstemplate
          uri: lb://mstemplate
          predicates:
            - Path=/mstemplate/**
          filters:
            - StripPrefix=1
        # 门户服务
        - id: bite-portal
          uri: lb://bite-portal
          predicates:
            - Path=/portal/**
          filters:
            - StripPrefix=1
        # 鉴权模块
        - id: bite-admin
          uri: lb://bite-admin
          predicates:
            - Path=/admin/**
          filters:
            - StripPrefix=1
        # 文件
        - id: bite-file
          uri: lb://bite-file
          predicates:
            - Path=/file/**
          filters:
            - StripPrefix=1
          metadata:
            response-timeout: 300000
            connect-timeout: 300000

# 安全配置
security:
  # 不校验白名单
  ignore:
    whites:
      - /admin/logout
      - /admin/register
      - /admin/codeLogin
      - /**/login/**
      - /**/send_code/**
      - /**/nologin/**
      - /**/test/**','b6476af4d05245cc366bb2b3771ec17c',now(),now(),'nacos','112.46.64.96','网关','flashcode-dev','','','','yaml','',''),
                                                                                                                                                                             ('share-sms-dev.yaml','DEFAULT_GROUP','sms:
  send-message: false
  send-limit: 200
  code-expiration: 5
  sing-name: "你的短信签名"
  aliyun:
    accessKeyId: 你的阿里云AK
    accessKeySecret: 你的阿里云SK
    endpoint: dysmsapi.aliyuncs.com
    templateCode: 你的阿里云短信模板','b704d125c1dc1f50377bd9e1b3527dc0',now(),now(),'nacos','172.18.0.1','短信通用服务','flashcode-dev','','','','yaml','',''),
                                                                                                                                                                             ('share-map-dev.yaml','DEFAULT_GROUP','map:
  type: qqmap
  enabled: false
  regionenabled: false

qqmap:
  key: 你的腾讯地图KEY
  apiServer: https://apis.map.qq.com','19195726133e9c9cfc420d4064091983',now(),now(),'nacos','112.46.64.96','公共地图配置','flashcode-dev','','','','yaml','',''),
                                                                                                                                                                             ('bite-admin-dev.yaml','DEFAULT_GROUP','# 用戶
appuser:
  info:
    defaultAvatar: https://你的阿里云OSS桶名称.oss-cn-chengdu.aliyuncs.com/你的阿里云OSS路径前缀/web/profile/default-avatar.png','f88f0ecaf1e7a076cbf44df05104e1ce',now(),now(),'nacos','112.46.64.96','基础管理服务','flashcode-dev','','','','yaml','',''),
                                                                                                                                                                             ('share-rabbitmq-dev.yaml','DEFAULT_GROUP','spring:
  rabbitmq:
    port: 5672
    host: 192.168.56.107
    virtual-host: /
    username: admin
    password: bite@123','547905b9da54726573f9a12a50202260',now(),now(),'nacos','112.46.64.96','通用rabbitmq公共配置','flashcode-dev','
','','','yaml','',''),
                                                                                                                                                                             ('bite-file-dev.yaml','DEFAULT_GROUP','storage:
  type: oss

oss:
  internal: ${FILE_UPLOAD_INTERNAL:false}
  endpoint: 你的endpoint
  intEndpoint: 你的intEndpoint
  region: 你的region
  accessKeyId: 你的阿里云AK
  accessKeySecret: 你的阿里云SK
  bucketName: 你的阿里云OSS桶名称
  presignedUrlExpireInMinutes: 60
  pathPrefix: 你的阿里云OSS路径前缀/
  expre: 600
  minLen: 0
  maxLen: 1073741824

spring:
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB','c42244faa84553043e3fc60d2420612b',now(),now(),'nacos','112.46.64.96','文件服务配置','flashcode-dev','','','','yaml','',''),
                                                                                                                                                                             ('bite-portal-dev.yaml','DEFAULT_GROUP','# 微信小程序
wx:
  applet:
    app-id: wxced773bb27a21222
    app-secret: 21d9bf91f89d410868dde7f0b7226433','654d56916263537e391277d77bd8604f',now(),now(),'nacos','112.46.64.96','门户首页服务','flashcode-dev',NULL,NULL,NULL,'yaml',NULL,'');
INSERT INTO config_info (data_id,group_id,content,md5,gmt_create,gmt_modified,src_user,src_ip,app_name,tenant_id,c_desc,c_use,effect,`type`,c_schema,encrypted_data_key) VALUES
    ('share-caffeine-dev.yaml','DEFAULT_GROUP','caffeine:
  build:
    initial-capacity: 128
    maximum-size: 1024
    expire: 60','92e0d0f563d11d8e5e34e8932444ee1e',now(),now(),'nacos','112.46.64.96','本地缓存公共配置','flashcode-dev',NULL,NULL,NULL,'yaml',NULL,'');



INSERT INTO tenant_info (kp,tenant_id,tenant_name,tenant_desc,create_source,gmt_create,gmt_modified) VALUES
    ('1','flashcode-dev','flashcode-dev','测试环境','nacos',unix_timestamp()*1000,unix_timestamp()*1000);
