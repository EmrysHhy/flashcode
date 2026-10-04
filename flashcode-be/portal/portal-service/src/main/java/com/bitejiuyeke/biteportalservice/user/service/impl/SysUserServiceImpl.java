package com.bitejiuyeke.biteportalservice.user.service.impl;

import cn.hutool.crypto.digest.DigestUtil;

import com.bitejiuyeke.biteadminapi.config.feign.DictionaryFeignClient;
import com.bitejiuyeke.bitecommoncore.utils.AESUtil;
import com.bitejiuyeke.bitecommoncore.utils.VerifyUtil;
import com.bitejiuyeke.bitecommondomain.domain.ResultCode;
import com.bitejiuyeke.bitecommondomain.exception.ServiceException;
import com.bitejiuyeke.bitecommonsecurity.domain.dto.LoginUserDTO;
import com.bitejiuyeke.bitecommonsecurity.domain.dto.TokenDTO;
import com.bitejiuyeke.bitecommonsecurity.service.TokenService;
import com.bitejiuyeke.biteportalservice.user.domain.dto.PasswordLoginDTO;
import com.bitejiuyeke.biteportalservice.user.domain.dto.SysUserDTO;
import com.bitejiuyeke.biteportalservice.user.domain.dto.SysUserListReqDTO;
import com.bitejiuyeke.biteportalservice.user.domain.dto.SysUserLoginDTO;
import com.bitejiuyeke.biteportalservice.user.domain.entity.SysUser;
import com.bitejiuyeke.biteportalservice.user.mapper.SysUserMapper;
import com.bitejiuyeke.biteportalservice.user.service.ISysUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * B端用户登录服务实现类
 */
@Service
public class SysUserServiceImpl implements ISysUserService {

    /**
     * B端用户表的mapper
     */
    @Autowired
    private SysUserMapper sysUserMapper;

    /**
     * token服务类
     */
    @Autowired
    private TokenService tokenService;
    @Autowired
    private DictionaryFeignClient sysDictionaryService;
    /**
     * B端用户账户密码登录
     *
     * @param passwordLoginDTO 用户登录DTO
     * @return tokenDTO token信息
     */
    @Override
    public TokenDTO login(PasswordLoginDTO passwordLoginDTO) {
        // 需要用户登录的生命周期LoginUserDTO
        LoginUserDTO loginUserDTO = new LoginUserDTO();

        // 1 校验参数：手机号是否合理
        if (!VerifyUtil.checkPhone(passwordLoginDTO.getPhone())) {
            throw new ServiceException("手机号不合理", ResultCode.INVALID_PARA.getCode());
        }

        // 接着判断手机号是否存在于mysql
        SysUser sysUser = sysUserMapper.selectByPhoneNumber(
                AESUtil.encryptHex(passwordLoginDTO.getPhone()));
        if (sysUser == null) {
            throw new ServiceException("手机号不存在", ResultCode.INVALID_PARA.getCode());
        }

        // 2 校验密码
        // 先解密
        String password = AESUtil.decryptHex(passwordLoginDTO.getPassword());
        if (StringUtils.isEmpty(password)) {
            throw new ServiceException("密码解析为空", ResultCode.INVALID_PARA.getCode());
        }
        String passwordEncrypt = DigestUtil.sha256Hex(password);
        // 拿加密后的密码与数据库字段比较
        if (!passwordEncrypt.equals(sysUser.getPassword())) {
            throw new ServiceException("密码不正确", ResultCode.INVALID_PARA.getCode());
        }

        // 3 校验用户状态
        if (sysUser.getStatus().equals("disable")) {
            throw new ServiceException(ResultCode.USER_DISABLE);
        }

        // 4 设置登录信息
        loginUserDTO.setUserId(sysUser.getId());
        loginUserDTO.setUserName(sysUser.getNickName());
        loginUserDTO.setUserFrom("sys");

        return tokenService.createToken(loginUserDTO);
    }

