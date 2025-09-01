package com.gzx.gzxpicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 会员兑换请求
 */
@Data
public class VipExchangeRequest implements Serializable {

    private static final long serialVersionUID = 8735650154179439661L;


    private String vipCode;
}
