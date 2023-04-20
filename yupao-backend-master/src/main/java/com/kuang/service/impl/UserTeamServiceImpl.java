package com.kuang.service.impl;




import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kuang.mapper.UserTeamMapper;
import com.kuang.model.domain.UserTeam;
import com.kuang.service.UserTeamService;
import org.springframework.stereotype.Service;

/**
* @author 86187
* @description 针对表【user_team(用户队伍关系)】的数据库操作Service实现
* @createDate 2023-02-27 12:31:26
*/
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService {

}




