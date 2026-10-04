package com.bitejiuyeke.biteadminservice.config.service;

import com.bitejiuyeke.biteadminapi.config.domain.dto.*;
import com.bitejiuyeke.biteadminapi.config.domain.vo.DictionaryDataVo;
import com.bitejiuyeke.biteadminapi.config.domain.vo.DictionaryTypeVO;
import com.bitejiuyeke.bitecommondomain.domain.vo.BasePageVO;

import java.util.List;
import java.util.Map;

/**
 * 字典服务 接口
 */
public interface ISysDictionaryService {

    /**
     * 新增字典类型
     * @param dictionaryTypeWriteReqDTO 新增字典类型DTO
     * @return Long
     */
    Long addType(DictionaryTypeWriteReqDTO dictionaryTypeWriteReqDTO);

    /**
     * 字典类型列表
     * @param dictionaryTypeListReqDTO 字典类型列表DTO
     * @return BasePageVO
     */
    BasePageVO<DictionaryTypeVO> listType(DictionaryTypeListReqDTO dictionaryTypeListReqDTO);

    /**
     * 编辑字典类型
     * @param dictionaryTypeWriteReqDTO 编辑字典类型DTO
     * @return Long
     */
    Long editType(DictionaryTypeWriteReqDTO dictionaryTypeWriteReqDTO);

    /**
     * 添加字典数据
     * @param dictionaryDataAddReqDTO 添加字典数据DTO
     * @return Long
     */
    Long addData(DictionaryDataAddReqDTO dictionaryDataAddReqDTO);

    /**
     * 查看字典数据
     * @param dictionaryDataListReqDTO 字典数据列表DTO
     * @return BasePageVO
     */
    BasePageVO<DictionaryDataVo> listData(DictionaryDataListReqDTO dictionaryDataListReqDTO);

    /**
     * 编辑字典数据
     * @param dictionaryDataEditReqDTO 编辑字典数据DTO
     * @return Long
     */
    Long editData(DictionaryDataEditReqDTO dictionaryDataEditReqDTO);

    /**
     * 获取某个字典类型下的所有字典数据
     * @param typeKey 字典类型键
     * @return 字典数据列表
     */
    List<DictionaryDataDTO> selectDictDataByType(String typeKey);

    /**
     * 获取多个字典类型下的所有字典数据
     * @param typeKeys 字典类型键列表
     * @return 哈希  字典类型键->字典数据列表
     */
    Map<String, List<DictionaryDataDTO>> selectDictDataByTypes(List<String> typeKeys);

    /**
     * 根据字典数据业务主键获取字典数据对象
     * @param dataKey 字典数据业务主键
     * @return DictionaryDataDTO
     */
    DictionaryDataDTO getDicDataByKey(String dataKey);

    /**
     * 根据多个字典数据业务主键获取多个字典数据对象
     * @param dataKeys 多个字典数据业务主键
     * @return List<DictionaryDataDTO>
     */
    List<DictionaryDataDTO> getDicDataByKeys(List<String> dataKeys);
}
