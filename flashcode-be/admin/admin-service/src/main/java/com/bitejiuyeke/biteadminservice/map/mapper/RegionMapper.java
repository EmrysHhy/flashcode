package com.bitejiuyeke.biteadminservice.map.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bitejiuyeke.biteadminservice.map.domain.entity.SysRegion;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * sys_region表的mapper
 */
@Mapper
public interface RegionMapper extends BaseMapper<SysRegion> {

    /**
     * 获取全量区域信息
     * @return 区域列表
     */
    List<SysRegion> selectAllRegion();
}
