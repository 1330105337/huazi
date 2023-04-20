package com.kuang.utils.service;


import org.junit.jupiter.api.Test;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class RedissonTest {

    @Resource
    private RedissonClient redissonClient;
    @Test
    void test(){
        //list
        List<String> list=new ArrayList<>();
        list.add("yupi");
        System.out.println("list:"+ list.get(0));

        RList<Object> list1 = redissonClient.getList("list-null");
        list1.add("yupi");
        System.out.println("rlist" + list1.get(0));

    }
}
