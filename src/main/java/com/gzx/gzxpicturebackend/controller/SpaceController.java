package com.gzx.gzxpicturebackend.controller;

import com.gzx.gzxpicturebackend.annotation.AuthCheck;
import com.gzx.gzxpicturebackend.common.BaseResponse;
import com.gzx.gzxpicturebackend.common.ResultUtils;
import com.gzx.gzxpicturebackend.constant.UserConstant;
import com.gzx.gzxpicturebackend.exception.BusinessException;
import com.gzx.gzxpicturebackend.exception.ErrorCode;
import com.gzx.gzxpicturebackend.exception.ThrowUtils;
import com.gzx.gzxpicturebackend.manager.auth.SpaceUserAuthManager;
import com.gzx.gzxpicturebackend.model.dto.entity.Space;
import com.gzx.gzxpicturebackend.model.dto.entity.User;
import com.gzx.gzxpicturebackend.model.dto.enums.SpaceLevelEnum;
import com.gzx.gzxpicturebackend.model.dto.space.SpaceLevel;
import com.gzx.gzxpicturebackend.model.dto.space.SpaceUpdateRequest;
import com.gzx.gzxpicturebackend.model.dto.vo.SpaceVO;
import com.gzx.gzxpicturebackend.service.SpaceService;
import com.gzx.gzxpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j//todo:添加sa-token
@RestController
@RequestMapping("/space")
public class SpaceController {

    @Resource
    private SpaceService spaceService;
    @Resource
    private UserService userService;
    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @PostMapping("/update")
    @AuthCheck(role = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest,
                                             HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUpdateRequest == null || spaceUpdateRequest.getId() <= 0,ErrorCode.PARAMS_ERROR);
       spaceService.updateSpace(spaceUpdateRequest, request);
        return ResultUtils.success(true);
    }
    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values())
                .map(spaceLevelEnum -> new SpaceLevel(
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()
                ))
                .collect(Collectors.toList());
        return ResultUtils.success(spaceLevelList);
    }
    @GetMapping("/get/vo")
    public BaseResponse<SpaceVO> getSpaceVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        SpaceVO spaceVO = spaceService.getSpaceVO(space, request);
        User loginUser = userService.getLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        spaceVO.setPermissionList(permissionList);
        // 获取封装类
        return ResultUtils.success(spaceVO);
    }
}
