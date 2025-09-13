package com.gzx.gzxpicturebackend.manager.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.gzx.gzxpicturebackend.manager.auth.model.SpaceUserAuthConfig;
import com.gzx.gzxpicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.gzx.gzxpicturebackend.manager.auth.model.SpaceUserRole;
import com.gzx.gzxpicturebackend.model.dto.entity.Space;
import com.gzx.gzxpicturebackend.model.dto.entity.SpaceUser;
import com.gzx.gzxpicturebackend.model.dto.entity.User;
import com.gzx.gzxpicturebackend.model.dto.enums.SpaceRoleEnum;
import com.gzx.gzxpicturebackend.model.dto.enums.SpaceTypeEnum;
import com.gzx.gzxpicturebackend.service.SpaceUserService;
import com.gzx.gzxpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

/**
 * 空间成员权限管理
 */
@Slf4j
@Component
public class SpaceUserAuthManager {

    @Resource
    private UserService userService;

    @Resource
    private SpaceUserService spaceUserService;

    public static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;
    private static final Map<String, List<String>> ROLE_PERMISSION_MAP = new HashMap<>();
    private static final List<String> ADMIN_PERMISSIONS;
    private static final List<String> EDITOR_PERMISSIONS;

    static {
        try {
            String json = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
            SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(json, SpaceUserAuthConfig.class);

            // 构建角色权限映射缓存
            if (SPACE_USER_AUTH_CONFIG.getRoles() != null) {
                for (SpaceUserRole role : SPACE_USER_AUTH_CONFIG.getRoles()) {
                    ROLE_PERMISSION_MAP.put(role.getKey(), Collections.unmodifiableList(role.getPermissions()));
                }
            }
            EDITOR_PERMISSIONS = getPermissionsByRoleCached(SpaceRoleEnum.EDITOR.getValue());
            ADMIN_PERMISSIONS = getPermissionsByRoleCached(SpaceRoleEnum.ADMIN.getValue());

        } catch (Exception e) {
            throw new RuntimeException("Failed to load space user auth config.", e);
        }
    }

    /**
     * 根据角色获取权限列表（带缓存）
     *
     * @param spaceUserRole 角色标识
     * @return 权限列表副本
     */
    private static List<String> getPermissionsByRoleCached(String spaceUserRole) {
        List<String> permissions = ROLE_PERMISSION_MAP.get(spaceUserRole);
        return permissions == null ? new ArrayList<>() : new ArrayList<>(permissions);
    }

    /**
     * 根据角色获取权限列表（对外接口）
     *
     * @param spaceUserRole 角色标识
     * @return 权限列表副本
     */
    public List<String> getPermissionsByRole(String spaceUserRole) {
        if (StrUtil.isBlank(spaceUserRole)) {
            return new ArrayList<>();
        }
        return getPermissionsByRoleCached(spaceUserRole);
    }

    /**
     * 获取权限列表
     *
     * @param space     空间对象
     * @param loginUser 登录用户
     * @return 权限列表
     */
    public List<String> getPermissionList(Space space, User loginUser) {
        if (loginUser == null) {
            return new ArrayList<>();
        }

        // 公共图库
        if (space == null) {
            if (userService.isAdmin(loginUser)) {
                return ADMIN_PERMISSIONS;
            }
            return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
        }

        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(space.getSpaceType());
        if (spaceTypeEnum == null) {
            return new ArrayList<>();
        }

        switch (spaceTypeEnum) {
            case PRIVATE:
                // 私有空间，仅本人或管理员有所有权限
                if (space.getUserId().equals(loginUser.getId()) ) {
                    return EDITOR_PERMISSIONS;
                } else if(userService.isAdmin(loginUser)) {
                    return ADMIN_PERMISSIONS;
                }else {
                    return new ArrayList<>();
                }
            case TEAM:
                try {
                    SpaceUser spaceUser = spaceUserService.lambdaQuery()
                            .eq(SpaceUser::getSpaceId, space.getId())
                            .eq(SpaceUser::getUserId, loginUser.getId())
                            .one();
                    if (spaceUser == null) {
                        return new ArrayList<>();
                    } else {
                        return getPermissionsByRole(spaceUser.getSpaceRole());
                    }
                } catch (Exception e) {
                    // 增加异常处理，防止数据库异常导致服务中断
                    log.error("Failed to query space user info for spaceId={}, userId={}", space.getId(), loginUser.getId(), e);
                    return new ArrayList<>();
                }
            default:
                // 防止遗漏枚举类型处理
                return new ArrayList<>();
        }
    }
}

