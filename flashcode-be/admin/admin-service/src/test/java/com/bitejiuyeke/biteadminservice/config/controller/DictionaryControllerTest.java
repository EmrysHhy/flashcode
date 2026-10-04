package com.bitejiuyeke.biteadminservice.config.controller;

import com.bitejiuyeke.biteadminapi.config.domain.dto.*;
import com.bitejiuyeke.biteadminservice.BiteAdminServiceApplication;
import com.bitejiuyeke.biteadminservice.config.service.ISysDictionaryService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest(classes = BiteAdminServiceApplication.class)
public class DictionaryControllerTest {

    @Resource
    private ISysDictionaryService sysDictionaryService;

    @Test
    @Transactional
    void addType() {
        DictionaryTypeWriteReqDTO dto = new DictionaryTypeWriteReqDTO();
        dto.setTypeKey("weight");
        dto.setValue("重量");
        dto.setRemark("重量配置");
        Assertions.assertTrue(sysDictionaryService.addType(dto) > 0L);
    }

    @Test
    @Transactional
    void listType() {
        DictionaryTypeWriteReqDTO dto = new DictionaryTypeWriteReqDTO();
        dto.setTypeKey("weight");
        dto.setValue("重量");
        dto.setRemark("重量配置");
        sysDictionaryService.addType(dto);
        dto.setTypeKey("height");
        dto.setValue("高度");
        dto.setRemark("高度配置");
        sysDictionaryService.addType(dto);
        DictionaryTypeListReqDTO dictionaryTypeListReqDTO = new DictionaryTypeListReqDTO();
        Assertions.assertTrue(sysDictionaryService.listType(dictionaryTypeListReqDTO).getList().size() > 0);
        dictionaryTypeListReqDTO.setTypeKey("weight");
        Assertions.assertTrue(sysDictionaryService.listType(dictionaryTypeListReqDTO).getList().size() == 1);
        dictionaryTypeListReqDTO.setTypeKey("weight1");
        Assertions.assertTrue(sysDictionaryService.listType(dictionaryTypeListReqDTO).getList().size() == 0);
    }

    @Test
    @Transactional
    void editType() {
        DictionaryTypeWriteReqDTO dto = new DictionaryTypeWriteReqDTO();
        dto.setTypeKey("weight");
        dto.setValue("重量");
        dto.setRemark("重量配置");
        sysDictionaryService.addType(dto);
        dto.setTypeKey("height");
        dto.setValue("高度");
        dto.setRemark("高度配置");
        sysDictionaryService.addType(dto);
        dto.setTypeKey("weight1");
        dto.setValue("重量1");
        dto.setRemark("重量配置1");
//        Assertions.assertTrue(sysDictionaryService.editType(dto) > 0L);
        dto.setTypeKey("weight");
        dto.setValue("高度");
        dto.setRemark("重量配置1");
        Assertions.assertTrue(sysDictionaryService.editType(dto) > 0L);
    }

    @Test
    @Transactional
    void addData() {
        DictionaryTypeWriteReqDTO dto = new DictionaryTypeWriteReqDTO();
        dto.setTypeKey("weight");
        dto.setValue("重量");
        dto.setRemark("重量配置");
        sysDictionaryService.addType(dto);
        DictionaryDataAddReqDTO dictionaryDataAddReqDTO = new DictionaryDataAddReqDTO();
        dictionaryDataAddReqDTO.setTypeKey("weight");
        dictionaryDataAddReqDTO.setDataKey("tenKg");
        dictionaryDataAddReqDTO.setValue("十公斤");
        Assertions.assertTrue(sysDictionaryService.addData(dictionaryDataAddReqDTO) > 0L);
        dictionaryDataAddReqDTO.setTypeKey("weight");
        dictionaryDataAddReqDTO.setDataKey("tenKg");
        dictionaryDataAddReqDTO.setValue("十公斤");
        Assertions.assertTrue(sysDictionaryService.addData(dictionaryDataAddReqDTO) > 0L);
    }

