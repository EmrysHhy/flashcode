package com.bitejiuyeke.portalservice.flash.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bitejiuyeke.portalservice.flash.domain.dto.require.ChatHistoryReqParam;
import com.bitejiuyeke.portalservice.flash.domain.entity.ChatHistoryDO;
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

    /**
     * 删除当前会话
     * @param appId
     */
    @Update("UPDATE chat_history SET is_deleted = 1 WHERE app_id = #{appId}")
    void deleteByAppId(Long appId);

    /**
     * 分页查询某个用户的未删除聊天记录。
     */
    List<ChatHistoryDO> selectChatHistory(ChatHistoryReqParam chatHistoryReqParam);

    /**
     * 统计某个用户的未删除聊天记录数量。
     */
    Long countChatHistory(ChatHistoryReqParam chatHistoryReqParam);
}
