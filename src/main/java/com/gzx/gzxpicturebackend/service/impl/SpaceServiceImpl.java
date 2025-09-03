package com.gzx.gzxpicturebackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gzx.gzxpicturebackend.exception.BusinessException;
import com.gzx.gzxpicturebackend.exception.ErrorCode;
import com.gzx.gzxpicturebackend.exception.ThrowUtils;
import com.gzx.gzxpicturebackend.model.dto.entity.Space;
import com.gzx.gzxpicturebackend.model.dto.entity.User;
import com.gzx.gzxpicturebackend.model.dto.enums.SpaceLevelEnum;
import com.gzx.gzxpicturebackend.model.dto.enums.SpaceTypeEnum;
import com.gzx.gzxpicturebackend.model.dto.space.SpaceAddRequest;
import com.gzx.gzxpicturebackend.service.SpaceService;
import com.gzx.gzxpicturebackend.mapper.SpaceMapper;
import com.gzx.gzxpicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
* @author guozhongxing
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2025-09-03 10:22:03
*/
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceService{
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private UserService userService;
    Map<Long ,Object> lockMap = new ConcurrentHashMap<>();
    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {//TODO：多传入一个用户ID，管理员为用户创建空间在controller上控制好权限
        ThrowUtils.throwIf(spaceAddRequest == null, ErrorCode.PARAMS_ERROR, "参数为空");
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);
        if (StrUtil.isBlank(space.getSpaceName())) {
            space.setSpaceName("默认空间");
        }
        if (space.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        if (space.getSpaceType() == null) {
            space.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
        // 填充容量和大小
        this.fillSpaceBySpaceLevel(space);
        // 2. 校验参数
        this.validSpace(space, true);
        // 3. 校验权限，非管理员只能创建普通级别的空间
        Long userId = loginUser.getId();
        space.setUserId(userId);
        ThrowUtils.throwIf(SpaceLevelEnum.COMMON.getValue() != space.getSpaceLevel() && !userService.isAdmin(loginUser),
                ErrorCode.NO_AUTH_ERROR, "无权限创建指定级别的空间" );
        // 4. 控制同一用户只能创建一个私有空间、以及一个团队空间
        Object lock = lockMap.computeIfAbsent(userId, k -> new Object());
        synchronized (lock) {
            try {
            Long newSpaceId = transactionTemplate.execute(status -> {
                // 判断是否已有空间
                LambdaQueryWrapper<Space> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(Space::getUserId, userId);
                wrapper.eq(Space::getSpaceType, space.getSpaceType());
                // 如果已有空间，就不能再创建
                ThrowUtils.throwIf(this.exists(wrapper), ErrorCode.OPERATION_ERROR, "每个用户每类空间只能创建一个");
                // 创建
                boolean result = this.save(space);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "保存空间到数据库失败");
                return space.getId();
            });
            return Optional.ofNullable(newSpaceId).orElse(-1L);
            }finally {
                lockMap.remove(userId);
            }
        }
    }

    @Override
    public void validSpace(Space space, boolean add) {

            ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
            // 从对象中取值
            String spaceName = space.getSpaceName();
            Integer spaceLevel = space.getSpaceLevel();
            Integer spaceType = space.getSpaceType();

            SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);

            SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);
            // 创建时校验
            if (add) {
                ThrowUtils.throwIf(StrUtil.isBlank(spaceName), ErrorCode.PARAMS_ERROR, "空间名称不能为空");
                ThrowUtils.throwIf(spaceLevel == null, ErrorCode.PARAMS_ERROR, "空间级别不能为空");
                ThrowUtils.throwIf(spaceType == null, ErrorCode.PARAMS_ERROR, "空间类别不能为空");
            }
            ThrowUtils.throwIf(StrUtil.isNotBlank(spaceName) && spaceName.length() > 30, ErrorCode.PARAMS_ERROR, "空间名称过长");
            ThrowUtils.throwIf(spaceLevel != null && spaceLevelEnum == null,ErrorCode.PARAMS_ERROR, "空间级别不存在");
            ThrowUtils.throwIf(spaceType != null && spaceTypeEnum == null,ErrorCode.PARAMS_ERROR, "空间类别不存在");
        }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        Integer spaceLevel = space.getSpaceLevel();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        if (spaceLevelEnum != null) {
            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
        }
    }

//TODO：实现删除空间并清理空间内的图片
    //TODO：实现更新空间并用事务更新时减少原理图片的额度增加新增图片的额度，异步删除原有图片
}