    @Test
    @Transactional
    void listData() {
        DictionaryTypeWriteReqDTO dto = new DictionaryTypeWriteReqDTO();
        dto.setTypeKey("weight");
        dto.setValue("重量");
        dto.setRemark("重量配置");
        sysDictionaryService.addType(dto);
        DictionaryDataAddReqDTO dictionaryDataAddReqDTO = new DictionaryDataAddReqDTO();
        dictionaryDataAddReqDTO.setTypeKey("weight");
        dictionaryDataAddReqDTO.setDataKey("tenKg");
        dictionaryDataAddReqDTO.setValue("十公斤");
        sysDictionaryService.addData(dictionaryDataAddReqDTO);
        dictionaryDataAddReqDTO.setTypeKey("weight");
        dictionaryDataAddReqDTO.setDataKey("twentyKg");
        dictionaryDataAddReqDTO.setValue("二十公斤");
        sysDictionaryService.addData(dictionaryDataAddReqDTO);
        DictionaryDataListReqDTO dictionaryDataListReqDTO = new DictionaryDataListReqDTO();
        dictionaryDataListReqDTO.setTypeKey("weight");
        Assertions.assertTrue(sysDictionaryService.listData(dictionaryDataListReqDTO).getList().size() == 2);
        dictionaryDataListReqDTO.setPageSize(1);
        Assertions.assertTrue(sysDictionaryService.listData(dictionaryDataListReqDTO).getTotalPages() == 2);
    }

    @Test
    @Transactional
    void editData() {
        DictionaryTypeWriteReqDTO dto = new DictionaryTypeWriteReqDTO();
        dto.setTypeKey("weight");
        dto.setValue("重量");
        dto.setRemark("重量配置");
        sysDictionaryService.addType(dto);
        DictionaryDataAddReqDTO dictionaryDataAddReqDTO = new DictionaryDataAddReqDTO();
        dictionaryDataAddReqDTO.setTypeKey("weight");
        dictionaryDataAddReqDTO.setDataKey("tenKg");
        dictionaryDataAddReqDTO.setValue("十公斤");
        sysDictionaryService.addData(dictionaryDataAddReqDTO);
        dictionaryDataAddReqDTO.setTypeKey("weight");
        dictionaryDataAddReqDTO.setDataKey("twentyKg");
        dictionaryDataAddReqDTO.setValue("二十公斤");
        sysDictionaryService.addData(dictionaryDataAddReqDTO);
        DictionaryDataEditReqDTO dictionaryDataEditReqDTO = new DictionaryDataEditReqDTO();
        dictionaryDataEditReqDTO.setDataKey("tenKg");
        dictionaryDataEditReqDTO.setValue("二十公斤");
        Assertions.assertTrue(sysDictionaryService.editData(dictionaryDataEditReqDTO) > 0L);
    }

    @Test
    @Transactional
    void selectDictDataByType() {
        DictionaryTypeWriteReqDTO dto = new DictionaryTypeWriteReqDTO();
        dto.setTypeKey("weight");
        dto.setValue("重量");
        dto.setRemark("重量配置");
        sysDictionaryService.addType(dto);
        DictionaryDataAddReqDTO dictionaryDataAddReqDTO = new DictionaryDataAddReqDTO();
        dictionaryDataAddReqDTO.setTypeKey("weight");
        dictionaryDataAddReqDTO.setDataKey("tenKg");
        dictionaryDataAddReqDTO.setValue("十公斤");
        sysDictionaryService.addData(dictionaryDataAddReqDTO);
        dictionaryDataAddReqDTO.setTypeKey("weight");
        dictionaryDataAddReqDTO.setDataKey("twentyKg");
        dictionaryDataAddReqDTO.setValue("二十公斤");
        sysDictionaryService.addData(dictionaryDataAddReqDTO);
        Assertions.assertTrue(sysDictionaryService.selectDictDataByType("weight").size() == 2);
    }

