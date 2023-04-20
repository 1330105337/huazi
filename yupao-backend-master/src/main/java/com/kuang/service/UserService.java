package com.kuang.service;

import com.kuang.common.BaseResponse;
import com.kuang.model.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kuang.model.vo.UserSendMessage;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 86187
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2022-11-28 16:55:57
*/
public interface UserService extends IService<User> {

    /**
     *  用户注册
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @param checkPassword 验证码
     * @return
     */
    long userRegister(String userAccount,String userPassword,String checkPassword,String planetCode,String code,String userEmail);

    /**
     *用户登录
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @return 返回脱敏后的用户信息
     */

    User userLogin(String userAccount, String userPassword,HttpServletRequest request);

    /**
     * 用户脱敏
     * @param originUser
     * @return
     */
    User getSafetyUser(User originUser);

    /**
     * 用户注销
     */
    int userLogout(HttpServletRequest request);

    /**
     * 根据标签搜索用户
     *
     * @param TagList
     * @return
     */
    List<User> searchUsersByTags(List<String> TagList);

    /**
     * 更新用户
     * @param user
     * @return
     */
    int updateUser(User user,User loginUser);

    /**
     * 获取用户登录信息
     * @return
     */
    User getLoginUser(HttpServletRequest request);


    /**
     * 是否为管理员
     * @param request
     * @return
     */
   boolean isAdmin(HttpServletRequest request);
    /**
     * 是否为管理员
     * @param loginUser
     * @return
     */
   boolean isAdmin(User loginUser);

    /**
     * 推荐与登录用户标签相似度高的用户
     *
     * @param num
     * @param loginUser
     * @return
     */
    List<User> matchUsers(long num, User loginUser);

    BaseResponse<Boolean> sendMessage(UserSendMessage userSendMessage);
}
