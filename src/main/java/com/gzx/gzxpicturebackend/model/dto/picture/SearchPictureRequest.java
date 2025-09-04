package com.gzx.gzxpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 以图搜图请求
 */
@Data
public class SearchPictureRequest implements Serializable {

    /**
     * 图片 id
     *
     */
    private Long pictureId;
    private Long spaceId;
    private String pictureColor;
    private static final long serialVersionUID = 1L;
}
