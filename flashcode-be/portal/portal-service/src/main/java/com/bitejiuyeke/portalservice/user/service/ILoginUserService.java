package com.bitejiuyeke.portalservice.user.service;

import com.bitejiuyeke.biteadminapi.appuser.domain.dto.UserEditReqParam;
import com.bitejiuyeke.bitecommonsecurity.domain.dto.TokenDTO;
import com.bitejiuyeke.portalservice.user.domain.dto.LoginDTO;
import com.bitejiuyeke.portalservice.user.domain.dto.UserDTO;

/**
 * 门户用户服务接口
 */
public interface ILoginUserService {

    TokenDTO login(LoginDTO loginDTO);

    String sendCode(String account);

    void edit(UserEditReqParam userEditParam);

    UserDTO getLoginUser();

    void logout();
}
