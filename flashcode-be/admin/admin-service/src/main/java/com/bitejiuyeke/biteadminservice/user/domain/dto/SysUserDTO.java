package com.bitejiuyeke.biteadminservice.user.domain.dto;

import com.bitejiuyeke.biteadminservice.user.domain.vo.SysUserVo;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * B端用户信息
 */
@Data
public class SysUserDTO {

    /**
     * B端人员用户ID
     */
    private Long userId;

    /**
     * 身份
     */
    private String identity;

    /**
     * 手机号
     */
    private String phoneNumber;

    /**
     * 密码
     */
    private String password;

    /**
     * 昵称
     */
    @NotBlank(message = "昵称不能为空")
    private String nickName;

    /**
     * 状态
     */
    @NotBlank(message = "状态不能为空")
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 校验密码是否合理
     * @return 布尔类型
     */
    public boolean checkPassword() {
        if (StringUtils.isEmpty(this.password)) {
            return false;
        }
        if (this.password.length() > 20) {
            return false;
        }
        return this.password.matches("^[a-zA-Z0-9]+$");
    }

    /**
     * DTO转换VO
     * @return B端用户VO
     */
    public SysUserVo convertToVO() {
        SysUserVo sysUserVo = new SysUserVo();
        sysUserVo.setUserId(this.userId);
        sysUserVo.setIdentity(this.identity);
        sysUserVo.setPhoneNumber(this.phoneNumber);
        sysUserVo.setNickName(this.nickName);
        sysUserVo.setStatus(this.status);
        sysUserVo.setRemark(this.remark);
        return sysUserVo;
    }
}
