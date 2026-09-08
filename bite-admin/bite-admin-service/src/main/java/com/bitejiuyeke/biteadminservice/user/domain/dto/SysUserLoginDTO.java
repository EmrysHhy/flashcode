package com.bitejiuyeke.biteadminservice.user.domain.dto;

import com.bitejiuyeke.biteadminservice.user.domain.vo.SysUserLoginVO;
import com.bitejiuyeke.bitecommonsecurity.domain.dto.LoginUserDTO;
import lombok.Data;
import org.springframework.beans.BeanUtils;

/**
 * B端登录用户信息DTO
 */
@Data
public class SysUserLoginDTO extends LoginUserDTO {

    /**
     * 昵称
     */
    private String nickName;

    /**
     * 身份
     */
    private String identity;

    /**
     * 状态
     */
    private String status;

    /**
     * B端用户登录信息DTO转VO
     * @return B端用户登录信息VO
     */
    public SysUserLoginVO convertToVO() {
        SysUserLoginVO sysUserLoginVO = new SysUserLoginVO();
        BeanUtils.copyProperties(this, sysUserLoginVO);
        return sysUserLoginVO;
    }
}
