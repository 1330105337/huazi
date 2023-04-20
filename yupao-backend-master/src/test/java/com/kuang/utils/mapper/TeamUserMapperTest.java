package com.kuang.utils.mapper;

import com.kuang.mapper.TeamMapper;
import com.kuang.mapper.UserTeamMapper;
import com.kuang.model.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TeamUserMapperTest {

    @Resource
    private TeamMapper teamMapper;

    @Resource
    private UserTeamMapper userTeamMapper;

    @Test
    public void test(){
        List<User> users = teamMapper.SelectUsers(15);
        System.out.println(users.toString());
    }



}
