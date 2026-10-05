package com.bitejiuyeke.portalservice.user.domain.dto;

import com.bitejiuyeke.bitecommonsecurity.domain.dto.LoginUserDTO;
import com.bitejiuyeke.portalservice.user.domain.vo.UserVo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.BeanUtils;

/**
 * C端用户DTO
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserDTO extends LoginUserDTO {

    /**
     * 用户头像
     */
    private String avatar;

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 手机号
     */
    private String phoneNumber;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 对象转换
     * @return
     */
    public UserVo convertToVO() {
        UserVo userVo = new UserVo();
        BeanUtils.copyProperties(this, userVo);
        if (userVo.getNickName() == null || userVo.getNickName().isBlank()) {
            userVo.setNickName(this.getUserName());
        }
        return userVo;
    }
}
