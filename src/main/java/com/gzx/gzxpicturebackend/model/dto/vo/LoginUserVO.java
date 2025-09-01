package com.gzx.gzxpicturebackend.model.dto.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 已登录用户视图（脱敏）
 */
@Data
public class LoginUserVO implements Serializable {


    private Long id;


    private String userAccount;


    private String userName;


    private String userAvatar;


    private String userProfile;


    private String userRole;


    private Date editTime;


    private Date createTime;


    private Date updateTime;

    private static final long serialVersionUID = 1L;
}