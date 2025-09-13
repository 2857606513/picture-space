package com.gzx.gzxpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.server.HttpServerRequest;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gzx.gzxpicturebackend.common.BaseResponse;
import com.gzx.gzxpicturebackend.constant.UserConstant;
import com.gzx.gzxpicturebackend.exception.BusinessException;
import com.gzx.gzxpicturebackend.exception.ErrorCode;
import com.gzx.gzxpicturebackend.exception.ThrowUtils;
import com.gzx.gzxpicturebackend.manager.auth.StpKit;
import com.gzx.gzxpicturebackend.model.dto.entity.User;
import com.gzx.gzxpicturebackend.model.dto.enums.UserRoleEnum;
import com.gzx.gzxpicturebackend.model.dto.enums.SpaceTypeEnum;
import com.gzx.gzxpicturebackend.model.dto.enums.SpaceLevelEnum;
import com.gzx.gzxpicturebackend.model.dto.space.SpaceAddRequest;
import com.gzx.gzxpicturebackend.model.dto.user.UserQueryRequest;
import com.gzx.gzxpicturebackend.model.dto.user.VipExchangeRequest;
import com.gzx.gzxpicturebackend.model.dto.vo.LoginUserVO;
import com.gzx.gzxpicturebackend.model.dto.vo.UserVO;
import com.gzx.gzxpicturebackend.service.SpaceService;
import com.gzx.gzxpicturebackend.service.UserService;
import com.gzx.gzxpicturebackend.mapper.UserMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.gzx.gzxpicturebackend.constant.UserConstant.USER_LOGIN_STATE;

