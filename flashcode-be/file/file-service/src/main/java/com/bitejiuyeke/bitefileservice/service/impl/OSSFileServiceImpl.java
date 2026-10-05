package com.bitejiuyeke.bitefileservice.service.impl;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.bitejiuyeke.bitecommondomain.domain.ResultCode;
import com.bitejiuyeke.bitecommondomain.exception.ServiceException;
import com.bitejiuyeke.bitefileservice.config.OSSProperties;
import com.bitejiuyeke.bitefileservice.constants.OSSCustomConstants;
import com.bitejiuyeke.bitefileapi.file.domain.vo.FileVO;
import com.bitejiuyeke.bitefileapi.file.domain.vo.SignVO;
import com.bitejiuyeke.bitefileservice.service.IFileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.InputStream;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
@ConditionalOnProperty(value = "storage.type", havingValue = "oss")
public class OSSFileServiceImpl implements IFileService {

    @Autowired
    private OSSClient ossClient;

    @Autowired
    private OSSProperties ossProperties;

    private FileVO upload(MultipartFile file,Long dirId) {
        try {
            InputStream inputStream = file.getInputStream();
            //获取原始的文件名
            String originalFilename = file.getOriginalFilename();
            String extName = originalFilename.substring(originalFilename.lastIndexOf(".")+1);
            //在oss中存储名字就是UUID + 文件的后缀名
            String prefix = ossProperties.getPathPrefix();
            if (StringUtils.isNotBlank(prefix) && !prefix.endsWith("/")) {
                prefix = prefix + "/";
            }
            String objectName = prefix + dirId + "/" + UUID.randomUUID() + "." + extName;
            ObjectMetadata objectMetadata = new ObjectMetadata();
            // set public read
            objectMetadata.setObjectAcl(CannedAccessControlList.PublicRead);

            // 创建PutObjectRequest对象。
            PutObjectRequest putObjectRequest = new PutObjectRequest(ossProperties.getBucketName(), objectName, inputStream, objectMetadata);

            // 创建PutObject请求。
            PutObjectResult putObjectResult = ossClient.putObject(putObjectRequest);

            if (putObjectResult == null || StringUtils.isBlank(putObjectResult.getRequestId())) {
                log.error("上传oss异常putObjectResult未正常返回: {}", putObjectRequest);
                throw new ServiceException(ResultCode.OSS_UPLOAD_FAILED);
            }
            FileVO fileVO = new FileVO();
            fileVO.setUrl(ossProperties.getBaseUrl() + objectName);
            fileVO.setKey(objectName);
            fileVO.setName(new File(objectName).getName());
            return fileVO;
        } catch (Exception e) {
            log.error("上传oss异常", e);
            throw new ServiceException(ResultCode.OSS_UPLOAD_FAILED);
        }
    }
    @Override
    public FileVO uploadAppScreenShot(MultipartFile file,Long appId) {
        return upload(file,appId);
    }
    @Override
    public FileVO uploadUserAvatar(MultipartFile file,Long userId) {
        return upload(file,userId);
    }
    @Override
    public SignVO getSign() {
        try {
            //获取ak sk
            String accesskeyid = ossProperties.getAccessKeyId();
            String accesskeysecret = ossProperties.getAccessKeySecret();
            // 获取当前时间
            Instant now = Instant.now();
            //构建返回数据
            SignVO signVO = new SignVO();
            signVO.setHost(ossProperties.getBaseUrl());
            signVO.setPathPrefix(ossProperties.getPathPrefix());

            // 步骤1：创建policy。
            ObjectMapper mapper = new ObjectMapper();

            Map<String, Object> policy = new HashMap<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(OSSCustomConstants.SIGN_EXPIRE_TIME_FORMAT)
                    .withZone(java.time.ZoneOffset.UTC);
            String expiration = formatter.format(now.plusSeconds(ossProperties.getExpre()));
            policy.put("expiration", expiration);

            List<Object> conditions = new ArrayList<>();

            Map<String, String> bucketCondition = new HashMap<>();
            bucketCondition.put("bucket", ossProperties.getBucketName());
            conditions.add(bucketCondition);

            Map<String, String> signatureVersionCondition = new HashMap<>();
            signatureVersionCondition.put("x-oss-signature-version", "OSS4-HMAC-SHA256");
            conditions.add(signatureVersionCondition);

            Map<String, String> credentialCondition = new HashMap<>();
            formatter = DateTimeFormatter.ofPattern(OSSCustomConstants.SIGN_DATE_FORMAT)
                    .withZone(java.time.ZoneOffset.UTC);
            String dateStr = formatter.format(now);
            String xOSSCredential = accesskeyid + "/" + dateStr + "/" + ossProperties.getRegion() + "/oss/aliyun_v4_request";
            signVO.setXOSSCredential(xOSSCredential);
            credentialCondition.put("x-oss-credential", xOSSCredential); // 替换为实际的 access key id
            conditions.add(credentialCondition);

            Map<String, String> dateCondition = new HashMap<>();

            // 定义日期时间格式化器
            formatter = DateTimeFormatter.ofPattern(OSSCustomConstants.SIGN_REQUEST_TIME_FORMAT)
                    .withZone(java.time.ZoneOffset.UTC);

            // 格式化时间
            String xOSSDate = formatter.format(now);
            signVO.setXOSSDate(xOSSDate);
            dateCondition.put("x-oss-date", xOSSDate);

            conditions.add(dateCondition);

            conditions.add(Arrays.asList("content-length-range", ossProperties.getMinLen(), ossProperties.getMaxLen()));
            conditions.add(Arrays.asList("eq", "$success_action_status", "200"));

            policy.put("conditions", conditions);

            String jsonPolicy = mapper.writeValueAsString(policy);

            // 步骤2：构造待签名字符串（StringToSign）。
            String policyBase64 = new String(Base64.encodeBase64(jsonPolicy.getBytes()));
            signVO.setPolicy(policyBase64);

            // 步骤3：计算SigningKey。
            byte[] dateKey = hmacsha256(("aliyun_v4" + accesskeysecret).getBytes(), dateStr);
            byte[] dateRegionKey = hmacsha256(dateKey, ossProperties.getRegion());
            byte[] dateRegionServiceKey = hmacsha256(dateRegionKey, "oss");
            byte[] signingKey = hmacsha256(dateRegionServiceKey, "aliyun_v4_request");

            // 步骤4：计算Signature。
            byte[] result = hmacsha256(signingKey, policyBase64);
            String signature = BinaryUtil.toHex(result);
            signVO.setSignature(signature);
            return signVO;
        } catch (Exception e) {
            log.error("生成直传签名失败", e);
            throw new ServiceException(ResultCode.PRE_SIGN_URL_FAILED);
        }
    }

    public static byte[] hmacsha256(byte[] key, String data) {
        try {
            // 初始化HMAC密钥规格，指定算法为HMAC-SHA256并使用提供的密钥。
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "HmacSHA256");

            // 获取Mac实例，并通过getInstance方法指定使用HMAC-SHA256算法。
            Mac mac = Mac.getInstance("HmacSHA256");
            // 使用密钥初始化Mac对象。
            mac.init(secretKeySpec);

            // 执行HMAC计算，通过doFinal方法接收需要计算的数据并返回计算结果的数组。
            byte[] hmacBytes = mac.doFinal(data.getBytes());

            return hmacBytes;
        } catch (Exception e) {
            log.error("生成直传签名失败", e);
            throw new ServiceException(ResultCode.PRE_SIGN_URL_FAILED);
        }
    }

    public static void main(String[] args) {
        System.out.println(UUID.randomUUID());
    }
}
