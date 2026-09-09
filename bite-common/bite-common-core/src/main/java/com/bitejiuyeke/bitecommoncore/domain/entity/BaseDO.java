package com.bitejiuyeke.bitecommoncore.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.time.LocalDateTime;

/**
 *  do基类
 */
@Data
public class BaseDO {

    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 新增时间
     */
    private LocalDateTime createTime;

    /**
     * 更改时间
     */
    private LocalDateTime updateTime;

}
