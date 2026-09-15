package com.bitejiuyeke.biteportalservice.flash.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bitejiuyeke.biteportalservice.flash.domain.entity.ChatHistoryDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 聊天历史记录 Mapper
 *
 * @author Emrys
 */
@Mapper
public interface ChatHistoryMapper extends BaseMapper<ChatHistoryDO> {
    /**
     * 获取最后的{size}条数据
     * @param appId
     * @param size
     * @return
     */
    @Select("""
            SELECT * FROM (
                SELECT * FROM chat_history
                WHERE app_id = #{appId}
                ORDER BY id DESC
                LIMIT #{size}
            ) t
            ORDER BY id ASC
            """)
    List<ChatHistoryDO> getLastMesByAppId(Long appId, int size);

    @Update("UPDATE chat_history SET is_deleted = 1 WHERE app_id = #{appId}")
    void deleteByAppId(Long appId);
}
