package com.gzx.gzxpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gzx.gzxpicturebackend.exception.ErrorCode;
import com.gzx.gzxpicturebackend.exception.ThrowUtils;
import com.gzx.gzxpicturebackend.model.dto.entity.Space;
import com.gzx.gzxpicturebackend.model.dto.entity.SpaceUser;
import com.gzx.gzxpicturebackend.model.dto.entity.User;
import com.gzx.gzxpicturebackend.model.dto.enums.SpaceRoleEnum;
import com.gzx.gzxpicturebackend.model.dto.enums.SpaceTypeEnum;
import com.gzx.gzxpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.gzx.gzxpicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.gzx.gzxpicturebackend.model.dto.vo.SpaceUserVO;
import com.gzx.gzxpicturebackend.model.dto.vo.SpaceVO;
import com.gzx.gzxpicturebackend.model.dto.vo.UserVO;
import com.gzx.gzxpicturebackend.service.SpaceService;
import com.gzx.gzxpicturebackend.service.SpaceUserService;
import com.gzx.gzxpicturebackend.mapper.SpaceUserMapper;
import com.gzx.gzxpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
* @author guozhongxing
* @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
* @createDate 2025-09-09 19:01:03
*/
@Service
@Slf4j
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
    implements SpaceUserService{

    @Resource
    @Lazy
    private SpaceService spaceService;
    @Resource
    private UserService userService;

    @Override
    public Long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest) {
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR, "参数为空");
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserAddRequest, spaceUser);
        validSpaceUser(spaceUser, true);
        //todo：不能保证一个用户创建一个空间建议复用spaceservice中的添加空间
        boolean result = this.save(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "添加空间失败");

        return spaceUser.getId();
    }
    @Override
    public void validSpaceUser(SpaceUser spaceUser, boolean add) {
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.PARAMS_ERROR);
        // 创建时，空间 id 和用户 id 必填
        Long spaceId = spaceUser.getSpaceId();
        Long userId = spaceUser.getUserId();
        if (add) {
            ThrowUtils.throwIf(ObjectUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
            User user = userService.getById(userId);
            ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        // 校验空间角色
        String spaceRole = spaceUser.getSpaceRole();
        SpaceRoleEnum spaceRoleEnum = SpaceRoleEnum.getEnumByValue(spaceRole);
        ThrowUtils.throwIf(spaceRole != null && spaceRoleEnum == null,ErrorCode.PARAMS_ERROR, "空间角色不存在");
    }

    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        if (spaceUserQueryRequest == null){
            return queryWrapper;
        }
        Long id = spaceUserQueryRequest.getId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        String spaceRole = spaceUserQueryRequest.getSpaceRole();
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceRole), "spaceRole", spaceRole);

        return queryWrapper;
    }
    @Override
    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        // 对象转封装类
        SpaceUserVO spaceUserVO = SpaceUserVO.objToVo(spaceUser);
        // 关联查询用户信息
        Long userId = spaceUser.getUserId();
        if (userId != null && userId > 0) {
            UserVO userVO = userService.getUserVO(userService.getById(userId));
            spaceUserVO.setUser(userVO);
        }
        // 关联查询空间信息
        Long spaceId = spaceUser.getSpaceId();
        if (spaceId != null && spaceId > 0) {
            SpaceVO spaceVO = spaceService.getSpaceVO(spaceService.getById(spaceId), request);
            spaceUserVO.setSpace(spaceVO);
        }
        return spaceUserVO;
    }
