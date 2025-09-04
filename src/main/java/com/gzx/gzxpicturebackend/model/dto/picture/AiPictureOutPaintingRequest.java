package com.gzx.gzxpicturebackend.model.dto.picture;

import com.gzx.gzxpicturebackend.api.aliyunai.model.AiOutPaintingRequest;
import lombok.Data;

import java.io.Serializable;

/**
 * 创建扩图任务请求
 */
@Data
public class AiPictureOutPaintingRequest implements Serializable {

    /**
     * 图片 id
     */
    private Long pictureId;

    /**
     * 扩图参数
     */
    private AiOutPaintingRequest.Parameters parameters;

    private static final long serialVersionUID = 1L;
}