package com.bitejiuyeke.biteportalservice.flash.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bitejiuyeke.biteportalservice.flash.domain.entity.AppDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 *
 * @author Emrys
 * content:
 */
@Mapper
public interface AppMapper extends BaseMapper<AppDO> {
    @Update("UPDATE app SET app_preview_url = #{url} WHERE id = #{appId}")
    int updateUrlById(@Param("appId") Long appId, @Param("url") String url);
}
