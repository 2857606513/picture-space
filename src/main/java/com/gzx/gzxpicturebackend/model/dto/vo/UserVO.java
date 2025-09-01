package com.gzx.gzxpicturebackend.model.dto.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户视图（脱敏）
 */
@Data
public class UserVO implements Serializable {


    private Long id;


    private String userAccount;


    private String userName;


    private String userAvatar;


    private String userProfile;


    private String userRole;


    private Date vipExpireTime;


    private String vipCode;


    private Long vipNumber;


    private Date createTime;

    private static final long serialVersionUID = 1L;
}