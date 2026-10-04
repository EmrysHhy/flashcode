package com.bitejiuyeke.biteadminapi.map.feign;

import com.bitejiuyeke.biteadminapi.map.domain.dto.LocationReqDTO;
import com.bitejiuyeke.biteadminapi.map.domain.dto.PlaceSearchReqDTO;
import com.bitejiuyeke.biteadminapi.map.domain.vo.RegionCityVo;
import com.bitejiuyeke.biteadminapi.map.domain.vo.RegionVO;
import com.bitejiuyeke.biteadminapi.map.domain.vo.SearchPoiVo;
import com.bitejiuyeke.bitecommondomain.domain.R;
import com.bitejiuyeke.bitecommondomain.domain.vo.BasePageVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * 地图服务相关远程调用
 */
@FeignClient(contextId = "mapFeignClient", value = "bite-admin")
public interface MapFeignClient {

    /**
     * 城市列表查询
     * @return 城市列表信息
     */
    @GetMapping("/map/city_list")
    R<List<RegionVO>> getCityList();

    /**
     * 城市拼音归类查询
     * @return 城市字母与城市列表的哈希
     */
    @GetMapping("/map/city_pinyin_list")
    R<Map<String, List<RegionVO>>> getCityPylist();

    /**
     * 根据父级区域ID获取子集区域列表
     * @param parentId 父级区域ID
     * @return 子集区域列表
     */
    @GetMapping("/map/region_children_list")
    R<List<RegionVO>> regionChildren(@RequestParam Long parentId);

    /**
     * 获取热门城市列表
     * @return 城市列表
     */
    @GetMapping("/map/city_hot_list")
    R<List<RegionVO>> getHotCityList();

    /**
     * 根据地点搜索
     * @param placeSearchReqDTO 搜索条件
     * @return 搜索结果
     */
    @PostMapping("/map/search")
    R<BasePageVO<SearchPoiVo>> searchSuggestOnMap(@RequestBody PlaceSearchReqDTO placeSearchReqDTO);

    /**
     * 根据经纬度来定位城市
     * @param locationReqDTO 经纬度信息
     * @return 城市信息
     */
    @PostMapping("/map/locate_city_by_location")
    R<RegionCityVo> locateCityByLocation(@RequestBody LocationReqDTO locationReqDTO);
}
