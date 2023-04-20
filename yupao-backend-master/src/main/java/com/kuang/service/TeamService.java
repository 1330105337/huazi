package com.kuang.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.kuang.model.domain.Team;
import com.kuang.model.domain.User;
import com.kuang.model.dto.TeamQuery;
import com.kuang.model.request.TeamJoinRequest;
import com.kuang.model.request.TeamQuitRequest;
import com.kuang.model.request.TeamUpdateRequest;
import com.kuang.model.vo.TeamUserVo;

import java.util.List;

/**
* @author 86187
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2023-02-27 12:30:28
*/
public interface TeamService extends IService<Team> {
    /**
     * 创建队伍
     * @param team
     * @param loginUser
     * @return
     */
    long addTeam(Team team, User loginUser);

    /**
     * 搜索队伍
     * @param teamQuery
     * @return
     */
    List<TeamUserVo> listTeams(TeamQuery teamQuery,boolean isAdmin);

    /**
     * 更新用户
     * @param teamUpdateRequest
     * @return
     */
    boolean updateTeam(TeamUpdateRequest teamUpdateRequest,User loginUser);

    /**
     * 加入队伍
     * @param teamJoinRequest
     * @return
     */
    boolean joinTeam(TeamJoinRequest teamJoinRequest,User loginUser);

    /**
     * 退出队伍
     * @param teamQuitRequest
     * @param loginUser
     * @return
     */
    boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser);

    /**
     * 解散队伍
     * @param id
     * @param loginUser
     * @return
     */
    boolean deleteTeam(long id,User loginUser);
}
