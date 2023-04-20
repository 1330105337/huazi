package com.kuang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kuang.common.BaseResponse;
import com.kuang.common.ErrorCode;
import com.kuang.common.ResultUtils;
import com.kuang.contant.UserContant;
import com.kuang.exception.BusinessException;
import com.kuang.model.domain.User;
import com.kuang.model.vo.UserSendMessage;
import com.kuang.service.UserService;
import com.kuang.mapper.UserMapper;
import com.kuang.utils.AlgorithmUtils;
import com.kuang.utils.ValidateCodeUtils;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.kuang.contant.UserContant.USER_LOGIN_STATE;

/**
 * 用户方法实现类
* @author 华子
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2022-11-28 16:55:56
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService{
    @Resource
    private UserMapper userMapper;
    /**
     * 盐值，混淆密码
     */
    private static final String SALT="yupi";

    //把yml配置的邮箱号赋值到from
    @Value("${spring.mail.username}")
    private String from;
    //发送邮件需要的对象
    @Resource
    private JavaMailSender javaMailSender;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;


    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword,String planetCode,String code,String userEmail) {


        //同时判断三个参数是否为空
        if (StringUtils.isAnyBlank(userAccount,userPassword,checkPassword,planetCode,code,userEmail)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"参数为空");
        }
        //检验长度
        if(userAccount.length()<4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户账号过短");
        }
        if (planetCode.length()>5){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"星球账号过大");
        }
        if (userPassword.length()<8 || checkPassword.length()<8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户密码过短");
        }
        //账户不能包含特殊字符
//        String validPattern= "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*()——+|{}【】‘；：”“’。，、？]";
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"含特殊字符");
        }
        //密码和校验密码相同
        if (!userPassword.equals(checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"两次密码输入不同");
        }

        //从ression获取验证码
        String redisKey = String.format("my:user:sendMessage:%s", userEmail);
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        log.info(redisKey);
        UserSendMessage sendMessage = (UserSendMessage) valueOperations.get(redisKey);
        //如果没有值，失败
        if (!Optional.ofNullable(sendMessage).isPresent()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "获取验证码失败!");
        }
        //比对验证码
        String sendMessageCode = sendMessage.getCode();
        log.info(sendMessageCode);
        if (!code.equals(sendMessageCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码不匹配!");
        }

        //账户不能重复使用，检验数据库中的数据是否与新创建的用户是否相同
        QueryWrapper<User> queryWrapper=new QueryWrapper<>();
        //获取用户
        queryWrapper.eq("userAccount",userAccount);
        //计算用户数量
        long count = userMapper.selectCount(queryWrapper);
        log.info(count+"");
        if (count>0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"存在重复用户");
        }
        //星球编号不能重复使用
        queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("planetCode",planetCode);
         count = userMapper.selectCount(queryWrapper);
        if (count>0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"编号重复");
        }
        //加密密码，MD5算法加密
        String newPassword= DigestUtils.md5DigestAsHex((SALT+userPassword).getBytes());
        //向数据库中插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(newPassword);
        user.setPlanetCode(planetCode);
        user.setEmail(userEmail);
        boolean saveResult= this.save(user);
        if (!saveResult){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"不存在用户");
        }
        return user.getId();
    }

    @Override
    //用户登录
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //同时判断三个参数是否为空
        log.info(userAccount+" "+  userPassword);
        if (StringUtils.isAnyBlank(userAccount,userPassword)){
            throw new BusinessException(ErrorCode.NULL_ERROR,"数据为空");
        }
        //检验长度
        if(userAccount.length()<4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"长度过小");
        }
        if (userPassword.length()<8 ){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"长度过小");
        }
        //账户不能包含特殊字符
