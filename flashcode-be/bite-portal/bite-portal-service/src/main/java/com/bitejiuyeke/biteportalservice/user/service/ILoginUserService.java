package com.bitejiuyeke.biteportalservice.user.service;

import com.bitejiuyeke.biteadminapi.appuser.domain.dto.UserEditReqDTO;
import com.bitejiuyeke.bitecommonsecurity.domain.dto.TokenDTO;
import com.bitejiuyeke.biteportalservice.user.domain.dto.LoginDTO;
import com.bitejiuyeke.biteportalservice.user.domain.dto.UserDTO;

/**
 * 门户用户服务接口
 */
public interface ILoginUserService {

    TokenDTO login(LoginDTO loginDTO);

    String sendCode(String account);

    void edit(UserEditReqDTO userEditReqDTO);

    UserDTO getLoginUser();

    void logout();
}
