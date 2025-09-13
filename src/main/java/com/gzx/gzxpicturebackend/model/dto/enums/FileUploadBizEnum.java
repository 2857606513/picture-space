package com.gzx.gzxpicturebackend.model.dto.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

@Getter
public enum FileUploadBizEnum {
   AVATAR("头像","/uploads/avatars"),
    PICTURE("图片", "/uploads/pictures"),
    THUMBNAIL("缩略图", "/uploads/thumbnails"),
    PNG("png", "/uploads/PNG"),
    JPG("jpg", "/uploads/JPG"),
    GIF("gif", "/uploads/GIF"),
    JPEG("jpeg", "/uploads/JPEG");
    private final String text;

    private final String value;


    FileUploadBizEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }
    public  static FileUploadBizEnum getEnumByUploadPath(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        for (FileUploadBizEnum fileUploadBizEnum : FileUploadBizEnum.values()) {
            if (fileUploadBizEnum.value.equals(value)) {
                return fileUploadBizEnum;
            }
        }
        return null;
    }

}
