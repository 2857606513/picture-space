package com.gzx.gzxpicturebackend.model.dto.file;

import lombok.Data;

/**
 * 上传图片的结果
 */
@Data
public class UploadPictureResult {

    /**
     * 图片地址
     */
    private String url;

    /**
     * 缩略图 url
     */
    private String thumbnailUrl;

    /**
     * 图片名称
     */
    private String pictureName;

    /**
     * 文件体积
     */
    private Long pictureSize;

    /**
     * 图片宽度
     */
    private int pictureWidth;

    /**
     * 图片高度
     */
    private int pictureHeight;

    /**
     * 图片宽高比
     */
    private Double pictureScale;

    /**
     * 图片格式
     */
    private String pictureFormat;

    /**
     * 图片主色调
     */
    private String pictureColor;
}