    @Test
    @Transactional
    void selectDictDataByTypes() {
        DictionaryTypeWriteReqDTO dto = new DictionaryTypeWriteReqDTO();
        dto.setTypeKey("weight");
        dto.setValue("重量");
        dto.setRemark("重量配置");
        sysDictionaryService.addType(dto);
        dto.setTypeKey("height");
        dto.setValue("高度");
        dto.setRemark("高度配置");
        sysDictionaryService.addType(dto);
        DictionaryDataAddReqDTO dictionaryDataAddReqDTO = new DictionaryDataAddReqDTO();
        dictionaryDataAddReqDTO.setTypeKey("weight");
        dictionaryDataAddReqDTO.setDataKey("tenKg");
        dictionaryDataAddReqDTO.setValue("十公斤");
        sysDictionaryService.addData(dictionaryDataAddReqDTO);
        dictionaryDataAddReqDTO.setTypeKey("weight");
        dictionaryDataAddReqDTO.setDataKey("twentyKg");
        dictionaryDataAddReqDTO.setValue("二十公斤");
        sysDictionaryService.addData(dictionaryDataAddReqDTO);
        dictionaryDataAddReqDTO.setTypeKey("height");
        dictionaryDataAddReqDTO.setDataKey("tenCm");
        dictionaryDataAddReqDTO.setValue("十厘米");
        sysDictionaryService.addData(dictionaryDataAddReqDTO);
        dictionaryDataAddReqDTO.setTypeKey("height");
        dictionaryDataAddReqDTO.setDataKey("twentyCm");
        dictionaryDataAddReqDTO.setValue("二十厘米");
        sysDictionaryService.addData(dictionaryDataAddReqDTO);
        List<String> typeKeys = new ArrayList<>();
        typeKeys.add("weight");
        typeKeys.add("height");
        Assertions.assertTrue(sysDictionaryService.selectDictDataByTypes(typeKeys).get("weight").size() == 2);
    }

    @Test
    @Transactional
    void getDicDataByKey() {
        DictionaryTypeWriteReqDTO dto = new DictionaryTypeWriteReqDTO();
        dto.setTypeKey("weight");
        dto.setValue("重量");
        dto.setRemark("重量配置");
        sysDictionaryService.addType(dto);
        DictionaryDataAddReqDTO dictionaryDataAddReqDTO = new DictionaryDataAddReqDTO();
        dictionaryDataAddReqDTO.setTypeKey("weight");
        dictionaryDataAddReqDTO.setDataKey("tenKg");
        dictionaryDataAddReqDTO.setValue("十公斤");
        sysDictionaryService.addData(dictionaryDataAddReqDTO);
        dictionaryDataAddReqDTO.setTypeKey("weight");
        dictionaryDataAddReqDTO.setDataKey("twentyKg");
        dictionaryDataAddReqDTO.setValue("二十公斤");
        sysDictionaryService.addData(dictionaryDataAddReqDTO);
        Assertions.assertTrue(sysDictionaryService.getDicDataByKey("tenKg").getValue().equals("十公斤"));
    }

    @Test
    @Transactional
    void getDicDataByKeys() {
        DictionaryTypeWriteReqDTO dto = new DictionaryTypeWriteReqDTO();
        dto.setTypeKey("weight");
        dto.setValue("重量");
        dto.setRemark("重量配置");
        sysDictionaryService.addType(dto);
        DictionaryDataAddReqDTO dictionaryDataAddReqDTO = new DictionaryDataAddReqDTO();
        dictionaryDataAddReqDTO.setTypeKey("weight");
        dictionaryDataAddReqDTO.setDataKey("tenKg");
        dictionaryDataAddReqDTO.setValue("十公斤");
        sysDictionaryService.addData(dictionaryDataAddReqDTO);
        dictionaryDataAddReqDTO.setTypeKey("weight");
        dictionaryDataAddReqDTO.setDataKey("twentyKg");
        dictionaryDataAddReqDTO.setValue("二十公斤");
        sysDictionaryService.addData(dictionaryDataAddReqDTO);
        List<String> list = new ArrayList<>();
        list.add("tenKg");
        list.add("twentyKg");
        Assertions.assertTrue(sysDictionaryService.getDicDataByKeys(list).size() == 2);
    }
}
