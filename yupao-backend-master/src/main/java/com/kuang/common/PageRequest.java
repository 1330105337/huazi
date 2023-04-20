package com.kuang.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 分页参数
 */
@Data
public class PageRequest implements Serializable{


    /**
     * 页面大小
     */
    protected int pageSize=10;
    /**
     * 当前页数
     */
    protected int pageNum=1;
}
