package com.bitejiuyeke.biteportalservice.flash.config;

import com.bitejiuyeke.bitecommoncore.utils.BeanCopyUtil;
import com.bitejiuyeke.bitecommonredis.service.RedisService;
import com.bitejiuyeke.biteportalservice.flash.constants.FlashcodeConstant;
import com.bitejiuyeke.biteportalservice.flash.domain.dto.result.RedisChatHistoryDTO;
import com.bitejiuyeke.biteportalservice.flash.domain.entity.ChatHistoryDO;
import com.bitejiuyeke.biteportalservice.flash.mapper.ChatHistoryMapper;
import com.bitejiuyeke.biteportalservice.flash.enums.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 *
 * @author Emrys
 * content:
 */
@Configuration
@Slf4j
public class RedisChatMemoryConfig implements ChatMemory {
    @Value("${chat.memory.max-massage:10}")
    private Integer maxMassage;
    /**
     * 超时删除
     */
    @Value("${chat.memory.ttl:12}") //单位为h
    private Integer ttl;

    @Autowired
    RedisService redisService;

    @Autowired
    ChatHistoryMapper chatHistoryMapper;
    /**
     * redis缓存过期时间，单位：秒
     */
    public static final long REDIS_CACHE_EXPIRE_TIME = 60 * 60 * 24 * 7; // 7天

    @Override
    public void add(String conversationId, List<Message> messages) {
        Long appId = parseAppId(conversationId);
        if (appId == null) {
            log.warn("conversationId 不是 appId，跳过写入聊天记忆: {}", conversationId);
            return;
        }
        String key = FlashcodeConstant.REDIS_CHAT_HISTORY_PRE + appId;
        if (messages.isEmpty()) {
            log.warn("conversationId or messages is null or empty, cannot add to chat memory.");
            return;
        }
        for (Message message : messages) {
            //存入数据库
            ChatHistoryDO chatHistoryDO = new ChatHistoryDO();
            chatHistoryDO.setAppId(appId);
            if (message instanceof UserMessage userMessage) {
                chatHistoryDO.setChatRole(Role.USER.getValue());
                chatHistoryDO.setContent(userMessage.getText());
            } else if (message instanceof AssistantMessage assistantMessage) {
                chatHistoryDO.setChatRole(Role.LLM.getValue());
                chatHistoryDO.setContent(assistantMessage.getText());
            }
            chatHistoryMapper.insert(chatHistoryDO);

            //转成DTO
            RedisChatHistoryDTO redisChatHistoryDTO = new RedisChatHistoryDTO();
            BeanCopyUtil.copyProperties(chatHistoryDO, redisChatHistoryDTO);
            //存入redis
            redisService.rightPushForList(key, chatHistoryDO);
            redisService.trimList(key, maxMassage); //只保留后面几个消息
            redisService.expire(key, ttl, TimeUnit.HOURS);
        }

    }

    /**
     * 得到List<消息>
     * @param conversationId
     * @return
     */
    @Override
    public List<Message> get(String conversationId) {
        Long appId = parseAppId(conversationId);
        if (appId == null) {
            return List.of();
        }
        String key = FlashcodeConstant.REDIS_CHAT_HISTORY_PRE + appId;
        // 先从redis中获取
        List<RedisChatHistoryDTO> redisChatHistories = redisService.getCacheListByRange(key, 0, maxMassage-1, RedisChatHistoryDTO.class);
        if(redisChatHistories != null && !redisChatHistories.isEmpty()){
            log.info("从redis中获取到历史消息，appId: {}, 消息数量: {}", appId, redisChatHistories.size());
            return convertToMessages(redisChatHistories);
        }
        //拿不到就从数据库获取
        log.info("redis缓存中没有,从数据库中获取历史消息，appId: {}", appId);
        List<ChatHistoryDO> lastMesByAppId = chatHistoryMapper.getLastMesByAppId(appId, maxMassage);
        if (lastMesByAppId == null || lastMesByAppId.isEmpty()) {
            return List.of();
        }
        redisChatHistories = BeanCopyUtil.copyListProperties(lastMesByAppId, RedisChatHistoryDTO::new);
        if (redisChatHistories == null || redisChatHistories.isEmpty()) {
            return List.of();
        }
        redisService.setCacheList(key, redisChatHistories);
        redisService.trimList(key, maxMassage); //只保留后面几个消息
        redisService.expire(key, ttl, TimeUnit.HOURS);
        return convertToMessages(redisChatHistories);
    }



    @Override
    public void clear(String conversationId) {
        Long appId = parseAppId(conversationId);
        if (appId == null) {
            return;
        }
        //1.清除redis数据
        String key = FlashcodeConstant.REDIS_CHAT_HISTORY_PRE + appId;
        redisService.deleteObject(key);
        //3.是否清除数据库数据? 还是说用 字段 标记可用或者不可用? 我认为应该采用标记的方式
        chatHistoryMapper.deleteByAppId(appId);
    }

    /**
     * 会话 ID 必须是 appId。Spring AI 未传参时会用 "default"，不能 parseLong。
     */
    private static Long parseAppId(String conversationId) {
        if (conversationId == null || conversationId.isBlank() || "default".equals(conversationId)) {
            return null;
        }
        try {
            return Long.parseLong(conversationId);
        } catch (NumberFormatException e) {
            return null;
        }
    }


    private List<Message> convertToMessages(List<RedisChatHistoryDTO> redisChatHistories) {
        if (redisChatHistories == null || redisChatHistories.isEmpty()) {
            return List.of();
        }
        return redisChatHistories.stream()
                .filter(dto -> dto != null && dto.getContent() != null)
                .map(dto -> Role.USER.getValue().equals(dto.getChatRole())
                        ? (Message) new UserMessage(dto.getContent())
                        : new AssistantMessage(dto.getContent()))
                .collect(Collectors.toList());
    }
}
