package com.gzx.gzxpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadRequest  implements Serializable {
    private long id;
    private String fileUrl;
    private String pictureName;
    private Long spaceId;
    public static final long serialVersionUID = 1L;

}
