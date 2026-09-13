package com.bitejiuyeke.biteportalservice.user.entity.dto;

import com.bitejiuyeke.bitecommonsecurity.domain.dto.LoginUserDTO;
import com.bitejiuyeke.biteportalservice.user.entity.vo.UserVo;
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
     * 对象转换
     * @return
     */
    public UserVo convertToVO() {
        UserVo userVo = new UserVo();
        BeanUtils.copyProperties(this, userVo);
        userVo.setNickName(this.getUserName());
        return userVo;
    }
}
