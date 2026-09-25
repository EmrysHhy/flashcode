package com.bitejiuyeke.biteportalservice.flash.service.implement;

import com.bitejiuyeke.bitecommoncore.domain.dto.BasePageDTO;
import com.bitejiuyeke.biteportalservice.flash.domain.dto.require.ChatHistoryReqParam;
import com.bitejiuyeke.biteportalservice.flash.domain.dto.require.MyselfListReqParam;
import com.bitejiuyeke.biteportalservice.flash.domain.dto.require.SquareListReqParam;
import com.bitejiuyeke.biteportalservice.flash.domain.dto.result.ChatHistoryDTO;
import com.bitejiuyeke.biteportalservice.flash.domain.dto.result.MyselfAppDTO;
import com.bitejiuyeke.biteportalservice.flash.domain.dto.result.SquareAppDTO;
import com.bitejiuyeke.biteportalservice.flash.domain.entity.AppDO;
import com.bitejiuyeke.biteportalservice.flash.domain.entity.ChatHistoryDO;
import com.bitejiuyeke.biteportalservice.flash.enums.DeployStatusEnum;
import com.bitejiuyeke.biteportalservice.flash.mapper.AppMapper;
import com.bitejiuyeke.biteportalservice.flash.mapper.ChatHistoryMapper;
import com.bitejiuyeke.biteportalservice.flash.service.IAppPageService;
import com.bitejiuyeke.biteportalservice.user.mapper.AppUserMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 *
 * @author Emrys
 * content:
 */
@Service
public class AppPageServiceImpl implements IAppPageService {
    @Autowired
    AppMapper appMapper;
    @Autowired
    AppUserMapper userMapper;
    @Autowired
    ChatHistoryMapper chatHistoryMapper;
    @Override
    public BasePageDTO<SquareAppDTO> getSquare(SquareListReqParam squareListDTO) {
        Integer deployed = DeployStatusEnum.DEPLOYED.getValue();
        Long totals = appMapper.countSquareApp(squareListDTO, deployed);
        BasePageDTO<SquareAppDTO> page = new BasePageDTO<>();
        page.setTotals(totals.intValue());
        page.setTotalPages(BasePageDTO.calculateTotalPages(totals, squareListDTO.getPageSize()));
        if (totals == 0) {
            page.setList(List.of());
            return page;
        }
        page.setList(appMapper.selectSquareApp(squareListDTO, deployed));
        return page;
    }


    @Override
    public BasePageDTO<ChatHistoryDTO> getChatHistory(ChatHistoryReqParam chatHistoryReqParam) {
        Long totals = chatHistoryMapper.countChatHistory(chatHistoryReqParam);
        BasePageDTO<ChatHistoryDTO> page = new BasePageDTO<>();
        page.setTotals(totals.intValue());
        page.setTotalPages(BasePageDTO.calculateTotalPages(totals, chatHistoryReqParam.getPageSize()));
        if (totals == 0) {
            page.setList(List.of());
            return page;
        }
        page.setList(chatHistoryMapper.selectChatHistory(chatHistoryReqParam).stream()
                .map(this::toChatHistoryDTO)
                .toList());
        return page;
    }

    private ChatHistoryDTO toChatHistoryDTO(ChatHistoryDO history) {
        ChatHistoryDTO chatHistoryDTO = new ChatHistoryDTO();
        chatHistoryDTO.setId(history.getId());
        chatHistoryDTO.setAppId(history.getAppId());
        chatHistoryDTO.setMsgRole(history.getChatRole() == null ? 0 : history.getChatRole());
        chatHistoryDTO.setContent(history.getContent());
        return chatHistoryDTO;
    }

    @Override
    public BasePageDTO<MyselfAppDTO> getMyself(MyselfListReqParam myselfListDTO) {
        Long totals = appMapper.countMyselfApp(myselfListDTO);
        BasePageDTO<MyselfAppDTO> page = new BasePageDTO<>();
        page.setTotals(totals.intValue());
        page.setTotalPages(BasePageDTO.calculateTotalPages(totals, myselfListDTO.getPageSize()));
        if (totals == 0) {
            page.setList(List.of());
            return page;
        }
        page.setList(appMapper.selectMyselfAppByUserId(myselfListDTO).stream()
                .map(this::toMyselfAppDTO)
                .toList());
        return page;
    }

    private MyselfAppDTO toMyselfAppDTO(AppDO app) {
        MyselfAppDTO myselfAppDTO = new MyselfAppDTO();
        BeanUtils.copyProperties(app, myselfAppDTO);
        return myselfAppDTO;
    }
}
