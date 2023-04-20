package com.kuang.model.vo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 队伍和用户信息封装类（脱敏）
 *
 */
@Data
public class TeamUserVo {

        /**
         * id
         */
        private Long id;

        /**
         * 队伍名称
         */
        private String name;

        /**
         * 描述
         */
        private String description;

        /**
         * 最大人数
         */
        private Integer maxNum;

        /**
         * 过期时间
         */
        private Date expireTime;

        /**
         * 用户id
         */
        private Long userId;

        /**
         * 0 - 公开，1 - 私有，2 - 加密
         */
        private Integer status;

        /**
         * 创建时间
         */
        private Date createTime;

        /**
         *更新时间
         */
        private Date updateTime;
        /**
         * 创建人信息
         */
        private UserVo createUser;
        /**
         * 用户是否加入队伍
         */
        private boolean hasjoin=false;
        /**
         * 加入队伍用户数
         */
        private Integer hasJoinNum;
        /**
         * 加入队伍的用户
         */
        private List<UserVo> userVOS;

}

