package com.gzx.gzxpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gzx.gzxpicturebackend.model.dto.entity.User;
import com.gzx.gzxpicturebackend.model.dto.user.UserAddRequest;
import com.gzx.gzxpicturebackend.model.dto.user.UserQueryRequest;
import com.gzx.gzxpicturebackend.model.dto.vo.UserVO;

import java.util.List;

public interface AdminService extends IService<User> {
    User addUser(UserAddRequest userAddRequest);
    String getEncryptPassword(String userPassword);
    Page<UserVO> listUserVObyPage(UserQueryRequest userQueryRequest);
    List<UserVO> getUserVOList(List<User> userList);
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);
    UserVO getUserVO(User user);

}
