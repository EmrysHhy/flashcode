package com.bitejiuyeke.biteadminservice.map.domain.dto;

import lombok.Data;

/**
 * 城市搜索地点查询条件
 */
@Data
public class SuggestSearchDTO {

    /**
     * 搜索的关键字
     */
    private String keyword;

    /**
     * 城市id
     */
    private String id;

    /**
     * 页码
     */
    private Integer pageIndex;

    /**
     * 每页的数量
     */
    private Integer pageSize;
}
