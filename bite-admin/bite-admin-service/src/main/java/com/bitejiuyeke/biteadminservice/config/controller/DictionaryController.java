package com.bitejiuyeke.biteadminservice.config.controller;

import com.bitejiuyeke.biteadminapi.config.domain.dto.*;
import com.bitejiuyeke.biteadminapi.config.domain.vo.DictionaryDataVo;
import com.bitejiuyeke.biteadminapi.config.domain.vo.DictionaryTypeVO;
import com.bitejiuyeke.biteadminapi.config.feign.DictionaryFeignClient;
import com.bitejiuyeke.biteadminservice.config.service.ISysDictionaryService;
import com.bitejiuyeke.bitecommondomain.domain.R;
import com.bitejiuyeke.bitecommondomain.domain.vo.BasePageVO;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 字典服务相关接口
 */
@RestController
public class DictionaryController implements DictionaryFeignClient {

    @Resource
    private ISysDictionaryService iSysDictionaryService;

    /**
     * 新增字典类型
     * @param dictionaryTypeWriteReqDTO 新增字典类型DTO
     * @return Long
     */
    @PostMapping("/dictionary_type/add")
    public R<Long> addType(@RequestBody @Validated DictionaryTypeWriteReqDTO dictionaryTypeWriteReqDTO) {
        return R.ok(iSysDictionaryService.addType(dictionaryTypeWriteReqDTO));
    }

    /**
     * 字典类型列表
     * @param dictionaryTypeListReqDTO 字典类型列表DTO
     * @return BasePageVO
     */
    @GetMapping("/dictionary_type/list")
    public R<BasePageVO<DictionaryTypeVO>> listType(@Validated DictionaryTypeListReqDTO dictionaryTypeListReqDTO) {
        return R.ok(iSysDictionaryService.listType(dictionaryTypeListReqDTO));
    }

    /**
     * 编辑字典类型
     * @param dictionaryTypeWriteReqDTO 编辑字典类型DTO
     * @return Long
     */
    @PostMapping("/dictionary_type/edit")
    public R<Long> editType(@RequestBody @Validated DictionaryTypeWriteReqDTO dictionaryTypeWriteReqDTO) {
        return R.ok(iSysDictionaryService.editType(dictionaryTypeWriteReqDTO));
    }

    /**
     * 新增字典数据
     * @param dictionaryDataAddReqDTO 新增字典数据DTO
     * @return Long
     */
    @PostMapping("/dictionary_data/add")
    public R<Long> addData(@RequestBody @Validated DictionaryDataAddReqDTO dictionaryDataAddReqDTO) {
        return R.ok(iSysDictionaryService.addData(dictionaryDataAddReqDTO));
    }

    /**
     * 字典数据列表
     * @param dictionaryDataListReqDTO 字典数据列表DTO
     * @return BasePageVO
     */
    @GetMapping("/dictionary_data/list")
    public R<BasePageVO<DictionaryDataVo>> listData(@Validated DictionaryDataListReqDTO dictionaryDataListReqDTO) {
        return R.ok(iSysDictionaryService.listData(dictionaryDataListReqDTO));
    }

    /**
     * 编辑字典数据
     * @param dictionaryDataEditReqDTO 编辑字典数据DTO
     * @return Long
     */
    @PostMapping("/dictionary_data/edit")
    public R<Long> editData(@RequestBody @Validated DictionaryDataEditReqDTO dictionaryDataEditReqDTO) {
        return R.ok(iSysDictionaryService.editData(dictionaryDataEditReqDTO));
    }

    @Override
    public List<DictionaryDataDTO> selectDictDataByType(String typeKey) {
        return iSysDictionaryService.selectDictDataByType(typeKey);
    }

    @Override
    public Map<String, List<DictionaryDataDTO>> selectDictDataByTypes(List<String> typeKeys) {
        return iSysDictionaryService.selectDictDataByTypes(typeKeys);
    }

    @Override
    public DictionaryDataDTO getDicDataByKey(String dataKey) {
        return iSysDictionaryService.getDicDataByKey(dataKey);
    }

    @Override
    public List<DictionaryDataDTO> getDicDataByKeys(List<String> dataKeys) {
        return iSysDictionaryService.getDicDataByKeys(dataKeys);
    }
}
