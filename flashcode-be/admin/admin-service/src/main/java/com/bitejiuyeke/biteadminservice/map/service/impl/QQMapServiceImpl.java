package com.bitejiuyeke.biteadminservice.map.service.impl;

import com.bitejiuyeke.biteadminapi.map.constants.MapConstants;
import com.bitejiuyeke.biteadminservice.map.domain.dto.GeoResultDTO;
import com.bitejiuyeke.biteadminservice.map.domain.dto.LocationDTO;
import com.bitejiuyeke.biteadminservice.map.domain.dto.PoiListDTO;
import com.bitejiuyeke.biteadminservice.map.domain.dto.SuggestSearchDTO;
import com.bitejiuyeke.biteadminservice.map.service.IMapProvider;
import com.bitejiuyeke.bitecommondomain.domain.ResultCode;
import com.bitejiuyeke.bitecommondomain.exception.ServiceException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * 地图服务的实现类
 */
@RefreshScope
@Component
@Data
@Slf4j
@ConditionalOnProperty(value = "map.type", havingValue = "qqmap")
public class QQMapServiceImpl implements IMapProvider {


    /**
     * 腾讯位置服务域名
     */
    @Value("${qqmap.apiServer}")
    private String apiServer;

    /**
     * 调用腾讯位置服务的秘钥
     */
    @Value("${qqmap.key}")
    private String key;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 根据关键词搜索地点
     * @param suggestSearchDTO 搜索条件
     * @return 搜索结果
     */
    @Override
    public PoiListDTO searchQQMapPlaceByRegion(SuggestSearchDTO suggestSearchDTO) {
        // 1 构建请求url
        String url = String.format(
                apiServer + MapConstants.QQMAP_API_PLACE_SUGGESTION +
                        "?key=%s&region=%s&region_fix=1&page_index=%s&page_size=%s&keyword=%s",
                key, suggestSearchDTO.getId(), suggestSearchDTO.getPageIndex(), suggestSearchDTO.getPageSize(),suggestSearchDTO.getKeyword()
        );
        // 2 直接发送请求，并拿到返回结果再做对象转换
        ResponseEntity<PoiListDTO> response =  restTemplate.getForEntity(url, PoiListDTO.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("获取关键词查询结果异常", response);
            throw new ServiceException(ResultCode.QQMAP_QUERY_FAILED);
        }
        return response.getBody();
    }

    /**
     * 根据经纬度来获取区域信息
     * @param locationDTO 经纬度
     * @return 区域信息
     */
    @Override
    public GeoResultDTO getQQMapDistrictByLonLat(LocationDTO locationDTO) {
        // 1 构建请求url
        String url = String.format(apiServer + MapConstants.QQMAP_GEOCODER +
                "?key=%s&location=%s",
                key, locationDTO.formatInfo()
        );
        // 2 直接发送请求，并拿到返回结果再做对象转换
        ResponseEntity<GeoResultDTO> response =  restTemplate.getForEntity(url, GeoResultDTO.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("根据经纬度来获取区域信息查询结果异常", response);
            throw new ServiceException(ResultCode.QQMAP_QUERY_FAILED);
        }
        return response.getBody();
    }
}
