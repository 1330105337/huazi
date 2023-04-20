package com.kuang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kuang.common.ErrorCode;
import com.kuang.exception.BusinessException;
import com.kuang.mapper.TeamMapper;
import com.kuang.model.domain.Team;
import com.kuang.model.domain.User;
import com.kuang.model.domain.UserTeam;
import com.kuang.model.dto.TeamQuery;
import com.kuang.model.enums.TeamStatusEnum;
import com.kuang.model.request.TeamJoinRequest;
import com.kuang.model.request.TeamQuitRequest;
import com.kuang.model.request.TeamUpdateRequest;
import com.kuang.model.vo.TeamUserVo;
import com.kuang.model.vo.UserVo;
import com.kuang.service.TeamService;
import com.kuang.service.UserService;
import com.kuang.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


/**
 * @author 86187
 * @description 针对表【team(队伍)】的数据库操作Service实现
 * @createDate 2023-02-27 12:30:28
 */
@Service
//事务开启注解，在遇到异常的时候，直接返回事务，重新加载事务（利用回滚）
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
        implements TeamService {

    @Resource
    private UserTeamService userTeamService;


    @Resource
    private TeamMapper teamMapper;
    @Resource
    private UserService userService;
    @Resource
    private RedissonClient redissonClient;

    @Override
    public long addTeam(Team team, User loginUser) {
//        请求参数是否为空？
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "请求参数是否为空");
        }
//        是否登录，未登录不允许创建
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "未登录不允许创建");
        }
        Long userId = loginUser.getId();
//                校验信息
//        队伍人数 > 1 且 <= 20,Optional.ofNullable进行判空处理，如果为空默认参数为0
        int maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);
        if (maxNum < 1 || maxNum > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍人数超出指定范围");
        }
//        队伍标题 <= 20
        String name = team.getName();
        if (StringUtils.isBlank(name) || name.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍标题错误");
        }
//        描述 <= 512
        String description = team.getDescription();
        if (StringUtils.isNotBlank(description) && description.length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍描述过长");
        }
//        status 是否公开（int）不传默认为 0（公开）
        Integer status = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
        if (statusEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍状态不满足要求");
        }
//        如果 status 是加密状态，一定要有密码，且密码 <= 32
        String password = team.getPassword();
        if (TeamStatusEnum.SECRET.equals(statusEnum)) {
            if (StringUtils.isBlank(password) && password.length() > 32) {
                throw new BusinessException(ErrorCode.NULL_ERROR, "密码格式不正确");
            }
        }
//        超时时间 > 当前时间
        Date expireTime = team.getExpireTime();
        if (new Date().after(expireTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "超时");
        }
//        校验用户最多创建 5 个队伍
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        //查询userId等于已登录用户的id
        queryWrapper.eq("userId", userId);
        long counts = this.count(queryWrapper);
        if (counts >= 5) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "创建用户超出数量");
        }
//        插入队伍信息到队伍表
        team.setId(null);
        team.setUserId(userId);
        boolean result = this.save(team);
        Long teamId = team.getId();
        if (!result || teamId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "插入队伍信息失败");
        }
