package com.bitejiuyeke.portalservice.flash.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bitejiuyeke.portalservice.flash.domain.dto.require.MyselfListReqParam;
import com.bitejiuyeke.portalservice.flash.domain.dto.require.SquareListReqParam;
import com.bitejiuyeke.portalservice.flash.domain.dto.result.SquareAppDTO;
import com.bitejiuyeke.portalservice.flash.domain.entity.AppDO;
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

    @Update("UPDATE app SET deploy_status = 1, app_url = #{appUrl} WHERE id = #{appId}")
    int updateDeployById(@Param("appId") Long appId, @Param("appUrl") String appUrl);

    /** 需求文档没插过行时补一条，避免构建完 UPDATE 0 行。 */
    @Insert("INSERT IGNORE INTO app (id, user_id, app_name, app_desc, deploy_status) VALUES (#{appId}, #{userId}, '待生成', '待生成', 0)")
    int insertIfAbsent(@Param("appId") Long appId, @Param("userId") Long userId);

    /** 历史数据把作者写成了占位用户 999，当前登录用户继续生成时改回本人。 */
    @Update("UPDATE app SET user_id = #{userId} WHERE id = #{appId} AND user_id = 999")
    int bindOwnerIfPlaceholder(@Param("appId") Long appId, @Param("userId") Long userId);
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

    /**
     * 查询应用类型
     * @return
     */
    @Select("SELECT DISTINCT app_type FROM app WHERE id = #{appId}")
    Integer selectTpyeById(Long appId);
}