//    @Override
//    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) {
//        // 判断输入列表是否为空
//        if (CollUtil.isEmpty(spaceUserList)) {
//            return Collections.emptyList();
//        }
//
//        // 对象列表 => 封装对象列表
//        List<SpaceUserVO> spaceUserVOList = spaceUserList.stream().map(SpaceUserVO::objToVo).collect(Collectors.toList());
//        // 1. 收集需要关联查询的用户 ID 和空间 ID
//        Set<Long> userIdSet = spaceUserList.stream().map(SpaceUser::getUserId).collect(Collectors.toSet());
//        Set<Long> spaceIdSet = spaceUserList.stream().map(SpaceUser::getSpaceId).collect(Collectors.toSet());
//        // 2. 批量查询用户和空间
//        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
//                .collect(Collectors.groupingBy(User::getId));
//        Map<Long, List<Space>> spaceIdSpaceListMap = spaceService.listByIds(spaceIdSet).stream()
//                .collect(Collectors.groupingBy(Space::getId));
//        // 3. 填充 SpaceUserVO 的用户和空间信息
//        spaceUserVOList.forEach(spaceUserVO -> {
//            Long userId = spaceUserVO.getUserId();
//            Long spaceId = spaceUserVO.getSpaceId();
//            // 填充用户信息
//            User user = null;
//            if (userIdUserListMap.containsKey(userId)) {
//                user = userIdUserListMap.get(userId).get(0);
//            }
//            spaceUserVO.setUser(userService.getUserVO(user));
//            // 填充空间信息
//            Space space = null;
//            if (spaceIdSpaceListMap.containsKey(spaceId)) {
//                space = spaceIdSpaceListMap.get(spaceId).get(0);
//            }
//            spaceUserVO.setSpace(SpaceVO.objToVo(space));
//        });
//        return spaceUserVOList;
//    }



    @Override
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) {
        // 判断输入列表是否为空
        if (CollUtil.isEmpty(spaceUserList)) {
            return Collections.emptyList();
        }

        // 1. 收集需要关联查询的用户 ID 和空间 ID，并构建 VO 列表
        Set<Long> userIdSet = new HashSet<>();
        Set<Long> spaceIdSet = new HashSet<>();
        List<SpaceUserVO> spaceUserVOList = new ArrayList<>();

        for (SpaceUser spaceUser : spaceUserList) {
            if (spaceUser == null) continue;

            Long userId = spaceUser.getUserId();
            Long spaceId = spaceUser.getSpaceId();

            if (userId != null) userIdSet.add(userId);
            if (spaceId != null) spaceIdSet.add(spaceId);

            spaceUserVOList.add(SpaceUserVO.objToVo(spaceUser));
        }

        // 2. 批量查询用户和空间信息
        Map<Long, User> userIdUserMap = new HashMap<>();
        try {
            if (!userIdSet.isEmpty()) {
                userIdUserMap = userService.listByIds(userIdSet).stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(User::getId, Function.identity(), (u1, u2) -> u1));
            }
        } catch (Exception e) {
            // 记录日志或抛出自定义异常
            log.error("Failed to fetch users by ids: {}", userIdSet, e);
        }

        Map<Long, Space> spaceIdSpaceMap = new HashMap<>();
        try {
            if (!spaceIdSet.isEmpty()) {
                spaceIdSpaceMap = spaceService.listByIds(spaceIdSet).stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(Space::getId, Function.identity(), (s1, s2) -> s1));
            }
        } catch (Exception e) {
            // 记录日志或抛出自定义异常
            log.error("Failed to fetch spaces by ids: {}", spaceIdSet, e);
        }

        // 3. 填充 SpaceUserVO 的用户和空间信息
        for (SpaceUserVO spaceUserVO : spaceUserVOList) {
            Long userId = spaceUserVO.getUserId();
            Long spaceId = spaceUserVO.getSpaceId();

            User user = userIdUserMap.get(userId);
            spaceUserVO.setUser(userService.getUserVO(user));

            Space space = spaceIdSpaceMap.get(spaceId);
            spaceUserVO.setSpace(SpaceVO.objToVo(space));
        }

        return spaceUserVOList;
    }

//todo：修改团队成员移除或者编辑，修改前判断检验是否是空间成员如果修改和之前一样就不用更新



}




