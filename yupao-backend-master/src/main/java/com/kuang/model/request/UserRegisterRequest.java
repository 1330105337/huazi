package com.kuang.model.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求体
 */
@Data
public class UserRegisterRequest implements Serializable{

    private static final long serialVersionUID = 6355403529808431774L;

    private String userAccount;

    private String userPassword;

    private String checkPassword;

    private String planteCode;

    private String userEmail;

    //邮箱编码
    private  String code;

}
