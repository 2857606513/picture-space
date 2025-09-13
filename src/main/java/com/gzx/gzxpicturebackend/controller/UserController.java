package com.gzx.gzxpicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gzx.gzxpicturebackend.annotation.AuthCheck;
import com.gzx.gzxpicturebackend.common.BaseResponse;
import com.gzx.gzxpicturebackend.common.DeleteRequest;
import com.gzx.gzxpicturebackend.common.ResultUtils;
import com.gzx.gzxpicturebackend.constant.UserConstant;
import com.gzx.gzxpicturebackend.exception.ErrorCode;
import com.gzx.gzxpicturebackend.exception.ThrowUtils;
import com.gzx.gzxpicturebackend.model.dto.entity.User;
import com.gzx.gzxpicturebackend.model.dto.user.UserAddRequest;
import com.gzx.gzxpicturebackend.model.dto.user.UserQueryRequest;
import com.gzx.gzxpicturebackend.model.dto.user.UserRegisterRequest;
import com.gzx.gzxpicturebackend.model.dto.user.UserRegisterWithInviteRequest;
import com.gzx.gzxpicturebackend.model.dto.user.UserUpdateRequest;
import com.gzx.gzxpicturebackend.model.dto.user.VipExchangeRequest;
import com.gzx.gzxpicturebackend.model.dto.vo.LoginUserVO;
import com.gzx.gzxpicturebackend.model.dto.vo.UserVO;
import com.gzx.gzxpicturebackend.service.AdminService;
import com.gzx.gzxpicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;
    @Resource
    private AdminService AdminService;

    @PostMapping("/register")
    @AuthCheck(role = UserConstant.DEFAULT_ROLE)
    public BaseResponse<Long> userregister(@RequestBody UserRegisterRequest userRegisterRequest){
       ThrowUtils.throwIf(userRegisterRequest==null , ErrorCode.PARAMS_ERROR, "参数为空");
       String userAccount = userRegisterRequest.getUserAccount();
       String userPassword = userRegisterRequest.getUserPassword();
       String checkPassword = userRegisterRequest.getCheckPassword();
        return ResultUtils.success(userService.userResiger(userAccount, userPassword, checkPassword));
    }
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserRegisterRequest userRegisterRequest, HttpServletRequest request){
        ThrowUtils.throwIf(userRegisterRequest==null , ErrorCode.PARAMS_ERROR, "参数为空");
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        return ResultUtils.success(userService.userLogin(userAccount, userPassword, request));
    }
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userService.userLogout(request));
    }
    @PostMapping("/add")
    @AuthCheck(role = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(AdminService.addUser(userAddRequest).getId());
    }


    @GetMapping("/get")
    @AuthCheck(role = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(userService.getById(id) == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(userService.getById(id));
    }


    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        UserVO userVo = userService.getUserVO(user);
        return ResultUtils.success(userVo);
    }

    @PostMapping("/delete")
    @AuthCheck(role = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 更新用户
     */
    @PostMapping("/update")
    @AuthCheck(role = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        ThrowUtils.throwIf(userUpdateRequest == null || userUpdateRequest.getId() == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }


    @PostMapping("/list/page/vo")
    @AuthCheck(role = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(AdminService.listUserVObyPage(userQueryRequest));
    }
    
    /**
     * VIP兑换
     */
    @PostMapping("/vip/exchange")
    public BaseResponse<Boolean> exchangeVip(@RequestBody VipExchangeRequest vipExchangeRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(vipExchangeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        boolean result = userService.exchangeVip(vipExchangeRequest, loginUser);
        return ResultUtils.success(result);
    }
    
    /**
     * 用户注册（带邀请码）
     */
    @PostMapping("/register/invite")
    @AuthCheck(role = UserConstant.DEFAULT_ROLE)
    public BaseResponse<Boolean> userRegisterWithInvite(@RequestBody UserRegisterWithInviteRequest userRegisterWithInviteRequest) {
        ThrowUtils.throwIf(userRegisterWithInviteRequest == null, ErrorCode.PARAMS_ERROR, "参数为空");
        String userAccount = userRegisterWithInviteRequest.getUserAccount();
        String userPassword = userRegisterWithInviteRequest.getUserPassword();
        String checkPassword = userRegisterWithInviteRequest.getCheckPassword();
        String inviteCode = userRegisterWithInviteRequest.getInviteCode();
        
        boolean result = userService.registerWithInviteCode(userAccount, userPassword, checkPassword, inviteCode);
        return ResultUtils.success(result);
    }
    
    /**
     * 生成分享码
     */
    @PostMapping("/share/generate")
    public BaseResponse<String> generateShareCode(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String shareCode = userService.generateShareCode(loginUser);
        return ResultUtils.success(shareCode);
    }
    
    /**
     * 获取邀请的用户列表
     */
    @GetMapping("/invite/list")
    public BaseResponse<List<UserVO>> getInvitedUsers(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<User> invitedUsers = userService.getInvitedUsers(loginUser.getId());
        List<UserVO> userVOList = userService.getUserVOList(invitedUsers);
        return ResultUtils.success(userVOList);
    }
}
