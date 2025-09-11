package com.gzx.gzxpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gzx.gzxpicturebackend.model.dto.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gzx.gzxpicturebackend.model.dto.entity.User;
import com.gzx.gzxpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.gzx.gzxpicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.gzx.gzxpicturebackend.model.dto.vo.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author guozhongxing
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2025-09-09 19:01:03
*/
public interface SpaceUserService extends IService<SpaceUser> {
Long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    void validSpaceUser(SpaceUser spaceUser, boolean add);
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);
}
