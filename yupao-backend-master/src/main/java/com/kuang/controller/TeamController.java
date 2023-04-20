package com.kuang.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kuang.common.BaseResponse;
import com.kuang.common.ErrorCode;
import com.kuang.common.ResultUtils;
import com.kuang.exception.BusinessException;
import com.kuang.model.domain.Team;
import com.kuang.model.domain.User;
import com.kuang.model.domain.UserTeam;
import com.kuang.model.dto.TeamQuery;
import com.kuang.model.request.*;
import com.kuang.model.vo.TeamUserVo;
import com.kuang.service.TeamService;
import com.kuang.service.UserService;
import com.kuang.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户注册接口
 * @author huazi
 */
@RestController
@RequestMapping("/team")
@CrossOrigin(origins = { "http://localhost:3000" })
@Slf4j
public class TeamController {

    @Resource
    private UserService userService;
    @Resource
    private TeamService teamService;

    @Resource
    private UserTeamService userTeamService;

    /**
     * 增加队伍
     * @param teamAddRequest
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request){
        //判断是否为空
        if (teamAddRequest == null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        //获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest,team);
        long teamId = teamService.addTeam(team,loginUser);
         return ResultUtils.success(teamId);
    }

    /**
     * 解散队伍
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request){
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        User loginUser = userService.getLoginUser(request);
        boolean save = teamService.deleteTeam(id,loginUser);
        if (!save){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"删除失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 修改队伍
     * @param teamUpdateRequest
     * @return
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest,HttpServletRequest request){
        //判断是否为空
        if (teamUpdateRequest == null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean updateById = teamService.updateTeam(teamUpdateRequest,loginUser);
        if (!updateById){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"修改失败");
        }
        return ResultUtils.success(true);
    }

    /**
     * 根据id查询队伍
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Team> getTeamById(int id){
        if (id<=0){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        Team result = teamService.getById(id);
        if (result==null){
            throw new BusinessException(ErrorCode.NULL_ERROR,"查询失败");
        }
        return ResultUtils.success(result);
    }

    /**
     * 以list形式查询队伍
     * @param teamQuery
     * @return
     */
    @GetMapping("/list")
    public BaseResponse<List<TeamUserVo>> listTeams(TeamQuery teamQuery,HttpServletRequest request){
        if (teamQuery==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean isAdmin = userService.isAdmin(request);
        List<TeamUserVo> teamList = teamService.listTeams(teamQuery,isAdmin);
        //判断当前用户是否加入队伍
        List<Long> teamIdlist = teamList.stream().map(TeamUserVo::getId).collect(Collectors.toList());
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        try {
            User loginUser = userService.getLoginUser(request);
            queryWrapper.eq("userId",loginUser.getId());
            //queryWrapper.in（“属性”，条件，条件 ）——符合多个条件的值
            queryWrapper.in("teamId",teamIdlist);
            List<UserTeam> userTeams = userTeamService.list(queryWrapper);
            //获取已加入队伍的集合id
            Set<Long> hasJoinTeamIdSet = userTeams.stream().map(UserTeam::getTeamId).collect(Collectors.toSet());
            //遍历展示的队伍列表
            teamList.forEach(team ->{
                //如果集合中有队伍id，说明已经加入了队伍，hasJoin返回true
                boolean hasJoin=hasJoinTeamIdSet.contains(team.getId());
                team.setHasjoin(hasJoin);
                    });
        }catch (Exception e){}
        //查询队伍人数
        QueryWrapper<UserTeam> userJoinTeamQueryWrapper = new QueryWrapper<>();
        userJoinTeamQueryWrapper.in("teamId",teamIdlist);
        List<UserTeam> userJoinTeamList = userTeamService.list(userJoinTeamQueryWrapper);
        //队伍id=>获取整个队伍用户信息表
        Map<Long, List<UserTeam>> teamIdUserTeamList = userJoinTeamList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        teamList.forEach(team ->{
            //遍历集合，更新加入队伍人数
            team.setHasJoinNum(teamIdUserTeamList.getOrDefault(team.getId(),new ArrayList<>()).size());
        });
        return ResultUtils.success(teamList);
    }
    /**
     *  分页查询队伍
     */

    @GetMapping("/list/page")
    public BaseResponse<Page<Team>> TeamPages(TeamQuery teamQuery){
        if (teamQuery==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = new Team();
        //将一个对象中的属性值赋值（拷贝）给另一个对象中对应的属性，并且对象之间可以没有任何联系
        BeanUtils.copyProperties(team,teamQuery);
        //获取页面大小和页数
        Page<Team> page=new Page<>(teamQuery.getPageNum(),teamQuery.getPageSize());
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>(team);
        //根据页数以及查询条件来获取队伍
        Page<Team> page1 = teamService.page(page, queryWrapper);
        return ResultUtils.success(page1);
    }

    /**
     * 用户加入队伍
     * @param teamJoinRequest
     * @param request
     * @return
     */

    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeams(@RequestBody TeamJoinRequest teamJoinRequest,HttpServletRequest request){
        if (teamJoinRequest==null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result=teamService.joinTeam(teamJoinRequest,loginUser);
        return ResultUtils.success(result);

    }

    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest, HttpServletRequest request){
        if (teamQuitRequest==null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result=teamService.quitTeam(teamQuitRequest,loginUser);
        return ResultUtils.success(result);

    }

    /**
     * 查询自己创建的队伍
     * @param teamQuery
     * @return
     */
    @GetMapping("/list/my/create")
    public BaseResponse<List<TeamUserVo>> listMyCreateTeams(TeamQuery teamQuery,HttpServletRequest request){
        if (teamQuery==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        teamQuery.setUserId(loginUser.getId());
        List<TeamUserVo> teamList = teamService.listTeams(teamQuery,true);
        return ResultUtils.success(teamList);
    }


    /**
     * 查询已加入的队伍
     * 先找到当前登录用户，根据当前登录用户的信息，在user-team表里
     * @param teamQuery
     * @return
     */
    @GetMapping("/list/my/join")
    public BaseResponse<List<TeamUserVo>> listMyJoinTeams(TeamQuery teamQuery,HttpServletRequest request){
        if (teamQuery==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        //根据登录用户的id查询
        queryWrapper.eq("userId",loginUser.getId());
        List<UserTeam> userTeamList = userTeamService.list(queryWrapper);
        //判断id是否重复，以userid作为键的map
         Map<Long,List<UserTeam>> listMap=userTeamList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
         //将map的键转化为list集合
        List<Long> idList = new ArrayList<>(listMap.keySet());
        teamQuery.setIdList(idList);
        List<TeamUserVo> teamList = teamService.listTeams(teamQuery,true);
        return ResultUtils.success(teamList);
    }



}



