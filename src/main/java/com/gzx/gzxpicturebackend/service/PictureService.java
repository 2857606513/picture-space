package com.gzx.gzxpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gzx.gzxpicturebackend.model.dto.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gzx.gzxpicturebackend.model.dto.entity.User;
import com.gzx.gzxpicturebackend.model.dto.picture.*;
import com.gzx.gzxpicturebackend.model.dto.vo.PictureVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author guozhongxing
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-09-01 11:16:23
*/
public interface PictureService extends IService<Picture> {
    PictureVO uploadPicture(Object inputSource,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);
    void validPicture(Picture picture);

    void deletePicture(Long pictureId, User loginUser);
    void pictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);
    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);
    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);
    void clearPictureFile(Picture oldPicture);
    List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser);
    void fillReviewParams(Picture picture, User loginUser/*TODO:加入空间参数让私人空间是本人的话自动通过审核*/);
}
