package com.bitejiuyeke.biteportalservice.flash.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bitejiuyeke.biteportalservice.flash.domain.entity.AppDO;
import org.apache.ibatis.annotations.Insert;
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

    @Update("UPDATE app SET app_screenshot = #{url} WHERE id = #{appId}")
    int updateScreenshotById(@Param("appId") Long appId, @Param("url") String url);

    @Update("UPDATE app SET app_type = #{appType} WHERE id = #{appId}")
    int updateTypeById(@Param("appId") Long appId, @Param("appType") Integer appType);

    /** 生成接口允许直接带 appId；需求文档没插过行时补一条，避免构建完 UPDATE 0 行。 */
    @Insert("INSERT IGNORE INTO app (id, user_id, app_name, app_desc) VALUES (#{appId}, 999, '待生成', '待生成')")
    int insertIfAbsent(@Param("appId") Long appId);
}
