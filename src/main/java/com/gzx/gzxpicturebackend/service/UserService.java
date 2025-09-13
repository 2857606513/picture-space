package com.gzx.gzxpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gzx.gzxpicturebackend.model.dto.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gzx.gzxpicturebackend.model.dto.user.UserQueryRequest;
import com.gzx.gzxpicturebackend.model.dto.user.VipExchangeRequest;
import com.gzx.gzxpicturebackend.model.dto.vo.LoginUserVO;
import com.gzx.gzxpicturebackend.model.dto.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


public interface UserService extends IService<User> {
    String getEncryptPassword(String userPassword);
    long userResiger(String userAccount, String userPassword, String checkPassword);
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);
    LoginUserVO getLoginUserVO(User user);
    User getLoginUser(HttpServletRequest request);
    boolean userLogout(HttpServletRequest request);
    UserVO getUserVO(User user);
    List<UserVO> getUserVOList(List<User> userList);
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);
    boolean isAdmin(User user);
    
    // VIP相关功能
    boolean exchangeVip(VipExchangeRequest vipExchangeRequest, User loginUser);
    boolean isVip(User user);
    void checkVipStatus(User user);
    
    // 用户邀请功能
    String generateShareCode(User user);
    boolean registerWithInviteCode(String userAccount, String userPassword, String checkPassword, String inviteCode);
    List<User> getInvitedUsers(Long userId);
}