    /**
     * 新增与编辑接口的实现方法
     * @param sysUserDTO B端用户信息DTO
     * @return 用户ID
     */
    @Override
    public Long addOrEdit(SysUserDTO sysUserDTO) {
        // 1 创建一个空的SysUser对象
        SysUser sysUser = new SysUser();

        // 2 先处理新增的逻辑
        if (sysUserDTO.getUserId() == null) {
            // 3 先校验手机号
            if (!VerifyUtil.checkPhone(sysUserDTO.getPhoneNumber())) {
                throw new ServiceException("手机格式错误", ResultCode.INVALID_PARA.getCode());
            }
            // 4 校验密码
            if (StringUtils.isEmpty(sysUserDTO.getPassword()) || !sysUserDTO.checkPassword()) {
                throw new ServiceException("密码校验失败", ResultCode.INVALID_PARA.getCode());
            }
            // 5 手机号唯一性判断
            SysUser existSysUser = sysUserMapper.selectByPhoneNumber(AESUtil.encryptHex(sysUserDTO.getPhoneNumber()));
            if (existSysUser != null) {
                throw new ServiceException("手机号已经被占用", ResultCode.INVALID_PARA.getCode());
            }
            // 6 判断身份信息
            if (StringUtils.isEmpty(sysUserDTO.getIdentity()) || sysDictionaryService.getDicDataByKey(sysUserDTO.getIdentity()) == null) {
                throw new ServiceException("用户身份错误", ResultCode.INVALID_PARA.getCode());
            }
            // 7 判断完成后，执行新增用户逻辑
            sysUser.setPhoneNumber(
                    AESUtil.encryptHex(sysUserDTO.getPhoneNumber())
            );
            sysUser.setPassword(
                    DigestUtil.sha256Hex(sysUserDTO.getPassword())
            );
            sysUser.setIdentity(sysUserDTO.getIdentity());
        }
        sysUser.setId(sysUserDTO.getUserId());
        sysUser.setNickName(sysUserDTO.getNickName());
        // 8 判断用户状态
        if (sysDictionaryService.getDicDataByKey(sysUserDTO.getStatus()) == null) {
            throw new ServiceException("用户状态错误", ResultCode.INVALID_PARA.getCode());
        }
        sysUser.setStatus(sysUserDTO.getStatus());
        sysUser.setRemark(sysUserDTO.getRemark());
        sysUserMapper.insertOrUpdate(sysUser);

        // 9 踢人
        if (sysUserDTO.getUserId() != null && sysUserDTO.getStatus().equals("disable")) {
            tokenService.delLoginUser(sysUserDTO.getUserId(), "sys");
        }
        return sysUser.getId();
    }

    /**
     * 查询B端用户
     * @param sysUserListReqDTO 用户查询DTO
     * @return B用户列表
     */
    @Override
    public List<SysUserDTO> getUserList(SysUserListReqDTO sysUserListReqDTO) {
        // 1 先构建查询对象
        SysUser searchSysUser = new SysUser();
        searchSysUser.setId(sysUserListReqDTO.getUserId());
        searchSysUser.setStatus(sysUserListReqDTO.getStatus());
        searchSysUser.setPhoneNumber(
                AESUtil.encryptHex(sysUserListReqDTO.getPhoneNumber())
        );

        // 2 执行查询SQL
        List<SysUser> sysUserList = sysUserMapper.selectList(searchSysUser);

        // 3 对查询结果封装转换
        return sysUserList.stream()
                .map(sysUser -> {
                    SysUserDTO sysUserDTO = new SysUserDTO();
                    sysUserDTO.setUserId(sysUser.getId());
                    sysUserDTO.setPhoneNumber(
                            AESUtil.decryptHex(sysUser.getPhoneNumber())
                    );
                    sysUserDTO.setNickName(sysUser.getNickName());
                    sysUserDTO.setIdentity(sysUser.getIdentity());
                    sysUserDTO.setStatus(sysUser.getStatus());
                    sysUserDTO.setRemark(sysUser.getRemark());
                    return sysUserDTO;
                }).collect(Collectors.toList());
    }

    /**
     * 获取B端登录用户信息
     * @return B端用户信息DTO
     */
    @Override
    public SysUserLoginDTO getLoginUser() {
        // 1 获取当前登录用户
        LoginUserDTO loginUserDTO = tokenService.getLoginUser();
        // 2 对象判断
        if (loginUserDTO == null || loginUserDTO.getUserId() == null) {
            throw new ServiceException("用户令牌有误", ResultCode.INVALID_PARA.getCode());
        }
        // 3 查询mysql
        SysUser sysUser = sysUserMapper.selectById(loginUserDTO.getUserId());
        if (sysUser == null) {
            throw new ServiceException("用户不存在", ResultCode.INVALID_PARA.getCode());
        }
        // 4 封装结果
        SysUserLoginDTO sysUserLoginDTO = new SysUserLoginDTO();
        BeanUtils.copyProperties(loginUserDTO, sysUserLoginDTO);
        BeanUtils.copyProperties(sysUser, sysUserLoginDTO);
        return sysUserLoginDTO;
    }
}
