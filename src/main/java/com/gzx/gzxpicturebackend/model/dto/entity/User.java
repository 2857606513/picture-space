package com.gzx.gzxpicturebackend.model.dto.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;


@TableName(value ="user")
@Data
public class User {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;


    private String userAccount;


    private String userPassword;


    private String userName;


    private String userAvatar;


    private String userProfile;


    private String userRole;


    private Date editTime;


    private Date createTime;


    private Date updateTime;


    @TableLogic
    private Integer isDelete;


    private Date vipExpireTime;


    private String vipCode;


    private Long vipNumber;


    private String shareCode;


    private Long iviteUser;
}