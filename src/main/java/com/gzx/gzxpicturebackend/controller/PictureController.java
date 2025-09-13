package com.gzx.gzxpicturebackend.controller;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.gzx.gzxpicturebackend.annotation.AuthCheck;
import com.gzx.gzxpicturebackend.api.aliyunai.AliYunAiApi;
import com.gzx.gzxpicturebackend.api.aliyunai.model.AiOutPaintingResponse;
import com.gzx.gzxpicturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.gzx.gzxpicturebackend.api.imagesearch.ImageSearchApiFacade;
import com.gzx.gzxpicturebackend.api.imagesearch.model.ImageSearchResult;
import com.gzx.gzxpicturebackend.common.BaseResponse;
import com.gzx.gzxpicturebackend.common.DeleteRequest;
import com.gzx.gzxpicturebackend.common.ResultUtils;
import com.gzx.gzxpicturebackend.constant.UserConstant;
import com.gzx.gzxpicturebackend.exception.BusinessException;
import com.gzx.gzxpicturebackend.exception.ErrorCode;
import com.gzx.gzxpicturebackend.exception.ThrowUtils;
import com.gzx.gzxpicturebackend.manager.auth.SpaceUserAuthManager;
import com.gzx.gzxpicturebackend.manager.auth.StpKit;
import com.gzx.gzxpicturebackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.gzx.gzxpicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.gzx.gzxpicturebackend.model.dto.entity.Picture;
import com.gzx.gzxpicturebackend.model.dto.entity.Space;
import com.gzx.gzxpicturebackend.model.dto.entity.User;
import com.gzx.gzxpicturebackend.model.dto.enums.PictureReviewStatusEnum;
import com.gzx.gzxpicturebackend.model.dto.picture.*;
import com.gzx.gzxpicturebackend.model.dto.vo.PictureTagCategory;
import com.gzx.gzxpicturebackend.model.dto.vo.PictureVO;
import com.gzx.gzxpicturebackend.service.PictureService;
import com.gzx.gzxpicturebackend.service.SpaceService;
import com.gzx.gzxpicturebackend.service.SpaceUserService;
import com.gzx.gzxpicturebackend.service.UserService;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;


@Slf4j
@RestController
@RequestMapping("/picture")
public class PictureController {
    @Resource
    private UserService userService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private PictureService pictureService;
    @Resource
    private AliYunAiApi aliYunAiApi;
    @Resource
    private SpaceService spaceService;
    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;
    @PostMapping("/upload")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPicture(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, userService.getLoginUser(request));
        return ResultUtils.success(pictureVO);
    }
    @PostMapping("/upload/url")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPictureByUrl(
            @RequestBody PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        PictureVO pictureVO = pictureService.uploadPicture( pictureUploadRequest.getFileUrl(), pictureUploadRequest,  userService.getLoginUser(request));
        return ResultUtils.success(pictureVO);
    }
    @PostMapping("/update")
    @AuthCheck(role = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest,
                                            HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0,ErrorCode.PARAMS_ERROR);
        pictureService.pictureUpdate(pictureUpdateRequest, request);
        return ResultUtils.success(true);
    }
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest
            , HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0,ErrorCode.PARAMS_ERROR);
        pictureService.deletePicture(deleteRequest.getId(), userService.getLoginUser(request));
        return ResultUtils.success(true);
    }
    @GetMapping("/get")
    @AuthCheck(role = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(picture);
    }

    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf( pictureService.getById(id) == null, ErrorCode.NOT_FOUND_ERROR);
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        Long spaceId = picture.getSpaceId();
        Space space = null;
        if (spaceId != null) {
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
            space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        // 获取权限列表
        User loginUser = userService.getLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        PictureVO pictureVO = pictureService.getPictureVO(picture, request);
        pictureVO.setPermissionList(permissionList);
        // 获取封装类
        return ResultUtils.success(pictureVO);
    }

    @PostMapping("/list/page")
    @AuthCheck(role = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        Page<Picture> picturePage = pictureService.page(new Page<>(pictureQueryRequest.getCurrent(), pictureQueryRequest.getPageSize()),
                pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage);
    }

    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));
    }


    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditRequest == null || pictureEditRequest.getId() <= 0,ErrorCode.PARAMS_ERROR);
      User loginUser = userService.getLoginUser(request);
      pictureService.editPicture(pictureEditRequest, loginUser);
        return ResultUtils.success(true);
    }

    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory(@RequestBody PictureEditRequest pictureEditRequest) {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }
