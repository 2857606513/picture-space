package com.gzx.gzxpicturebackend.model.dto.user;

import lombok.Data;

/**
 * 兑换码
 */
@Data
public class VipCode {


    private String code;


    private boolean hasUsed;
}