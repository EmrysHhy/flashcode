package com.bitejiuyeke.biteadminservice.config.service;

import com.bitejiuyeke.biteadminapi.config.domain.dto.ArgumentAddReqDTO;
import com.bitejiuyeke.biteadminapi.config.domain.dto.ArgumentDTO;
import com.bitejiuyeke.biteadminapi.config.domain.dto.ArgumentEditReqDTO;
import com.bitejiuyeke.biteadminapi.config.domain.dto.ArgumentListReqDTO;
import com.bitejiuyeke.biteadminapi.config.domain.vo.ArgumentVO;
import com.bitejiuyeke.bitecommondomain.domain.vo.BasePageVO;

import java.util.List;

/**
 * 参数服务相关接口
 */
public interface ISysArgumentService {

    /**
     * 新增参数
     * @param argumentAddReqDTO 添加参数DTO
     * @return 参数ID
     */
    Long add(ArgumentAddReqDTO argumentAddReqDTO);

    /**
     * 参数列表
     * @param argumentListReqDTO 查看参数DTO
     * @return BasePageVO
     */
    BasePageVO<ArgumentVO> list(ArgumentListReqDTO argumentListReqDTO);

    /**
     * 编辑参数
     * @param argumentEditReqDTO 编辑参数DTO
     * @return Long
     */
    Long edit(ArgumentEditReqDTO argumentEditReqDTO);

    /**
     * 根据参数键查询参数对象
     * @param configKey 参数键
     * @return 参数对象
     */
    ArgumentDTO getByConfigKey(String configKey);

    /**
     * 根据多个参数键查询多个参数对象
     * @param configKeys 多个参数键
     * @return 多个参数对象
     */
    List<ArgumentDTO> getByConfigKeys(List<String> configKeys);
}
