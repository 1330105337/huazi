package com.kuang.utils.service;

import com.kuang.model.domain.User;
import com.kuang.service.UserService;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

@SpringBootTest

class UserServiceTest {

    @Resource
    private UserService userService;

    @Test
    void testSearchUsersByTags() {
        //创建假数据
        List<String> tagList= Arrays.asList("java","python","c");
        //调用通过标签查询用户的方法
        List<User> userList = userService.searchUsersByTags(tagList);
        //判断是否存在这样的数据
        Assert.assertNotNull(userList);
    }

    @Test
    public void testAddUser() {
        User user = new User();
        user.setUsername("2260391948");
        user.setUserAccount("2260391948");
        user.setAvatarUrl("https://636f-codenav-8grj8px727565176-1256524210.tcb.qcloud.la/img/logo.png");
        user.setGender(0);
        user.setUserPassword("zxq1314mm");
        user.setPhone("123");
        user.setEmail("456");
        boolean result = userService.save(user);
        System.out.println(user.getId());
        Assertions.assertTrue(result);
    }

}