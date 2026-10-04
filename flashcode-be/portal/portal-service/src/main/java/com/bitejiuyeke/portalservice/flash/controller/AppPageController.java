package com.bitejiuyeke.portalservice.flash.controller;

import com.bitejiuyeke.bitecommoncore.domain.dto.BasePageDTO;
import com.bitejiuyeke.bitecommoncore.utils.BeanCopyUtil;
import com.bitejiuyeke.bitecommondomain.domain.R;
import com.bitejiuyeke.bitecommondomain.domain.vo.BasePageVO;
import com.bitejiuyeke.portalservice.flash.domain.dto.require.ChatHistoryReqParam;
import com.bitejiuyeke.portalservice.flash.domain.dto.require.MyselfListReqParam;
import com.bitejiuyeke.portalservice.flash.domain.dto.require.SquareListReqParam;
import com.bitejiuyeke.portalservice.flash.domain.dto.result.ChatHistoryDTO;
import com.bitejiuyeke.portalservice.flash.domain.dto.result.MyselfAppDTO;
import com.bitejiuyeke.portalservice.flash.domain.dto.result.SquareAppDTO;
import com.bitejiuyeke.portalservice.flash.domain.vo.ChatHistoryVO;
import com.bitejiuyeke.portalservice.flash.domain.vo.MyselfListVO;
import com.bitejiuyeke.portalservice.flash.domain.vo.SquareListVO;
import com.bitejiuyeke.portalservice.flash.service.IAppPageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.function.Supplier;

/**
 * 应用页面控制器
 * @author Emrys
 * content:
 */
@RequestMapping("/list")
@RestController
public class AppPageController {
    @Autowired
    IAppPageService appPageService;
    /**
     * 查询C端用户
     * @param squareListDTO 查询C端用户DTO
     * @return C端用户分页结果
     */
    @RequestMapping("/square")
    public R<BasePageVO<SquareListVO>> square(@RequestBody SquareListReqParam squareListDTO) {
        BasePageDTO<SquareAppDTO> page = appPageService.getSquare(squareListDTO);
        return R.ok(toPageVO(page, SquareListVO::new));
    }
    /**
     * 查询C端用户
     * @param myselfListDTO 查询C端用户DTO
     * @return C端用户分页结果
     */
    @RequestMapping("/personal")
    public R<BasePageVO<MyselfListVO>> myself(@RequestBody @Validated MyselfListReqParam myselfListDTO) {
        BasePageDTO<MyselfAppDTO> page = appPageService.getMyself(myselfListDTO);
        return R.ok(toPageVO(page, MyselfListVO::new));
    }
    /**
     * 查询历史记录
     */
    @RequestMapping("/chat_history")
    public R<BasePageVO<ChatHistoryVO>> chatHistory(@RequestBody @Validated ChatHistoryReqParam chatHistoryReqParam) {
        BasePageDTO<ChatHistoryDTO> page = appPageService.getChatHistory(chatHistoryReqParam);
        return R.ok(toPageVO(page, ChatHistoryVO::new));
    }

    private <S, T> BasePageVO<T> toPageVO(BasePageDTO<S> page, Supplier<T> target) {
        BasePageVO<T> result = new BasePageVO<>();
        BeanCopyUtil.copyProperties(page, result);
        result.setList(BeanCopyUtil.copyListProperties(page.getList(), target));
        return result;
    }

}
