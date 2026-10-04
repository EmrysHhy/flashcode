package com.bitejiuyeke.biteadminapi.config.feign;

import com.bitejiuyeke.biteadminapi.config.domain.dto.ArgumentDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 参数服务相关远程调用
 */
@FeignClient(contextId = "argumentFeignClient", value = "bite-admin", path = "/argument")
public interface ArgumentFeignClient {

    /**
     * 根据参数键查询参数对象
     * @param configKey 参数键
     * @return 参数对象
     */
    @GetMapping("/key")
    ArgumentDTO getByConfigKey(@RequestParam String configKey);

    /**
     * 根据多个参数键查询多个参数对象
     * @param configKeys 多个参数键
     * @return 多个参数对象
     */
    @GetMapping("/keys")
    List<ArgumentDTO> getByConfigKeys(@RequestParam List<String> configKeys);
}
