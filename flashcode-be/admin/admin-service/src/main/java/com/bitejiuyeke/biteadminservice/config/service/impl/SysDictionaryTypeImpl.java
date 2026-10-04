package com.bitejiuyeke.biteadminservice.config.service.impl;

import com.alibaba.nacos.shaded.com.google.common.collect.Maps;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bitejiuyeke.biteadminapi.config.domain.dto.*;
import com.bitejiuyeke.biteadminapi.config.domain.vo.DictionaryDataVo;
import com.bitejiuyeke.biteadminapi.config.domain.vo.DictionaryTypeVO;
import com.bitejiuyeke.biteadminservice.config.domain.entity.SysDictionaryData;
import com.bitejiuyeke.biteadminservice.config.domain.entity.SysDictionaryType;
import com.bitejiuyeke.biteadminservice.config.mapper.SysDictionaryDataMapper;
import com.bitejiuyeke.biteadminservice.config.mapper.SysDictionaryTypeMapper;
import com.bitejiuyeke.biteadminservice.config.service.ISysDictionaryService;
import com.bitejiuyeke.bitecommondomain.domain.vo.BasePageVO;
import com.bitejiuyeke.bitecommondomain.exception.ServiceException;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 字典服务 实现类
 */
@Service
public class SysDictionaryTypeImpl implements ISysDictionaryService {

    @Resource
    private SysDictionaryDataMapper sysDictionaryDataMapper;

    @Resource
    private SysDictionaryTypeMapper sysDictionaryTypeMapper;

    @Override
    public Long addType(DictionaryTypeWriteReqDTO dictionaryTypeWriteReqDTO) {
        LambdaQueryWrapper<SysDictionaryType> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.select(SysDictionaryType::getId).eq(SysDictionaryType::getValue, dictionaryTypeWriteReqDTO.getValue()).or()
                .eq(SysDictionaryType::getTypeKey, dictionaryTypeWriteReqDTO.getTypeKey());
        SysDictionaryType sysDictionaryType = sysDictionaryTypeMapper.selectOne(lambdaQueryWrapper);
        if (sysDictionaryType != null) {
            throw new ServiceException("字典类型键或者值已存在");
        }
        sysDictionaryType = new SysDictionaryType();
        sysDictionaryType.setValue(dictionaryTypeWriteReqDTO.getValue());
        sysDictionaryType.setTypeKey(dictionaryTypeWriteReqDTO.getTypeKey());
        if (StringUtils.isNotBlank(dictionaryTypeWriteReqDTO.getRemark())) {
            sysDictionaryType.setRemark(dictionaryTypeWriteReqDTO.getRemark());
        }
        sysDictionaryTypeMapper.insert(sysDictionaryType);
        return sysDictionaryType.getId();
    }

