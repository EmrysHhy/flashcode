package com.bitejiuyeke.biteportalservice.user.entity.vo;

import com.bitejiuyeke.bitecommondomain.domain.vo.LoginUserVO;
import lombok.Data;

/**
 * C端用户VO
 */
@Data
public class UserVo extends LoginUserVO {

    /**
     * 用户头像
     */
    private String avatar;

    /**
     * 昵称
     */
    private String nickName;
}
