package com.bitejiuyeke.portalservice.flash.service;

import com.bitejiuyeke.bitecommoncore.domain.dto.BasePageDTO;
import com.bitejiuyeke.portalservice.flash.domain.dto.require.ChatHistoryReqParam;
import com.bitejiuyeke.portalservice.flash.domain.dto.require.MyselfListReqParam;
import com.bitejiuyeke.portalservice.flash.domain.dto.require.SquareListReqParam;
import com.bitejiuyeke.portalservice.flash.domain.dto.result.ChatHistoryDTO;
import com.bitejiuyeke.portalservice.flash.domain.dto.result.MyselfAppDTO;
import com.bitejiuyeke.portalservice.flash.domain.dto.result.SquareAppDTO;
import org.springframework.validation.annotation.Validated;

/**
 *
 * @author Emrys
 * content:
 */
public interface IAppPageService {

    BasePageDTO<SquareAppDTO> getSquare(SquareListReqParam squareListDTO);

    BasePageDTO<MyselfAppDTO> getMyself(@Validated MyselfListReqParam myselfListDTO);

    BasePageDTO<ChatHistoryDTO> getChatHistory(ChatHistoryReqParam chatHistoryReqParam);
}
