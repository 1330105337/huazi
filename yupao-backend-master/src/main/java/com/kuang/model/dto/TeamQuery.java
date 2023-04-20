package com.kuang.model.dto;

import com.kuang.common.PageRequest;
import lombok.Data;

import java.util.List;

/**
 * 队伍查询封装类
 */
@Data
//@EqualsAndHashCode(callSuper = true)
public class TeamQuery extends PageRequest {
    /**
     * id
     */
    private Long id;

    /**
     * id
     */
    private List<Long> idList;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 队伍名称
     */
    private String searchText;
    /**
     * 描述
     */
    private String description;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;
}
