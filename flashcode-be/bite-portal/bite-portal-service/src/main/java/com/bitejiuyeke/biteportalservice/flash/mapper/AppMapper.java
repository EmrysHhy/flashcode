package com.bitejiuyeke.biteportalservice.flash.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bitejiuyeke.biteportalservice.flash.domain.dto.require.MyselfListReqParam;
import com.bitejiuyeke.biteportalservice.flash.domain.dto.require.SquareListReqParam;
import com.bitejiuyeke.biteportalservice.flash.domain.dto.result.SquareAppDTO;
import com.bitejiuyeke.biteportalservice.flash.domain.entity.AppDO;
import org.apache.ibatis.annotations.*;

import java.util.List;

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
    /**
     * 分页查询某个用户的应用。appType 为空时不按类型过滤。
     */
    List<AppDO> selectMyselfAppByUserId(MyselfListReqParam myselfListReqDTO);

    /**
     * 统计某个用户的应用数量，过滤条件与分页查询一致。
     */
    Long countMyselfApp(MyselfListReqParam myselfListReqDTO);

    /**
     * 分页查询已部署应用。appType 为空时不按类型过滤。
     */
    List<SquareAppDTO> selectSquareApp(@Param("query") SquareListReqParam query, @Param("deployStatus") Integer deployStatus);

    /**
     * 统计已部署应用数量，过滤条件与广场分页查询一致。
     */
    Long countSquareApp(@Param("query") SquareListReqParam query, @Param("deployStatus") Integer deployStatus);
}
