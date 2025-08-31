package com.gzx.gzxpicturebackend.service;

import cn.hutool.http.server.HttpServerRequest;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gzx.gzxpicturebackend.model.dto.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gzx.gzxpicturebackend.model.dto.user.UserQueryRequest;
import com.gzx.gzxpicturebackend.model.dto.vo.LoginUserVO;
import com.gzx.gzxpicturebackend.model.dto.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author guozhongxing
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-08-31 18:47:34
*/
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
}