//        插入用户 => 队伍关系到关系表
        UserTeam userTeam = new UserTeam();
        userTeam.setTeamId(teamId);
        userTeam.setUserId(userId);
        userTeam.setJoinTime(new Date());
        result = userTeamService.save(userTeam);
        if (!result) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "插入队伍关系失败");
        }
        return teamId;
    }

    @Override
    public List<TeamUserVo> listTeams(TeamQuery teamQuery, boolean isAdmin) {
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        //默认查询所有用户
        if (teamQuery != null) {
            Long id = teamQuery.getId();
            if (id != null && id > 0) {
                queryWrapper.eq("id", id);
            }
            List<Long> idList = teamQuery.getIdList();
            if (!CollectionUtils.isEmpty(idList)) {
                queryWrapper.in("id", idList);
            }
            //新增关键词查询
            String searchText = teamQuery.getSearchText();
            if (StringUtils.isNotBlank(searchText)) {
                //表达式的意思是拼接sql语句，利用关键词模糊查询
                queryWrapper.and(qw -> qw.like("name", searchText)).or().like("description", searchText);
            }
            String name = teamQuery.getName();
            if (StringUtils.isNotBlank(name)) {
                //查询相似的名称
                queryWrapper.like("name", name);
            }
            String description = teamQuery.getDescription();
            if (StringUtils.isNotBlank(description)) {
                queryWrapper.like("description", description);
            }
            //查询最大人数相等的用户
            Integer maxNum = teamQuery.getMaxNum();
            if (maxNum != null && maxNum > 0) {
                queryWrapper.eq("maxNum", maxNum);
            }
            //根据用户查询数据用户
            Long userId = teamQuery.getUserId();
            if (userId != null && userId > 0) {
                queryWrapper.eq("userId", userId);
            }
            //不是管理员不能查看用户的状态
                Integer status = teamQuery.getStatus();
                TeamStatusEnum enumByValue = TeamStatusEnum.getEnumByValue(status);
                if (enumByValue == null) {
                    enumByValue = TeamStatusEnum.PUBLIC;
                }
                if (!isAdmin && !enumByValue.equals(TeamStatusEnum.PRIVATE)) {
                    throw new BusinessException(ErrorCode.NO_AUTH);
                }
                queryWrapper.eq("status", enumByValue.getValue());

        }

        //不展示过期时间expireTime=null 或者expireTime>now()
        //此句表示expireTime>now()后面是expireTime=null
        queryWrapper.and(qw -> qw.gt("expireTime", new Date())).or().isNull("expireTime");
        List<Team> teamList = this.list(queryWrapper);
        if (CollectionUtils.isEmpty(teamList)) {
             return new ArrayList<>();
        }
        List<TeamUserVo> teamUserVoList = new ArrayList<>();
        //关联查询加入队伍的信息
        //通过队伍查询结果遍历获取每个队伍id
        for (Team team : teamList) {
            //获取队伍id
            Long teamId = team.getId();
            //获取创建人信息
            Long userId = team.getUserId();
            if (userId == null) {
                continue;
            }
            UserTeam userTeam = userTeamService.getById(teamId);
            User user = userService.getById(userId);
            //将队伍细信息放入teamUservo中
            TeamUserVo teamUserVo = new TeamUserVo();
            if (team != null) {
                BeanUtils.copyProperties(team, teamUserVo);
            }
            //将用户信息放到userVo中
            UserVo userVo = new UserVo();
            if (user != null) {
                BeanUtils.copyProperties(user, userVo);
            }
            //设置创建人信息
            teamUserVo.setCreateUser(userVo);
            //脱敏用户信息
            List<User> users = teamMapper.SelectUsers(teamId);
            List<UserVo> userVOS=new ArrayList<>();
            for (User User:users) {
                //将用户信息放入userVo中
                UserVo UserVo = new UserVo();
                if (User != null) {
                    BeanUtils.copyProperties(User, UserVo);
                }
                userVOS.add(UserVo);
            }
            teamUserVo.setUserVOS(userVOS);

            teamUserVoList.add(teamUserVo);
        }
        return teamUserVoList;
    }

    @Override
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser) {
//        判断请求参数是否为空
        if (teamUpdateRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
//        查询队伍是否存在
        Long id = teamUpdateRequest.getId();
        if (id <= 0 || id == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        Team oldTeam = this.getById(id);
        if (oldTeam == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
//        只有管理员或者队伍的创建者可以修改
        if (oldTeam.getUserId() != loginUser.getId() && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
//        如果用户传入的新值和老值一致，就不用 update 了（可自行实现，降低数据库使用次数）
//        如果队伍状态改为加密，必须要有密码
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(teamUpdateRequest.getStatus());
        if (statusEnum.equals(TeamStatusEnum.PRIVATE)) {
            if (StringUtils.isBlank(teamUpdateRequest.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "加密房间必须设置密码");
            }
        }
//         更新成功
        Team updateTeam = new Team();
        BeanUtils.copyProperties(teamUpdateRequest, updateTeam);
        return this.updateById(updateTeam);
    }

//    @Override
//    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser) {
////        队伍必须存在，只能加入未满、未过期的队伍
//        Long teamId = teamJoinRequest.getTeamId();
//        if (teamId <= 0 || teamId == null) {
//            throw new BusinessException(ErrorCode.NULL_ERROR);
//        }
//        Team team = this.getById(teamId);
//        if (team == null) {
//            throw new BusinessException(ErrorCode.NULL_ERROR);
//        }
//        Date expireTime = team.getExpireTime();
//        if (expireTime != null && expireTime.before(new Date())) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
//        }
////        禁止加入私有的队伍
//        Integer status = team.getStatus();
//        TeamStatusEnum enumByValue = TeamStatusEnum.getEnumByValue(status);
//        if (enumByValue.equals(TeamStatusEnum.PRIVATE)) {
//            throw new BusinessException(ErrorCode.NO_AUTH, "没有权限访问");
//        }
////        如果加入的队伍是加密的，必须密码匹配才可以
//        String password = teamJoinRequest.getPassword();
//        if (TeamStatusEnum.SECRET.equals(enumByValue)) {
//            if (StringUtils.isBlank(password) || !password.equals(team.getPassword())) {
//                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不匹配");
//            }
//        }
//        //获取锁，通过redisson
//        RLock lock = redissonClient.getLock("yupao:join_time");
//        try {
//            while (true) {
//                //所的唯一性，只有一个线程能得到锁
//                if (lock.tryLock(0, 30000l, TimeUnit.MILLISECONDS)) {
//                    System.out.println("getLock" + Thread.currentThread().getId());
//                    //        用户最多加入 5 个队伍,关联表查询
//                    Long userId = loginUser.getId();
//                    QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
//                    userTeamQueryWrapper.eq("userId", userId);
//                    long hasjoin = userTeamService.count(userTeamQueryWrapper);
//                    if (hasjoin > 5) {
//                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "加入队伍超过最大限制");
//                    }
//                    //不能重复加入已加入的队伍
//                    userTeamQueryWrapper = new QueryWrapper<>();
//                    userTeamQueryWrapper.eq("teamId", teamId);
//                    userTeamQueryWrapper.eq("userId", userId);
//                    long hasjoinTeam = userTeamService.count(userTeamQueryWrapper);
//                    if (hasjoinTeam > 0) {
//                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "该用户已经加入队伍");
//                    }
//                    //用户加入未满队伍
//                    long joinusers = this.countTeamUserByTeamId(teamId);
//                    if (joinusers > team.getMaxNum()) {
//                        throw new BusinessException(ErrorCode.NULL_ERROR, "队伍已满");
//                    }
////        新增队伍 - 用户关联信息
//                    UserTeam userTeam = new UserTeam();
//                    userTeam.setTeamId(teamId);
//                    userTeam.setJoinTime(new Date());
//                    userTeam.setUserId(userId);
//                    return userTeamService.save(userTeam);
//                }
//            }
//        }
//       catch (Exception e) {
//            log.error("doCacheRemmendUser", e);
//           return false;
//        }
//           finally {
//            //只能释放自己的锁
//            if (lock.isHeldByCurrentThread()) {
//                System.out.println("unLock" + Thread.currentThread().getId());
//                lock.unlock();
//            }
//        }
//    }

    @Override
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser) {
        if (teamJoinRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = teamJoinRequest.getTeamId();
        Team team = getTeamById(teamId);
        Date expireTime = team.getExpireTime();
        if (expireTime != null && expireTime.before(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已过期");
        }
        Integer status = team.getStatus();
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(status);
        if (TeamStatusEnum.PRIVATE.equals(teamStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "禁止加入私有队伍");
        }
        String password = teamJoinRequest.getPassword();
        if (TeamStatusEnum.SECRET.equals(teamStatusEnum)) {
            if (StringUtils.isBlank(password) || !password.equals(team.getPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
            }
        }
        // 该用户已加入的队伍数量
        long userId = loginUser.getId();
        // 只有一个线程能获取到锁
        RLock lock = redissonClient.getLock("my:join_team");
        try {
            // 抢到锁并执行
            while (true) {
                if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                    System.out.println("getLock: " + Thread.currentThread().getId());
                    QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                    userTeamQueryWrapper.eq("userId", userId);
                    long hasJoinNum = userTeamService.count(userTeamQueryWrapper);
                    if (hasJoinNum > 5) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多创建和加入 5 个队伍");
                    }
                    // 不能重复加入已加入的队伍
                    userTeamQueryWrapper = new QueryWrapper<>();
                    userTeamQueryWrapper.eq("userId", userId);
                    userTeamQueryWrapper.eq("teamId", teamId);
                    long hasUserJoinTeam = userTeamService.count(userTeamQueryWrapper);
                    if (hasUserJoinTeam > 0) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户已加入该队伍");
                    }
                    // 已加入队伍的人数
                    long teamHasJoinNum = this.countTeamUserByTeamId(teamId);
                    if (teamHasJoinNum >= team.getMaxNum()) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "队伍已满");
                    }
                    // 修改队伍信息
                    UserTeam userTeam = new UserTeam();
                    userTeam.setUserId(userId);
                    userTeam.setTeamId(teamId);
                    userTeam.setJoinTime(new Date());
                    return userTeamService.save(userTeam);
                }
            }
        } catch (InterruptedException e) {
            log.error("doCacheRecommendUser error", e);
            return false;
        } finally {
            // 只能释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                System.out.println("unLock: " + Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser) {
//        校验请求参数
        if (teamQuitRequest == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
//        校验队伍是否存在
        Long teamId = teamQuitRequest.getTeamId();

        if (teamId <= 0 || teamId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }



//        校验我是否已加入队伍
            Long userId = loginUser.getId();
            UserTeam userTeam = new UserTeam();
            userTeam.setTeamId(teamId);
            userTeam.setUserId(userId);
            QueryWrapper<UserTeam> objectQueryWrapper = new QueryWrapper<>(userTeam);
            long count = userTeamService.count(objectQueryWrapper);
            if (count == 0) {
                throw new BusinessException(ErrorCode.NULL_ERROR, "该用户未加入队伍");
            }
//        如果队伍 只剩一人，队伍解散
            long teamHasJoinNum = this.countTeamUserByTeamId(teamId);
            if (teamHasJoinNum == 1) {
                //删除队伍和加入队伍所有的关系
                this.removeById(teamId);
//           //删除队伍关系
            } //还有其他人
            else {
                //是否为队长
                if (team.getUserId().equals(userId)) {
                    // 把队伍转移给最早加入的用户
                    // 1. 查询已加入队伍的所有用户和加入时间
                    QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                    userTeamQueryWrapper.eq("teamId", teamId);
                    userTeamQueryWrapper.last("order by id asc limit 2");
                    List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
                    if (CollectionUtils.isEmpty(userTeamList) || userTeamList.size() <= 1) {
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR);
                    }
                    UserTeam nextUserTeam = userTeamList.get(1);
                    Long nextTeamLeaderId = nextUserTeam.getUserId();
                    // 更新当前队伍的队长
                    Team updateTeam = new Team();
                    updateTeam.setId(teamId);
                    updateTeam.setUserId(nextTeamLeaderId);
                    boolean result = this.updateById(updateTeam);
                    if (!result) {
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新队伍队长失败");
                    }
                }


            }
            //移除旧的信息
            return userTeamService.remove(objectQueryWrapper);
        }


    @Override
    //防止误删或者出现脏数据，利用事务进行回滚
    @Transactional(rollbackFor = Exception.class)

    public boolean deleteTeam(long id, User loginUser) {
//        校验请求参数
//        校验队伍是否存在
        Team team = getTeamById(id);
        long teamId = team.getId();
        System.out.println(team.getUserId());
        System.out.println(loginUser.getId());
//        校验你是不是队伍的队长
        if (!team.getUserId().equals(loginUser.getId()) ) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该用户不是队长");
        }
//        移除所有加入队伍的关联信息
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId", teamId);
        boolean result = userTeamService.remove(queryWrapper);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除队伍失败");
        }
//        删除队伍
        return this.removeById(teamId);
    }

    /**
     * 判断队伍是否存在
     *
     * @param teamId
     * @return
     */

    private Team getTeamById(Long teamId) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team team = this.getById(teamId);
        log.info(team.toString());
        if (team == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR, "队伍不存在");
        }
        return team;
    }

    /**
     * 获取某队伍人数
     *
     * @param teamId
     * @return
     */
    private long countTeamUserByTeamId(long teamId) {
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("teamId", teamId);
        return userTeamService.count(userTeamQueryWrapper);
    }
}





