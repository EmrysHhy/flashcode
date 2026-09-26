package com.bitejiuyeke.bitecommondomain.domain.vo;


import lombok.Data;

import java.util.List;

/**
 * 分页VO
 * @param <T> 报文类型
 */
@Data
public class BasePageVO<T> {
    /**
     * 查询结果总数
     */
    Integer totals;

    /**
     * 总页数
     */
    Integer totalPages;

    /**
     * 数据列表
     */
    List<T> list;
}
