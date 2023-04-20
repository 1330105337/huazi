package com.kuang.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kuang.model.domain.Team;
import com.kuang.model.domain.User;
import io.lettuce.core.dynamic.annotation.Param;

import java.util.List;

/**
* @author 86187
* @description 针对表【team(队伍)】的数据库操作Mapper
* @createDate 2023-02-27 12:30:28
* @Entity generator.domain.Team
*/
public interface TeamMapper extends BaseMapper<Team> {

    List<User> SelectUsers(@Param("teamId") long teamId);

}




