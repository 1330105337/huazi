package com.kuang.model.domain;


import lombok.Data;

@Data
public class UserVer {

    private String username;

    private String userPassword;

    private int age;

    private String email;

    private String sex;

    private String addr;

    //    验证码
    private String code;
}

