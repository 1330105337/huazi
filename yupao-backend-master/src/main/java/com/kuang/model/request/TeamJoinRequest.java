package com.kuang.model.request;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 用户加入队伍请求体
 */
@Data
public class TeamJoinRequest {

        /**
         * id
         */
        private Long teamId;

        /**
         * 用户密码
         */
        private String password;



}
