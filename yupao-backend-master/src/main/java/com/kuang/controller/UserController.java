package com.kuang.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kuang.common.BaseResponse;
import com.kuang.common.ErrorCode;
import com.kuang.common.ResultUtils;
import com.kuang.exception.BusinessException;
import com.kuang.model.domain.User;
import com.kuang.model.request.UserLoginRequest;
import com.kuang.model.request.UserRegisterRequest;
import com.kuang.model.vo.UserSendMessage;
import com.kuang.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import static com.kuang.contant.UserContant.USER_LOGIN_STATE;

/**
 * 用户注册接口
 */
@RestController
@RequestMapping("/user")
@CrossOrigin(origins = { "http://localhost:3000" })
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    @PostMapping("/sendMessage")
    public BaseResponse<Boolean> sendMessage(@RequestBody UserSendMessage userSendMessage) {
        log.info("userSendMessage:"+userSendMessage.toString());
        return userService.sendMessage(userSendMessage);
    }

    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest){
        if (userRegisterRequest == null){
          throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String planetCode=userRegisterRequest.getPlanteCode();
        String userEmail = userRegisterRequest.getUserEmail();
        String code = userRegisterRequest.getCode();
        if (StringUtils.isAnyBlank(userAccount,userPassword,checkPassword,planetCode,code,userEmail)){
            throw new BusinessException(ErrorCode.NULL_ERROR,"不存在");
        }
        long result= userService.userRegister(userAccount,userPassword,checkPassword,planetCode,code,userEmail);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录mysql
     * @param
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<User> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request){
       log.info(userLoginRequest.toString());
        if (userLoginRequest ==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        if (StringUtils.isAnyBlank(userAccount,userPassword)){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        User user = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(user);
    }
    /**
     * 获取当前登录用户
     */
    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request){
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser=(User) userObj;
        if (currentUser==null){
            throw new BusinessException(ErrorCode.NULL_ERROR,"用户为空");
        }
        long userId = currentUser.getId();
        User user = userService.getById(userId);
        //返回一个脱敏后的user
        User safetyUser = userService.getSafetyUser(user);
        return ResultUtils.success(safetyUser);
    }

    /**
     * 查询用户
     */
    @GetMapping("/search")
    public BaseResponse<List<User>> searchUsers(String username,HttpServletRequest request){

        QueryWrapper<User> queryWrapper=new QueryWrapper<>();
        //判断username是否为空
        if (StringUtils.isNotBlank(username)){
            //模糊查询
           queryWrapper.like("username",username);
        }
        //遍历userlist，将数据流---
        List<User> userList = userService.list(queryWrapper);
        List<User> list = userList.stream().map(user -> userService.getSafetyUser(user)).collect(Collectors.toList());
        return ResultUtils.success(list);
    }

    /**
     * 删除用户
     * @param
     * @return
     */
    @GetMapping("/delete")
    public BaseResponse<Boolean> deleteUsers(@RequestBody long id,HttpServletRequest request){
        if (!userService.isAdmin(request)){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        if (id<=0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 匹配用户
     * @param num
     * @param request
     * @return
     */
    @GetMapping("/match")
    public List<User> matchUsers(long num, HttpServletRequest request){
        if (num==0 || num>20){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        return userService.matchUsers(num,loginUser);
    }


    /**
     * 用户注销
     */
    @PostMapping("/logout")
    public BaseResponse<Integer> userLogout(HttpServletRequest request){
        if (request ==null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
       int result= userService.userLogout(request);
        return ResultUtils.success(result);
    }
    /**
     * 根据标签搜索用户
     */
    @GetMapping("/search/tags")
    public BaseResponse<List<User>> searchUsersByTags(@RequestParam(required = false) List<String> tagNameList){
        //判断是否为空
        if (CollectionUtils.isEmpty(tagNameList)){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        //通过前端提供的标签tagNameList利用方法进行搜索
        List<User> userList = userService.searchUsersByTags(tagNameList);
        //返回成功信息userList
        return ResultUtils.success(userList);
    }
    /**
     * 更新用户数据
     */
    @PostMapping("/update")
    //请求时传递json数据，需要加个RequestBody
    public BaseResponse<Integer> updateUser(@RequestBody User user,HttpServletRequest request){
        //首先判断用户是否为空
        if (user==null){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        //判断权限
        //获取当前用户
        User loginUser = userService.getLoginUser(request);
        //,返回更新后用户
        //判断前端发送数据是否为空？？？
        if (user.equals(loginUser)){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        Integer result = userService.updateUser(user,loginUser);
        return ResultUtils.success(result);

    }

    /**
     * 遍历用户推荐页
     * @param
     * @param
     * @return
     */
    @GetMapping("/recommend")
    public BaseResponse<Page<User>> recommendUsers(long pageSize, long pageNum, HttpServletRequest request){
        //获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        //每个用户推荐页也不相同
        String redisKey = String.format("yupao:user:recommend:userId:%s", loginUser.getId());
        //判断是否存在缓存，存在的话直接读取缓存，不存在的话需要加载
        ValueOperations<String,Object> valueOperations = redisTemplate.opsForValue();
        Page<User> userPage= (Page<User>) valueOperations.get(redisKey );
        if (userPage!=null){
             return ResultUtils.success(userPage);
        }
        QueryWrapper<User> queryWrapper=new QueryWrapper<>();
        userPage = userService.page(new Page<>(pageNum,pageSize),queryWrapper);
        //存入缓存
        try {
            //设置过期时间
            valueOperations.set(redisKey ,userPage,30000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.info("redis set key error",e);
        }
        return ResultUtils.success(userPage);
    }
    }