    @Override
    public BasePageVO<DictionaryTypeVO> listType(DictionaryTypeListReqDTO dictionaryTypeListReqDTO) {
        BasePageVO<DictionaryTypeVO> result = new BasePageVO<>();
        LambdaQueryWrapper<SysDictionaryType> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(dictionaryTypeListReqDTO.getValue())) {
            queryWrapper.likeRight(SysDictionaryType::getValue, dictionaryTypeListReqDTO.getValue());
        }
        if (StringUtils.isNotBlank(dictionaryTypeListReqDTO.getTypeKey())) {
            queryWrapper.eq(SysDictionaryType::getTypeKey, dictionaryTypeListReqDTO.getTypeKey());
        }
        Page<SysDictionaryType> page = sysDictionaryTypeMapper.selectPage(
                new Page<>(dictionaryTypeListReqDTO.getPageNo().longValue(), dictionaryTypeListReqDTO.getPageSize().longValue()),
                queryWrapper
        );
        result.setTotals(Integer.parseInt(String.valueOf(page.getTotal())));
        result.setTotalPages(Integer.parseInt(String.valueOf(page.getPages())));
        List<DictionaryTypeVO> list = new ArrayList<>();
        for (SysDictionaryType sysDictionaryType : page.getRecords()) {
            DictionaryTypeVO dictionaryTypeVO = new DictionaryTypeVO();
            BeanUtils.copyProperties(sysDictionaryType, dictionaryTypeVO);
            list.add(dictionaryTypeVO);
        }
        result.setList(list);
        return result;
    }

    @Override
    public Long editType(DictionaryTypeWriteReqDTO dictionaryTypeWriteReqDTO) {
        SysDictionaryType sysDictionaryType = sysDictionaryTypeMapper.selectOne(new LambdaQueryWrapper<SysDictionaryType>().eq(SysDictionaryType::getTypeKey, dictionaryTypeWriteReqDTO.getTypeKey()));
        if (sysDictionaryType == null) {
            throw new ServiceException("字典类型不存在");
        }
        if (sysDictionaryTypeMapper.selectOne(new LambdaQueryWrapper<SysDictionaryType>()
                .ne(SysDictionaryType::getTypeKey, dictionaryTypeWriteReqDTO.getTypeKey())
                .eq(SysDictionaryType::getValue, dictionaryTypeWriteReqDTO.getValue())
        ) != null) {
            throw new ServiceException("字典类型名称已存在");
        }
        sysDictionaryType.setValue(dictionaryTypeWriteReqDTO.getValue());
        sysDictionaryType.setRemark(dictionaryTypeWriteReqDTO.getRemark());
        sysDictionaryTypeMapper.updateById(sysDictionaryType);
        return sysDictionaryType.getId();
    }

    @Override
    public Long addData(DictionaryDataAddReqDTO dictionaryDataAddReqDTO) {
        if (sysDictionaryTypeMapper.selectOne(new LambdaQueryWrapper<SysDictionaryType>().eq(SysDictionaryType::getTypeKey, dictionaryDataAddReqDTO.getTypeKey())) == null) {
            throw new ServiceException("字典类型不存在");
        }
        SysDictionaryData sysDictionaryData = sysDictionaryDataMapper.selectOne(new LambdaQueryWrapper<SysDictionaryData>()
                .eq(SysDictionaryData::getValue, dictionaryDataAddReqDTO.getValue())
                .or()
                .eq(SysDictionaryData::getDataKey, dictionaryDataAddReqDTO.getDataKey())
        );
        if (sysDictionaryData != null) {
            throw new ServiceException("字典数据键或值已存在");
        }
        sysDictionaryData = new SysDictionaryData();
        sysDictionaryData.setDataKey(dictionaryDataAddReqDTO.getDataKey());
        sysDictionaryData.setTypeKey(dictionaryDataAddReqDTO.getTypeKey());
        if (dictionaryDataAddReqDTO.getSort() != null) {
            sysDictionaryData.setSort(dictionaryDataAddReqDTO.getSort());
        }
        if (StringUtils.isNotBlank(dictionaryDataAddReqDTO.getRemark())) {
            sysDictionaryData.setRemark(dictionaryDataAddReqDTO.getRemark());
        }
        sysDictionaryData.setValue(dictionaryDataAddReqDTO.getValue());
        sysDictionaryDataMapper.insert(sysDictionaryData);
        return sysDictionaryData.getId();
    }

    @Override
    public BasePageVO<DictionaryDataVo> listData(DictionaryDataListReqDTO dictionaryDataListReqDTO) {
        BasePageVO<DictionaryDataVo> result = new BasePageVO<>();
        LambdaQueryWrapper<SysDictionaryData> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysDictionaryData::getTypeKey, dictionaryDataListReqDTO.getTypeKey());
        if (StringUtils.isNotBlank(dictionaryDataListReqDTO.getValue())) {
            queryWrapper.likeRight(SysDictionaryData::getValue, dictionaryDataListReqDTO.getValue());
        }
        queryWrapper.orderByAsc(SysDictionaryData::getSort);
        queryWrapper.orderByAsc(SysDictionaryData::getId);
        Page<SysDictionaryData> page = sysDictionaryDataMapper.selectPage(
                new Page<>(dictionaryDataListReqDTO.getPageNo().longValue(), dictionaryDataListReqDTO.getPageSize().longValue()),
                queryWrapper
        );
        result.setTotals(((Long)page.getTotal()).intValue());
        result.setTotalPages(((Long)page.getPages()).intValue());
        List<DictionaryDataVo> list = new ArrayList<>();
        for (SysDictionaryData sysDictionaryData : page.getRecords()) {
            DictionaryDataVo dictionaryDataVo = new DictionaryDataVo();
            BeanUtils.copyProperties(sysDictionaryData, dictionaryDataVo);
            list.add(dictionaryDataVo);
        }
        result.setList(list);
        return result;
    }

    @Override
    public Long editData(DictionaryDataEditReqDTO dictionaryDataEditReqDTO) {
        SysDictionaryData sysDictionaryData = sysDictionaryDataMapper.selectOne(new LambdaQueryWrapper<SysDictionaryData>().eq(SysDictionaryData::getDataKey, dictionaryDataEditReqDTO.getDataKey()));
        if (sysDictionaryData == null) {
            throw new ServiceException("字典数据不存在");
        }
        if (sysDictionaryDataMapper.selectOne(new LambdaQueryWrapper<SysDictionaryData>().ne(SysDictionaryData::getDataKey, dictionaryDataEditReqDTO.getDataKey()).eq(SysDictionaryData::getValue, dictionaryDataEditReqDTO.getValue())) != null) {
            throw new ServiceException("字典数据名称已存在");
        }
        sysDictionaryData.setValue(dictionaryDataEditReqDTO.getValue());
        if (dictionaryDataEditReqDTO.getSort() != null) {
            sysDictionaryData.setSort(dictionaryDataEditReqDTO.getSort());
        }
        if (StringUtils.isNotBlank(dictionaryDataEditReqDTO.getRemark())) {
            sysDictionaryData.setRemark(dictionaryDataEditReqDTO.getRemark());
        }
        sysDictionaryDataMapper.updateById(sysDictionaryData);
        return sysDictionaryData.getId();
    }

    @Override
    public List<DictionaryDataDTO> selectDictDataByType(String typeKey) {
        // 1 先查询数据表实体类
        List<SysDictionaryData> list = sysDictionaryDataMapper.selectList(new LambdaQueryWrapper<SysDictionaryData>().eq(SysDictionaryData::getTypeKey, typeKey));
        // 2 需要把数据表实体类对象转换成出参实体类对象
        List<DictionaryDataDTO> result = new ArrayList<>();
        for (SysDictionaryData sysDictionaryData : list) {
            DictionaryDataDTO dictionaryDataDTO = new DictionaryDataDTO();
            BeanUtils.copyProperties(sysDictionaryData, dictionaryDataDTO);
            result.add(dictionaryDataDTO);
        }
        return result;
    }

    @Override
    public Map<String, List<DictionaryDataDTO>> selectDictDataByTypes(List<String> typeKeys) {
        // 1 先把所有的字典数据查询出来
        List<SysDictionaryData> list = sysDictionaryDataMapper.selectList(new LambdaQueryWrapper<SysDictionaryData>().in(SysDictionaryData::getTypeKey, typeKeys));
        List<DictionaryDataDTO> result = new ArrayList<>();
        for (SysDictionaryData sysDictionaryData : list) {
            DictionaryDataDTO dictionaryDataDTO = new DictionaryDataDTO();
            BeanUtils.copyProperties(sysDictionaryData, dictionaryDataDTO);
            result.add(dictionaryDataDTO);
        }
        // 2 把查询出来的结果封装成哈希映射的形式
        Map<String, List<DictionaryDataDTO>> map = Maps.newHashMap();
        for (DictionaryDataDTO dictionaryDataDTO : result) {
            List<DictionaryDataDTO> value;
            // 3 先判断当前字典类型业务主键是否在哈希中
            if (map.get(dictionaryDataDTO.getTypeKey()) == null) {
                value = new ArrayList<>();
                value.add(dictionaryDataDTO);
                map.put(dictionaryDataDTO.getTypeKey(), value);
            } else {
                // 4 当前字典类型业务主键已经在哈希中
                value = map.get(dictionaryDataDTO.getTypeKey());
                value.add(dictionaryDataDTO);
            }
        }
        return map;
    }

    @Override
    public DictionaryDataDTO getDicDataByKey(String dataKey) {
        // 1 根据字典数据业务主键查询字典数据表实体类对象
        SysDictionaryData sysDictionaryData = sysDictionaryDataMapper.selectOne(new LambdaQueryWrapper<SysDictionaryData>().eq(SysDictionaryData::getDataKey, dataKey));
        // 2 做对象转换
        DictionaryDataDTO dictionaryDataDTO = new DictionaryDataDTO();
        BeanUtils.copyProperties(sysDictionaryData, dictionaryDataDTO);
        return dictionaryDataDTO;
    }

    @Override
    public List<DictionaryDataDTO> getDicDataByKeys(List<String> dataKeys) {
        // 1 根据字典数据业务主键列表查询字典数据表实体类对象列表
        List<SysDictionaryData> list = sysDictionaryDataMapper.selectList(new LambdaQueryWrapper<SysDictionaryData>().in(SysDictionaryData::getDataKey, dataKeys));
        // 2 做列表的对象转换
        List<DictionaryDataDTO> result = new ArrayList<>();
        for (SysDictionaryData sysDictionaryData : list) {
            DictionaryDataDTO dictionaryDataDTO = new DictionaryDataDTO();
            BeanUtils.copyProperties(sysDictionaryData, dictionaryDataDTO);
            result.add(dictionaryDataDTO);
        }
        return result;
    }
}