// TODO: 使用配置中心动态管理图片标签的存储
    @PostMapping("/review")
    @AuthCheck(role = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> PictureReview(@RequestBody PictureReviewRequest pictureReviewRequest,
                                             HttpServletRequest request) {
    ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
    pictureService.pictureReview(pictureReviewRequest, userService.getLoginUser(request) );
    return ResultUtils.success(true);
}

    @PostMapping("/upload/batch")
    @AuthCheck(role = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
                                                      HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        int uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, userService.getLoginUser(request));
        return ResultUtils.success(uploadCount);
    }
    @PostMapping("/search/picture")
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(@RequestBody SearchPictureRequest searchPictureRequest) {
        // 参数校验
    ThrowUtils.throwIf(searchPictureRequest == null, ErrorCode.PARAMS_ERROR);
    // 图片id
    Long pictureId = searchPictureRequest.getPictureId();

    ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
    Picture picture = pictureService.getById(pictureId);

    ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
    // 调用图片搜索接口
    List<ImageSearchResult> resultList = ImageSearchApiFacade.searchImage(picture.getUrl());
    return ResultUtils.success(resultList);
}
    @PostMapping("/search/color")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<PictureVO>> searchPictureByColor(@RequestBody SearchPictureRequest searchPictureByColorRequest, HttpServletRequest request) {
    ThrowUtils.throwIf(searchPictureByColorRequest == null, ErrorCode.PARAMS_ERROR);
    String pictureColor = searchPictureByColorRequest.getPictureColor();
    Long spaceId = searchPictureByColorRequest.getSpaceId();
    User loginUser = userService.getLoginUser(request);
    List<PictureVO> pictureVOList = pictureService.searchPictureByColor(spaceId, pictureColor, loginUser);
    return ResultUtils.success(pictureVOList);
}
    @PostMapping("/edit/batch")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureBatchEditRequest pictureBatchEditRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureBatchEditRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Long spaceId = pictureBatchEditRequest.getSpaceId();
        Long loginUserId = loginUser.getId();
        pictureService.batchEditPictureMetadata(pictureBatchEditRequest,spaceId,loginUserId);
        return ResultUtils.success(true);
    }

    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                                      HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 普通用户默认只能看到审核通过的数据
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        // 查询缓存，缓存中没有，再查询数据库
        // 构建缓存的 key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String cacheKey = String.format("yupicture:listPictureVOByPage:%s", hashKey);
        // 1. 先从本地缓存中查询
        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (cachedValue != null) {
            // 如果缓存命中，返回结果
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachedPage);
        }
        // 2. 本地缓存未命中，查询 Redis 分布式缓存
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        cachedValue = opsForValue.get(cacheKey);
        if (cachedValue != null) {
            // 如果缓存命中，更新本地缓存，返回结果
            LOCAL_CACHE.put(cacheKey, cachedValue);
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachedPage);
        }
        // 3. 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
        // 4. 更新缓存
        // 更新 Redis 缓存
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        // 设置缓存的过期时间，5 - 10 分钟过期，防止缓存雪崩
        int cacheExpireTime = 300 + RandomUtil.randomInt(0, 300);
        //TODO：1.设置热点数据的最大超时时间，2.使用自动热点工具加入热点数据3.使用布隆过滤器防止访问不存在数据
        opsForValue.set(cacheKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);
        // 写入本地缓存
        LOCAL_CACHE.put(cacheKey, cacheValue);
        // 获取封装类
        return ResultUtils.success(pictureVOPage);
    }
    private static final Cache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            .maximumSize(1000L)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .initialCapacity(512)
            .build();
    //TODO:添加接口和service逻辑，手动刷新缓存的接口仅管理员调用
    /**
     * 创建 AI 扩图任务
     */
    @PostMapping("/out_painting/create_task")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
//    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<AiOutPaintingResponse> aiPictureOutPainting(@RequestBody AiPictureOutPaintingRequest aiPictureOutPaintingRequest,
                                                                            HttpServletRequest request) {
        ThrowUtils.throwIf(aiPictureOutPaintingRequest == null || aiPictureOutPaintingRequest.getPictureId() == null,ErrorCode.PARAMS_ERROR,"请选择图片");
        User loginUser = userService.getLoginUser(request);
        AiOutPaintingResponse response = pictureService.aiOutPaintingResponse(aiPictureOutPaintingRequest, loginUser);
        return ResultUtils.success(response);
    }

    /**
     * 查询 AI 扩图任务
     */
    @GetMapping("/out_painting/get_task")
    public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTask(String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(aliYunAiApi.getOutPaintingTask(taskId));
    }
}