/**
* @author guozhongxing
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-08-31 18:47:34
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Resource
    @Lazy
    private SpaceService spaceService;

    @Override
    public long userResiger(String userAccount, String userPassword, String checkPassword) {
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, userPassword, checkPassword) , ErrorCode.PARAMS_ERROR, "参数为空");
        ThrowUtils.throwIf(userAccount.length() < 4, ErrorCode.PARAMS_ERROR, "用户账号过短");
        ThrowUtils.throwIf(userPassword.length() < 8, ErrorCode.PARAMS_ERROR, "用户密码过短");
        ThrowUtils.throwIf(!userPassword.equals(checkPassword), ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.baseMapper.selectCount(queryWrapper);
        ThrowUtils.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "账号重复");
        String encryptPassword = getEncryptPassword(userPassword);
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName(userAccount);
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean save = this.save(user);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "注册失败");
       
        // 注册成功后创建默认私有空间
        try {
            SpaceAddRequest spaceAddRequest = new SpaceAddRequest();
            spaceAddRequest.setSpaceName("默认空间");
            spaceAddRequest.setSpaceType(SpaceTypeEnum.PRIVATE.getValue()); // 私有空间
            spaceAddRequest.setSpaceLevel(SpaceLevelEnum.COMMON.getValue()); // 普通级别
            spaceService.addSpace(spaceAddRequest, user);
        } catch (Exception e) {
            // 如果创建空间失败，记录日志但不影响注册流程
            // 可以考虑记录到日志文件中
            System.err.println("创建默认空间失败: " + e.getMessage());
        }
       
        return user.getId();
    }
    @Override
    public String getEncryptPassword(String userPassword) {
        final String SALT = "gzx";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

    }
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request){
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, userPassword) , ErrorCode.PARAMS_ERROR, "参数为空");
        ThrowUtils.throwIf(userAccount.length() < 4, ErrorCode.PARAMS_ERROR, "用户账号过短");
        ThrowUtils.throwIf(userPassword.length() < 8, ErrorCode.PARAMS_ERROR, "用户密码过短");
        String encryptPassword = getEncryptPassword(userPassword);
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);

    }
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 判断是否已经登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }
    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 判断是否已经登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());
    }
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public boolean isAdmin(User user) {
        return user!= null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    @Override
    public boolean exchangeVip(VipExchangeRequest vipExchangeRequest, User loginUser) {
        if (vipExchangeRequest == null || loginUser == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        
        String vipCode = vipExchangeRequest.getVipCode();
        if (StrUtil.isBlank(vipCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "VIP码不能为空");
        }
        
        // 检查用户是否已经是VIP
        if (isVip(loginUser)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "您已经是VIP用户");
        }
        
        // 验证VIP码（这里简化处理，实际应该从数据库或配置文件中验证）
        if (!isValidVipCode(vipCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效的VIP码");
        }
        
        // 设置VIP过期时间（30天）
        Date vipExpireTime = new Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000);
        loginUser.setVipExpireTime(vipExpireTime);
        loginUser.setVipCode(vipCode);
        loginUser.setVipNumber(1L);
        
        boolean result = this.updateById(loginUser);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR, "VIP兑换失败");
        
        return true;
    }
    
    @Override
    public boolean isVip(User user) {
        if (user == null || user.getVipExpireTime() == null) {
            return false;
        }
        return user.getVipExpireTime().after(new Date());
    }
    
    @Override
    public void checkVipStatus(User user) {
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (!isVip(user)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "需要VIP权限");
        }
    }
    
    @Override
    public String generateShareCode(User user) {
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        
        // 如果用户已有分享码，直接返回
        if (StrUtil.isNotBlank(user.getShareCode())) {
            return user.getShareCode();
        }
        
        // 生成新的分享码（用户ID + 随机字符串）
        String shareCode = "INVITE_" + user.getId() + "_" + System.currentTimeMillis();
        user.setShareCode(shareCode);
        
        boolean result = this.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR, "生成分享码失败");
        
        return shareCode;
    }
    
    @Override
    public boolean registerWithInviteCode(String userAccount, String userPassword, String checkPassword, String inviteCode) {
        // 基本参数验证
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, userPassword, checkPassword), ErrorCode.PARAMS_ERROR, "参数为空");
        ThrowUtils.throwIf(userAccount.length() < 4, ErrorCode.PARAMS_ERROR, "用户账号过短");
        ThrowUtils.throwIf(userPassword.length() < 8, ErrorCode.PARAMS_ERROR, "用户密码过短");
        ThrowUtils.throwIf(!userPassword.equals(checkPassword), ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        
        // 验证邀请码
        if (StrUtil.isBlank(inviteCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邀请码不能为空");
        }
        
        // 查找邀请人
        QueryWrapper<User> inviteQuery = new QueryWrapper<>();
        inviteQuery.eq("shareCode", inviteCode);
        User inviter = this.baseMapper.selectOne(inviteQuery);
        ThrowUtils.throwIf(inviter == null, ErrorCode.PARAMS_ERROR, "无效的邀请码");
        
        // 检查账号是否已存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.baseMapper.selectCount(queryWrapper);
        ThrowUtils.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "账号重复");
        
        // 创建新用户
        String encryptPassword = getEncryptPassword(userPassword);
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName(userAccount);
        user.setUserRole(UserRoleEnum.USER.getValue());
        user.setIviteUser(inviter.getId()); // 设置邀请人
        
        boolean save = this.save(user);
        ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "注册失败");
        
        // 注册成功后创建默认私有空间
        try {
            SpaceAddRequest spaceAddRequest = new SpaceAddRequest();
            spaceAddRequest.setSpaceName("默认空间");
            spaceAddRequest.setSpaceType(SpaceTypeEnum.PRIVATE.getValue()); // 私有空间
            spaceAddRequest.setSpaceLevel(SpaceLevelEnum.COMMON.getValue()); // 普通级别
            spaceService.addSpace(spaceAddRequest, user);
        } catch (Exception e) {
            // 如果创建空间失败，记录日志但不影响注册流程
            // 可以考虑记录到日志文件中
            System.err.println("创建默认空间失败: " + e.getMessage());
        }
        
        return true;
    }
    
    @Override
    public List<User> getInvitedUsers(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }
        
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("iviteUser", userId);
        return this.baseMapper.selectList(queryWrapper);
    }
    
    /**
     * 验证VIP码是否有效
     * @param vipCode VIP码
     * @return 是否有效
     */
    private boolean isValidVipCode(String vipCode) {
        // 这里简化处理，实际应该从数据库或配置文件中验证
        // 检查VIP码是否已被使用
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("vipCode", vipCode);
        long count = this.baseMapper.selectCount(queryWrapper);
        return count == 0; // 如果没有人使用过这个VIP码，则认为有效
    }
}




