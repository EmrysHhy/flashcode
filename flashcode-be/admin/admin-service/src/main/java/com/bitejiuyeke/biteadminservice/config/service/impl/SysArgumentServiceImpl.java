package com.bitejiuyeke.biteadminservice.config.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bitejiuyeke.biteadminapi.config.domain.dto.ArgumentAddReqDTO;
import com.bitejiuyeke.biteadminapi.config.domain.dto.ArgumentDTO;
import com.bitejiuyeke.biteadminapi.config.domain.dto.ArgumentEditReqDTO;
import com.bitejiuyeke.biteadminapi.config.domain.dto.ArgumentListReqDTO;
import com.bitejiuyeke.biteadminapi.config.domain.vo.ArgumentVO;
import com.bitejiuyeke.biteadminservice.config.domain.entity.SysArgument;
import com.bitejiuyeke.biteadminservice.config.mapper.SysArgumentMapper;
import com.bitejiuyeke.biteadminservice.config.service.ISysArgumentService;
import com.bitejiuyeke.bitecommondomain.domain.vo.BasePageVO;
import com.bitejiuyeke.bitecommondomain.exception.ServiceException;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 参数服务实现类
 */
@Service
public class SysArgumentServiceImpl implements ISysArgumentService {

    @Resource
    private SysArgumentMapper sysArgumentMapper;

    @Override
    public Long add(ArgumentAddReqDTO argumentAddReqDTO) {
        // 1 查询数据库已有数据
        SysArgument sysArgument = sysArgumentMapper.selectOne(new LambdaQueryWrapper<SysArgument>()
                .eq(SysArgument::getConfigKey, argumentAddReqDTO.getConfigKey())
                .or()
                .eq(SysArgument::getName, argumentAddReqDTO.getName())
        );
        // 2 判断查到的结果，存在抛异常 不存在直接新增
        if (sysArgument != null) {
            throw new ServiceException("参数已存在");
        }
        sysArgument = new SysArgument();
        sysArgument.setName(argumentAddReqDTO.getName());
        sysArgument.setConfigKey(argumentAddReqDTO.getConfigKey());
        sysArgument.setValue(argumentAddReqDTO.getValue());
        if (StringUtils.isNotBlank(argumentAddReqDTO.getRemark())) {
            sysArgument.setRemark(argumentAddReqDTO.getRemark());
        }
        sysArgumentMapper.insert(sysArgument);
        // 3 返回结果
        return sysArgument.getId();
    }

    @Override
    public BasePageVO<ArgumentVO> list(ArgumentListReqDTO argumentListReqDTO) {
        // 1 先创建返回结果对象接受查询到的结果
        BasePageVO<ArgumentVO> result = new BasePageVO<>();
        // 2 执行查询逻辑
        LambdaQueryWrapper<SysArgument> queryWrapper = new LambdaQueryWrapper<>();
        // 3 业务主键不为空
        if (StringUtils.isNotBlank(argumentListReqDTO.getConfigKey())) {
            queryWrapper.eq(SysArgument::getConfigKey, argumentListReqDTO.getConfigKey());
        }
        // 4 参数名称不为空
        if (StringUtils.isNotBlank(argumentListReqDTO.getName())) {
            queryWrapper.likeRight(SysArgument::getName, argumentListReqDTO.getName());
        }
        // 5 进行分页查询
        Page<SysArgument> page = sysArgumentMapper.selectPage(new Page<>(argumentListReqDTO.getPageNo().longValue(), argumentListReqDTO.getPageSize().longValue()), queryWrapper);
        // 6 把查询到的结果赋值给result
        result.setTotals((int) page.getTotal());
        result.setTotalPages((int) page.getPages());
        List<ArgumentVO> list = new ArrayList<>();
        for (SysArgument sysArgument : page.getRecords()) {
            ArgumentVO argumentVO = new ArgumentVO();
            BeanUtils.copyProperties(sysArgument, argumentVO);
            list.add(argumentVO);
        }
        result.setList(list);
        return result;
    }

    @Override
    public Long edit(ArgumentEditReqDTO argumentEditReqDTO) {
        // 1 判断参数是否存在
        SysArgument sysArgument = sysArgumentMapper.selectOne(new LambdaQueryWrapper<SysArgument>()
                .eq(SysArgument::getConfigKey, argumentEditReqDTO.getConfigKey())
        );
        // 2 根据1的查询结果判空处理
        if (sysArgument == null) {
            throw new ServiceException("参数不存在");
        }
        // 3 做唯一性参数名称(name)校验
        if (sysArgumentMapper.selectOne(new LambdaQueryWrapper<SysArgument>()
                .eq(SysArgument::getName, argumentEditReqDTO.getName())
                .ne(SysArgument::getConfigKey, argumentEditReqDTO.getConfigKey())
        ) != null) {
            throw new ServiceException("参数名称存在冲突");
        }
        // 4 根据3的结果判空处理
        sysArgument.setName(argumentEditReqDTO.getName());
        sysArgument.setValue(argumentEditReqDTO.getValue());
        if (StringUtils.isNotBlank(sysArgument.getRemark())) {
            sysArgument.setRemark(sysArgument.getRemark());
        }
        sysArgumentMapper.updateById(sysArgument);
        return sysArgument.getId();
    }

    @Override
    public ArgumentDTO getByConfigKey(String configKey) {
        // 1 根据参数业务主键查询参数对象
        SysArgument sysArgument = sysArgumentMapper.selectOne(new LambdaQueryWrapper<SysArgument>().eq(SysArgument::getConfigKey, configKey));
        // 2 做对象转换
        if (sysArgument != null) {
            ArgumentDTO argumentDTO = new ArgumentDTO();
            BeanUtils.copyProperties(sysArgument, argumentDTO);
            return argumentDTO;
        }
        return null;
    }

    @Override
    public List<ArgumentDTO> getByConfigKeys(List<String> configKeys) {
        // 1 根据多个参数业务主键查询多个参数对象
        List<SysArgument> sysArguments = sysArgumentMapper.selectList(new LambdaQueryWrapper<SysArgument>().in(SysArgument::getConfigKey, configKeys));
        // 2 做对象转换
        if (!sysArguments.isEmpty()) {
            List<ArgumentDTO> result = new ArrayList<>();
            for (SysArgument sysArgument : sysArguments) {
                ArgumentDTO argumentDTO = new ArgumentDTO();
                BeanUtils.copyProperties(sysArgument, argumentDTO);
                result.add(argumentDTO);
            }
            return result;
        }
        return null;
    }
}
