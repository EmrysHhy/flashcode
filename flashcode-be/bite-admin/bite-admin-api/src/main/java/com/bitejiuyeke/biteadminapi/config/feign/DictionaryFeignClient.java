package com.bitejiuyeke.biteadminapi.config.feign;

import com.bitejiuyeke.biteadminapi.config.domain.dto.DictionaryDataDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 字典服务相关远程调用
 */
@FeignClient(contextId = "dictionaryFeignClient", value = "bite-admin")
public interface DictionaryFeignClient {

    /**
     * 获取某个字典类型下的所有字典数据
     * @param typeKey 字典类型键
     * @return 字典数据列表
     */
    @GetMapping("/dictionary_data/type")
    List<DictionaryDataDTO> selectDictDataByType(@RequestParam String typeKey);

    /**
     * 获取多个字典类型下的所有字典数据
     * @param typeKeys 字典类型键列表
     * @return 哈希  字典类型键->字典数据列表
     */
    @PostMapping("/dictionary_data/types")
    Map<String, List<DictionaryDataDTO>> selectDictDataByTypes(@RequestBody List<String> typeKeys);

    /**
     * 根据字典数据业务主键获取字典数据对象
     * @param dataKey 字典数据业务主键
     * @return DictionaryDataDTO
     */
    @GetMapping("/dictionary_data/key")
    DictionaryDataDTO getDicDataByKey(@RequestParam String dataKey);

    /**
     * 根据多个字典数据业务主键获取多个字典数据对象
     * @param dataKeys 多个字典数据业务主键
     * @return List<DictionaryDataDTO>
     */
    @PostMapping("/dictionary_data/keys")
    List<DictionaryDataDTO> getDicDataByKeys(@RequestBody List<String> dataKeys);
}
