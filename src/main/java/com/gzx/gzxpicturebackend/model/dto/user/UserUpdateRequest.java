package com.gzx.gzxpicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新用户请求
 */
@Data
public class UserUpdateRequest implements Serializable {


    private Long id;


    private String userName;


    private String userAvatar;


    private String userProfile;


    private String userRole;

    private static final long serialVersionUID = 1L;
}