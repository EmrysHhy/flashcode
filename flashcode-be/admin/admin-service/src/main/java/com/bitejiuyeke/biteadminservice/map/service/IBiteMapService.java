package com.bitejiuyeke.biteadminservice.map.service;

import com.bitejiuyeke.biteadminapi.map.domain.dto.LocationReqDTO;
import com.bitejiuyeke.biteadminapi.map.domain.dto.PlaceSearchReqDTO;
import com.bitejiuyeke.biteadminservice.map.domain.dto.RegionCityDTO;
import com.bitejiuyeke.biteadminservice.map.domain.dto.SearchPoiDTO;
import com.bitejiuyeke.biteadminservice.map.domain.dto.SysRegionDTO;
import com.bitejiuyeke.bitecommoncore.domain.dto.BasePageDTO;
import com.bitejiuyeke.bitecommondomain.domain.dto.BasePageReqDTO;

import java.util.List;
import java.util.Map;

/**
 * 地图服务层的接口
 */
public interface IBiteMapService {

    /**
     * 城市列表查询
     * @return 城市列表信息
     */
    List<SysRegionDTO> getCityList();

    /**
     * 城市拼音归类查询
     * @return 城市字母与城市列表的哈希
     */
    Map<String, List<SysRegionDTO>> getCityPylist();

    /**
     * 根据父级区域ID获取子集区域列表
     * @param parentId 父级区域ID
     * @return 子集区域列表
     */
    List<SysRegionDTO> getRegionChildren(Long parentId);

    /**
     * 获取热门城市列表
     * @return 城市列表
     */
    List<SysRegionDTO> getHotCityList();

    /**
     * 根据地点搜索
     * @param placeSearchReqDTO 搜索条件
     * @return 搜索结果
     */
    BasePageDTO<SearchPoiDTO> searchSuggestOnMap(PlaceSearchReqDTO placeSearchReqDTO);

    /**
     * 根据经纬度来定位城市
     * @param locationReqDTO 经纬度信息
     * @return 城市信息
     */
    RegionCityDTO getCityByLocation(LocationReqDTO locationReqDTO);
}
