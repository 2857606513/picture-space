package com.gzx.gzxpicturebackend.service;

import com.gzx.gzxpicturebackend.model.dto.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gzx.gzxpicturebackend.model.dto.entity.User;
import com.gzx.gzxpicturebackend.model.dto.space.SpaceAddRequest;
import com.gzx.gzxpicturebackend.model.dto.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author guozhongxing
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-09-03 10:22:03
*/
public interface SpaceService extends IService<Space> {
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);
    void validSpace(Space space, boolean add);
    void fillSpaceBySpaceLevel(Space space);

    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    void checkSpaceAuth(User loginUser, Space space);
}
