package com.gzx.gzxpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadRequest  implements Serializable {
    private long id;
    public static final long serialVersionUID = 1L;
}