//        String validPattern= "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*()——+|{}【】‘；：”“’。，、？]";
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"含特殊字符");
        }
        //加密密码
        String newPassword= DigestUtils.md5DigestAsHex((SALT+userPassword).getBytes());
        //查询用户是否存在
        QueryWrapper<User> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        queryWrapper.eq("userPassword",newPassword);
        User user = userMapper.selectOne(queryWrapper);
        //用户不存在
        if (user==null){
            log.info("user login faild,userAccount not match userPassword");
            throw new BusinessException(ErrorCode.NULL_ERROR,"不存在用户");
        }
        //用户脱敏
          User safetyUser=getSafetyUser(user);
        //记录用户登录态
        request.getSession().setAttribute(USER_LOGIN_STATE,user);

        return safetyUser;
    }

    /**
     * 用户脱敏
     * @param originUser
     * @return
     */
    @Override
    public User getSafetyUser(User originUser){
        if (originUser==null){
            throw new BusinessException(ErrorCode.NULL_ERROR,"不存在用户");
        }
        User safetyUser= new User();
        safetyUser.setId(originUser.getId());
        safetyUser.setUserAccount(originUser.getUserAccount());
        safetyUser.setUsername(originUser.getUsername());
        safetyUser.setAvatarUrl(originUser.getAvatarUrl());
        safetyUser.setGender(originUser.getGender());
        safetyUser.setPhone(originUser.getPhone());
        safetyUser.setPlanetCode(originUser.getPlanetCode());
        safetyUser.setEmail(originUser.getEmail());
        safetyUser.setUserRole(originUser.getUserRole());
        safetyUser.setUserStatus(originUser.getUserStatus());
        safetyUser.setCreateTime(originUser.getCreateTime());
        safetyUser.setTags(originUser.getTags());
        return safetyUser;
    }

    @Override
    public int userLogout(HttpServletRequest request) {
        //将登录态从seeion中移除
       request.getSession().removeAttribute(USER_LOGIN_STATE);
       return 1;
    }

    /**
     * 根据标签搜索用户
     *
     * @param TagNameList 用户所拥有的标签
     * @return
     */
    @Override
    public List<User> searchUsersByTags(List<String>  TagNameList){
        //首先判断是否为空
        if (CollectionUtils.isEmpty(TagNameList)){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
////        SQL查询
////        模糊查询（like,like）,首先先遍历所有用户，然后根据标签查询用户
//        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
//        for (String tagList : TagNameList) {
//          queryWrapper= queryWrapper.like("tags", tagList);
//        }
//        //与数据库中数据进行对比
//         List<User> userList=userMapper.selectList(queryWrapper);
//        //用户脱敏
//        return userList.stream().map(this::getSafetyUser).collect(Collectors.toList());

//        //内存查询
//        //先在内存中寻找标签，寻找所有用户
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        List<User> userList=userMapper.selectList(queryWrapper);
        Gson gson=new Gson();
        //将标签进行反序列化，可以进行在内存中加载，可以进行查询
//        return userList.stream().filter(user -> {
//            String tagStr=user.getTags();
//            if (StringUtils.isBlank(tagStr)){
//                return false;
//            }
//            //将字符串反序列化成json
//            Set<String> tempTagNameSet = gson.fromJson(tagStr, new TypeToken<Set<String>>() {}.getType());
//            for (String tagName : tempTagNameSet) {
//                if (!tempTagNameSet.contains(tagName)){
//                    return false;
//                }
//            }
//            return true;
//        }).map(this::getSafetyUser).collect(Collectors.toList());
        //利用循环进行查询，没有的话返回false，有的话返回true


        return userList.stream().filter(user -> {
            String tagsStr = user.getTags();
            Set<String> tempTagNameSet = gson.fromJson(tagsStr, new TypeToken<Set<String>>() {
            }.getType());
            tempTagNameSet = Optional.ofNullable(tempTagNameSet).orElse(new HashSet<>());
            for (String tagName : TagNameList) {
                if (!tempTagNameSet.contains(tagName)) {
                    return false;
                }
            }
            return true;
        }).map(this::getSafetyUser).collect(Collectors.toList());
    }

    @Override
    public int updateUser( User user, User loginUser) {
        Long userId = user.getId();
        if (userId<=0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //判断是否为管理员,如果是管理员允许更新用户
        if (!isAdmin(loginUser) && userId !=loginUser.getId()){
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
            User selectById = userMapper.selectById(userId);
            if (selectById==null){
                throw new BusinessException(ErrorCode.NULL_ERROR);
            }
            //更新之前的用户
            return userMapper.updateById(user);
        }




    //获取用户登录信息
    @Override
    public User getLoginUser(HttpServletRequest request) {
        //判断是否为空
        if (request==null){
            return null;
        }
        //获取用户登录信息
        Object attribute = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (attribute==null){
            throw new BusinessException(ErrorCode.NO_AUTH,"未登录");
        }
        //返回user对象
        return (User) attribute;
    }

    @Override
    /**
     * 是否为管理员
     * @param request
     * @return
     */
      public boolean isAdmin(HttpServletRequest request){
        //仅管理员可以查询
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user=(User) userObj;
        return user !=null || user.getUserRole() ==UserContant.ADMIN_ROLE;
    }

    public boolean isAdmin(User loginUser){
        //仅管理员可以查询
        return loginUser !=null || loginUser.getUserRole() == UserContant.ADMIN_ROLE;
    }

    @Override
    public List<User> matchUsers(long num, User loginUser) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        //选择要查询的列
        queryWrapper.select("id","tags");
        queryWrapper.isNotNull("tags");
        //遍历所有用户
        List<User> userList = this.list(queryWrapper);
        //获取当前用户的tags，并且将字符串转换为列表形式
        String tags = loginUser.getTags();
        Gson gson = new Gson();
        List<String> tagList = gson.fromJson(tags, new TypeToken<List<String>>() {
        }.getType());
        //用户下表相似度
        List<Pair<User,Long>> list = new ArrayList<>();
        //获得所有用户与登陆用户的相似度
        for (int i=0 ; i < userList.size();i++) {
            //将所有用户的标签搜索出，然后将字符串转换为列表
            User user = userList.get(i);
            String userTags = user.getTags();
            //如果userTags或者遍历到当前登录用户，返回
            if (StringUtils.isBlank(userTags) || user.getId()==loginUser.getId()){
                continue;
            }
            //将gson格式的字符串转化为java对象列表
            List<String> userTagList = gson.fromJson(userTags, new TypeToken<List<String>>() {
            }.getType());
            //与当前登录用户进行比较，返回分数
            long distance = AlgorithmUtils.minDistance(tagList, userTagList);
           list.add(new Pair<>(user,distance));
        }
        //将编辑距离由小到大排序
        List<Pair<User, Long>> topUserPairList = list.stream()
                .sorted((a, b) -> (int) (a.getValue() - b.getValue()))
                .limit(num)
                .collect(Collectors.toList());
//         获取id列表
        List<Long> userIdlist =topUserPairList.stream().map(pair -> pair.getKey().getId()).collect(Collectors.toList());
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        //查询id是否包含在useridlist
        userQueryWrapper.in("id",userIdlist);
        //用户脱敏，转换为key是id的map
        Map<Long, List<User>> userIdUserListMap = this.list(userQueryWrapper)
                .stream()
                .map(user -> getSafetyUser(user))
                .collect(Collectors.groupingBy(User::getId));
        //创建一个新的列表，
        List<User> finalUserList = new ArrayList<>();
        for (Long userId : userIdlist) {
            //将map中的user对象放到list中，之后将数据进行返回
            finalUserList.add(userIdUserListMap.get(userId).get(0));
        }
        return finalUserList;
        }

    /**
     * 发送验证码
     * @param toEmail
     * @return
     */
    @Override
    public BaseResponse<Boolean> sendMessage(UserSendMessage toEmail) {
        String email = toEmail.getUserEmail();
        if (StringUtils.isEmpty(email)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "email为空");
        }
        String subject = "伙伴匹配系统";
        String code = "";
        //StringUtils.isNotEmpty字符串非空判断
        if (StringUtils.isNotEmpty(email)) {
            //发送一个四位数的验证码,把验证码变成String类型
            code = ValidateCodeUtils.generateValidateCode(6).toString();
            String text = "【伙伴匹配系统】您好，您的验证码为：" + code + "，请在5分钟内使用";
            log.info("验证码为：" + code);
            //发送短信
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(email);
            message.setSubject(subject);
            message.setText(text);
            //发送邮件
            javaMailSender.send(message);
            UserSendMessage userSendMessage = new UserSendMessage();
            userSendMessage.setUserEmail(email);
            userSendMessage.setCode(code);
            // 作为唯一标识
            String redisKey = String.format("my:user:sendMessage:%s", email);
            ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
            // 写缓存
            try {
                valueOperations.set(redisKey, userSendMessage, 300000, TimeUnit.MILLISECONDS);
                UserSendMessage sendMessage = (UserSendMessage) valueOperations.get(redisKey);
                log.info(sendMessage.toString());
                return ResultUtils.success(true);
            } catch (Exception e) {
                log.error("redis set key error", e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "缓存失败!");
            }
        }
        return ResultUtils.success(true);
    }
}




