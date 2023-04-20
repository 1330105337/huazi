package com.kuang.utils.controller;


import com.kuang.common.BaseResponse;
import com.kuang.controller.TeamController;
import com.kuang.model.dto.TeamQuery;
import com.kuang.model.vo.TeamUserVo;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;

@SpringBootTest
public class TeamControllerTest {

    @Resource
    private TeamController teamController;

    @Test
    public void test(){
        TeamQuery teamQuery=new TeamQuery();
//        BaseResponse<List<TeamUserVo>> teams = teamController.listTeams(teamQuery);
    }


}
