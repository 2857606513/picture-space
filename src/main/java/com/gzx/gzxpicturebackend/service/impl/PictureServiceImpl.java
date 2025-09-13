package com.gzx.gzxpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gzx.gzxpicturebackend.api.aliyunai.AliYunAiApi;
import com.gzx.gzxpicturebackend.api.aliyunai.model.AiOutPaintingRequest;
import com.gzx.gzxpicturebackend.api.aliyunai.model.AiOutPaintingResponse;
import com.gzx.gzxpicturebackend.exception.BusinessException;
import com.gzx.gzxpicturebackend.exception.ErrorCode;
import com.gzx.gzxpicturebackend.exception.ThrowUtils;
import com.gzx.gzxpicturebackend.manager.CosManager;
import com.gzx.gzxpicturebackend.manager.upload.FilePictureUpload;
import com.gzx.gzxpicturebackend.manager.upload.PictureUploadTemplate;
import com.gzx.gzxpicturebackend.manager.upload.UrlPictureUpload;
import com.gzx.gzxpicturebackend.model.dto.entity.Picture;
import com.gzx.gzxpicturebackend.model.dto.entity.Space;
import com.gzx.gzxpicturebackend.model.dto.entity.User;
import com.gzx.gzxpicturebackend.model.dto.enums.PictureReviewStatusEnum;
import com.gzx.gzxpicturebackend.model.dto.enums.SpaceTypeEnum;
import com.gzx.gzxpicturebackend.model.dto.file.UploadPictureResult;
import com.gzx.gzxpicturebackend.model.dto.picture.*;
import com.gzx.gzxpicturebackend.model.dto.vo.PictureVO;
import com.gzx.gzxpicturebackend.service.PictureService;
import com.gzx.gzxpicturebackend.mapper.PictureMapper;
import com.gzx.gzxpicturebackend.service.SpaceService;
import com.gzx.gzxpicturebackend.service.UserService;
import com.gzx.gzxpicturebackend.utils.ColorSimilarUtils;
import com.gzx.gzxpicturebackend.utils.ColorTransformUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
* @author guozhongxing
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2025-09-01 11:16:23
*/
@Slf4j
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

    @Resource
    private UserService userService;
    @Resource
    private CosManager cosManager;
    @Resource
    private SpaceService spaceService;
    @Resource
    private FilePictureUpload filePictureUpload;
    @Resource
    private ThreadPoolExecutor customExecutor;
    @Resource
    private AliYunAiApi aliYunAiApi;
    @Resource
    private UrlPictureUpload urlPictureUpload;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 参数验证
        validateUploadParams(loginUser, pictureUploadRequest);
        
        Long spaceId = pictureUploadRequest.getSpaceId();
        Long pictureId = pictureUploadRequest != null ? pictureUploadRequest.getId() : null;
        
        // 验证空间权限和额度
        validateSpacePermission(spaceId, loginUser);
        
        // 处理更新场景
        Picture oldPicture = handleUpdateScenario(pictureId, spaceId);
        
        // 上传图片
        UploadPictureResult uploadPictureResult = uploadPictureFile(inputSource, spaceId, loginUser);
        
        // 构建图片实体
        Picture picture = buildPictureEntity(uploadPictureResult, pictureUploadRequest, spaceId, loginUser, pictureId);
        
        // 保存到数据库并更新空间额度
        savePictureAndUpdateSpace(picture, spaceId, oldPicture);
        
        // 清理旧图片文件（如果是更新操作）
        if (oldPicture != null) {
            clearPictureFile(oldPicture);
        }
        
        log.info("图片上传成功，ID: {}, 用户ID: {}, 空间ID: {}", picture.getId(), loginUser.getId(), spaceId);
        return PictureVO.objToVo(picture);
    }
    
    /**
     * 验证上传参数
     */
    private void validateUploadParams(User loginUser, PictureUploadRequest pictureUploadRequest) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR, "用户未登录");
        ThrowUtils.throwIf(pictureUploadRequest == null, ErrorCode.PARAMS_ERROR, "上传请求参数不能为空");
    }
    
    /**
     * 验证空间权限和额度
     */
    private void validateSpacePermission(Long spaceId, User loginUser) {
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            
            // 校验额度 - 此时space已确定不为null
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, 
                    "空间条数不足，当前: " + space.getTotalCount() + "/" + space.getMaxCount());
            }
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, 
                    "空间大小不足，当前: " + space.getTotalSize() + "/" + space.getMaxSize());
            }
        }
    }
    
    /**
     * 处理更新场景
     */
    private Picture handleUpdateScenario(Long pictureId, Long spaceId) {
        if (pictureId == null) {
            return null;
        }
        
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        
        // 校验空间是否一致 - 此时oldPicture已确定不为null
        if (spaceId == null) {
            // 没传 spaceId，则复用原有图片的 spaceId（兼容公共图库）
            if (oldPicture.getSpaceId() != null) {
                spaceId = oldPicture.getSpaceId();
            }
        } else {
            // 传了 spaceId，必须和原图片的空间 id 一致
            if (ObjUtil.notEqual(spaceId, oldPicture.getSpaceId())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间 id 不一致");
            }
        }
        
        return oldPicture;
    }
    
    /**
     * 上传图片文件
     */
    private UploadPictureResult uploadPictureFile(Object inputSource, Long spaceId, User loginUser) {
        try {
            // 构建上传路径前缀
            String uploadPathPrefix = buildUploadPathPrefix(spaceId, loginUser);
            
            // 选择上传模板
            PictureUploadTemplate pictureUploadTemplate = selectUploadTemplate(inputSource);
            
            log.info("开始上传图片，用户ID: {}, 空间ID: {}, 路径前缀: {}", loginUser.getId(), spaceId, uploadPathPrefix);
            return pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        } catch (Exception e) {
            log.error("图片上传失败，用户ID: {}, 空间ID: {}", loginUser.getId(), spaceId, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 构建上传路径前缀
     */
    private String buildUploadPathPrefix(Long spaceId, User loginUser) {
        if (spaceId == null) {
            // 公共图库
            return String.format("public/%s", loginUser.getId());
        } else {
            // 空间
            return String.format("space/%s", spaceId);
        }
    }
    
    /**
     * 选择上传模板
     */
    private PictureUploadTemplate selectUploadTemplate(Object inputSource) {
        if (inputSource instanceof String) {
            return urlPictureUpload;
        }
        return filePictureUpload;
    }
    
    /**
     * 构建图片实体
     */
    private Picture buildPictureEntity(UploadPictureResult uploadPictureResult, PictureUploadRequest pictureUploadRequest, 
                                     Long spaceId, User loginUser, Long pictureId) {
        Picture picture = new Picture();
        picture.setSpaceId(spaceId);
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        
        // 设置图片名称
        String pictureName = uploadPictureResult.getPictureName();
        if (StrUtil.isNotBlank(pictureUploadRequest.getPictureName())) {
            pictureName = pictureUploadRequest.getPictureName();
        }
        picture.setName(pictureName);
        
        // 设置图片属性
        picture.setPictureSize(uploadPictureResult.getPictureSize());
        picture.setPictureWidth(uploadPictureResult.getPictureWidth());
        picture.setPictureHeight(uploadPictureResult.getPictureHeight());
        picture.setPictureScale(uploadPictureResult.getPictureScale());
        picture.setPictureFormat(uploadPictureResult.getPictureFormat());
        
        // 转换为标准颜色
        picture.setPictureColor(ColorTransformUtils.getStandardColor(uploadPictureResult.getPictureColor()));
        picture.setUserId(loginUser.getId());

        // 补充审核参数
        this.fillReviewParams(picture, loginUser,spaceService.getById(spaceId));
        
        // 如果是更新，设置ID和编辑时间
        if (pictureId != null) {
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        
        return picture;
    }
    
    /**
     * 保存图片并更新空间额度
     */
    private void savePictureAndUpdateSpace(Picture picture, Long spaceId, Picture oldPicture) {
        transactionTemplate.execute(status -> {
            try {
                // 保存或更新图片
                boolean result = this.saveOrUpdate(picture);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片保存失败");
                
                // 更新空间额度
                if (spaceId != null) {
                    updateSpaceQuota(spaceId, picture, oldPicture);
                }
                
                return picture;
            } catch (Exception e) {
                log.error("保存图片失败，图片ID: {}, 空间ID: {}", picture.getId(), spaceId, e);
                status.setRollbackOnly();
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存图片失败: " + e.getMessage());
            }
        });
    }
    
    /**
     * 更新空间额度
     */
    private void updateSpaceQuota(Long spaceId, Picture picture, Picture oldPicture) {
        try {
            if (oldPicture == null) {
                // 新增图片，增加额度
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, spaceId)
                        .setSql("totalSize = totalSize + " + picture.getPictureSize())
                        .setSql("totalCount = totalCount + 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "空间额度更新失败");
                log.info("空间额度更新成功，空间ID: {}, 新增大小: {}, 新增数量: 1", spaceId, picture.getPictureSize());
            } else {
                // 更新图片，计算差值
                long sizeDiff = picture.getPictureSize() - oldPicture.getPictureSize();
                if (sizeDiff != 0) {
                    boolean update = spaceService.lambdaUpdate()
                            .eq(Space::getId, spaceId)
                            .setSql("totalSize = totalSize + " + sizeDiff)
                            .update();
                    ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "空间额度更新失败");
                    log.info("空间额度更新成功，空间ID: {}, 大小变化: {}", spaceId, sizeDiff);
                }
            }
        } catch (Exception e) {
            log.error("更新空间额度失败，空间ID: {}", spaceId, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新空间额度失败: " + e.getMessage());
        }
    }

    @Override
    public void pictureUpdate(PictureUpdateRequest pictureUpdateRequest,HttpServletRequest request) {
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        this.validPicture(picture);
        Picture oldPicture = this.getById(pictureUpdateRequest.getId());
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        this.fillReviewParams(picture, userService.getLoginUser(request),spaceService.getById(picture.getSpaceId()));
        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

    }


    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null){
            return queryWrapper;
        }
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long userId = pictureQueryRequest.getUserId();
        Long pictureSize = pictureQueryRequest.getPictureSize();
        Integer pictureWidth = pictureQueryRequest.getPictureWidth();
        Integer pictureHeight = pictureQueryRequest.getPictureHeight();
        Double pictureScale = pictureQueryRequest.getPictureScale();
        String pictureFormat = pictureQueryRequest.getPictureFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long spaceId = pictureQueryRequest.getSpaceId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        if (StrUtil.isNotBlank(searchText)){
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText));
        }
            // 基本条件查询
            if (ObjUtil.isNotEmpty(id)) {
                queryWrapper.eq("id", id);
            }
            if (ObjUtil.isNotEmpty(startEditTime)){
                queryWrapper.ge("editTime", startEditTime);
            }
            if (ObjUtil.isNotEmpty(endEditTime)){
                queryWrapper.lt("editTime", endEditTime);
            }
            if (ObjUtil.isNotEmpty(spaceId)){
                queryWrapper.eq("spaceId", spaceId);
            }
            if (nullSpaceId){
                queryWrapper.isNull("spaceId");
            }
            if (ObjUtil.isNotEmpty(userId)) {
                queryWrapper.eq("userId", userId);
            }
            if (StrUtil.isNotBlank(name)) {
                queryWrapper.like("name", name);
            }
            if (StrUtil.isNotBlank(introduction)) {
                queryWrapper.like("introduction", introduction);
            }
            if (StrUtil.isNotBlank(pictureFormat)) {
                queryWrapper.like("pictureFormat", pictureFormat);
            }
            if (StrUtil.isNotBlank(category)) {
                queryWrapper.eq("category", category);
            }
            if (ObjUtil.isNotEmpty(pictureWidth)) {
                queryWrapper.eq("pictureWidth", pictureWidth);
            }
            if (ObjUtil.isNotEmpty(pictureHeight)) {
                queryWrapper.eq("pictureHeight", pictureHeight);
            }
            if (ObjUtil.isNotEmpty(pictureSize)) {
                queryWrapper.eq("pictureSize", pictureSize);
            }
            if (ObjUtil.isNotEmpty(pictureScale)) {
                queryWrapper.eq("pictureScale", pictureScale);
            }
            if (ObjUtil.isNotEmpty(reviewStatus)){
                queryWrapper.eq("reviewStatus", reviewStatus);
            }
            if (StrUtil.isNotBlank(reviewMessage)) {
                queryWrapper.like("reviewMessage", reviewMessage);
            }
            if (ObjUtil.isNotEmpty(reviewerId)) {
                queryWrapper.eq("reviewerId", reviewerId);
            }
            // 标签查询 - 使用更安全的方式处理JSON数组匹配
            if (CollUtil.isNotEmpty(tags)) {
                for (String tag : tags) {
                    // 使用 apply 方法安全地处理标签查询
                    queryWrapper.like("tags","\""+ tag+"\"");
                }
            }

            // 排序处理 - 添加空值检查避免 NullPointerException
            if (StrUtil.isNotBlank(sortField)) {
                boolean isAsc = sortOrder.equals("ascend");
                queryWrapper.orderBy(true, isAsc, sortField);
            }

            return queryWrapper;
        }

    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        PictureVO pictureVO = PictureVO.objToVo(picture);
        Long userId = picture.getUserId();

        if (userId != null&& userId > 0) {
            pictureVO.setUser(userService.getUserVO(userService.getById(userId)));

        }
        return pictureVO;
    }

    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
        // 1. 关联查询用户信息
        // 1,2,3,4
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        // 1 => user1, 2 => user2
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(ObjUtil.isNull( picture.getId()), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank( picture.getUrl())) {
            ThrowUtils.throwIf(picture.getUrl().length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(picture.getIntroduction())) {
            ThrowUtils.throwIf(picture.getIntroduction().length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    @Override
    public void deletePicture(Long pictureId, User loginUser) {ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 判断是否存在
        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 校验权限，已经改为使用注解鉴权
//        checkPictureAuth(loginUser, oldPicture);
        // 开启事务
        transactionTemplate.execute(status -> {
            // 操作数据库
            boolean result = this.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            // 更新空间的使用额度，释放额度
            UpdateWrapper<Space> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", oldPicture.getSpaceId());
            updateWrapper.setSql("totalSize = totalSize - " + oldPicture.getPictureSize());
            updateWrapper.setSql("totalCount = totalCount - 1");

            boolean update = spaceService.update(updateWrapper);
            ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            return true;
        });
        // 异步清理文件
        this.clearPictureFile(oldPicture);


    }



    //TODO:添加第三方平台的自动审核
    //TODO：举报机制给举报用户vip兑换码
    //TODO：审核通知审核完成后通过消息中心发给用户
    @Override
    public void pictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(pictureReviewRequest), ErrorCode.PARAMS_ERROR);

        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum pictureReviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);

        ThrowUtils.throwIf(pictureReviewStatusEnum == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(id == null, ErrorCode.PARAMS_ERROR);
        // 此时pictureReviewStatusEnum已确定不为null
        if (pictureReviewStatusEnum.equals(PictureReviewStatusEnum.REVIEWING)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "请勿重复操作");
        }
        Picture pictureId = this.getById(id);
        ThrowUtils.throwIf(pictureId == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(pictureId.getReviewStatus().equals(reviewStatus), ErrorCode.PARAMS_ERROR, "请勿重复操作");

        Picture updatePicture = new Picture();
        updatePicture.setId(id);
        updatePicture.setReviewStatus(reviewStatus);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        picture.setEditTime(new Date());
        this.validPicture(picture);
        Picture oldPicture = this.getById(pictureEditRequest.getId());
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        this.fillReviewParams(picture, loginUser,spaceService.getById(picture.getId()));

        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多 30 条");
        ThrowUtils.throwIf(count < 1, ErrorCode.PARAMS_ERROR, "至少 1 条");
        ThrowUtils.throwIf(StrUtil.isBlank(searchText), ErrorCode.PARAMS_ERROR, "搜索关键词不能为空");
        // 名称前缀默认等于搜索关键词
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }
        // 抓取内容
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        log.info("开始抓取图片：{}", fetchUrl);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
            log.info("获取页面成功");
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
        // 解析内容
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isEmpty(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        Elements imgElementList = div.select("img.mimg");
        // 遍历元素，依次处理上传图片
        int uploadCount = 0;
        for (int i = 0; i < imgElementList.size() && uploadCount < count; i++) {
            Element imgElement = imgElementList.get(i);
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过：{}", fileUrl);
                continue;
            }
            // 处理图片的地址，防止转义或者和对象存储冲突的问题
            // codefather.cn?yupi=dog，应该只保留 codefather.cn
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            // 上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setFileUrl(fileUrl);
            pictureUploadRequest.setPictureName(namePrefix + (uploadCount + 1));
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功，id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
            }
        }
        return uploadCount;
        //TODO:1.设置批量抓取的偏移量2.记录图片的url不给用户看便于回档3.设置批量抓取的标签实现和设置名称的逻辑一样
    }

    @Override
    public void fillReviewParams(Picture picture, User loginUser, Space spaceType) {
        //TODO:添加第三方平台自动审核,判断传入的空间类型是否为私人空间如果是则直接通过
        if (spaceType.getSpaceType().equals(SpaceTypeEnum.PRIVATE)){
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("私人空间自动过审");
            picture.setReviewTime(new Date());
        }
        if(userService.isAdmin(loginUser)){
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员审核通过");
            picture.setReviewTime(new Date());
        }
        else {
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("等待管理员审核");
            picture.setReviewTime(new Date());
        }
    }
    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        if (oldPicture == null) {
            log.warn("清理图片文件失败：图片对象为空");
            return;
        }
        
        try {
            String pictureUrl = oldPicture.getUrl();
            String thumbnailUrl = oldPicture.getThumbnailUrl();
            
            log.info("开始清理图片文件，图片ID: {}, URL: {}", oldPicture.getId(), pictureUrl);
            
            // 检查图片是否还被其他记录使用
            if (isPictureStillInUse(pictureUrl)) {
                log.info("图片仍被其他记录使用，跳过清理，URL: {}", pictureUrl);
                return;
            }
            
            // 删除主图片
            deletePictureFile(pictureUrl, "主图片");
            
            // 删除缩略图
            if (StrUtil.isNotBlank(thumbnailUrl)) {
                deletePictureFile(thumbnailUrl, "缩略图");
            }
            
            log.info("图片文件清理完成，图片ID: {}", oldPicture.getId());
            
        } catch (Exception e) {
            log.error("清理图片文件失败，图片ID: {}, URL: {}", oldPicture.getId(), oldPicture.getUrl(), e);
            // 异步方法中的异常不应该影响主流程，只记录日志
        }
    }
    
    /**
     * 检查图片是否仍被其他记录使用
     */
    private boolean isPictureStillInUse(String pictureUrl) {
        if (StrUtil.isBlank(pictureUrl)) {
            return false;
        }
        
        try {
            long count = this.lambdaQuery()
                    .eq(Picture::getUrl, pictureUrl)
                    .count();
            return count > 0;
        } catch (Exception e) {
            log.error("检查图片使用情况失败，URL: {}", pictureUrl, e);
            // 如果检查失败，为了安全起见，不删除文件
            return true;
        }
    }
    
    /**
     * 删除图片文件
     */
    private void deletePictureFile(String fileUrl, String fileType) {
        if (StrUtil.isBlank(fileUrl)) {
            log.warn("{}URL为空，跳过删除", fileType);
            return;
        }
        
        try {
            // 提取文件key（去除域名部分）
            String fileKey = extractFileKey(fileUrl);
            if (StrUtil.isBlank(fileKey)) {
                log.warn("无法提取{}的文件key，URL: {}", fileType, fileUrl);
                return;
            }
            
            cosManager.deleteObject(fileKey);
            log.info("{}删除成功，Key: {}", fileType, fileKey);
            
        } catch (Exception e) {
            log.error("删除{}失败，URL: {}", fileType, fileUrl, e);
            // 删除失败不影响主流程，只记录日志
        }
    }
    
    /**
     * 从完整URL中提取文件key
     */
    private String extractFileKey(String fileUrl) {
        if (StrUtil.isBlank(fileUrl)) {
            return null;
        }
        
        try {
            // 如果URL包含域名，提取路径部分
            if (fileUrl.contains("://")) {
                // 找到第三个斜杠后的部分
                int thirdSlashIndex = fileUrl.indexOf('/', fileUrl.indexOf("://") + 3);
                if (thirdSlashIndex > 0) {
                    return fileUrl.substring(thirdSlashIndex + 1);
                }
            }
            
            // 如果已经是相对路径，直接返回
            return fileUrl;
            
        } catch (Exception e) {
            log.error("提取文件key失败，URL: {}", fileUrl, e);
            return null;
        }
    }

    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String pictureColor, User loginUser) {
        //TODO:把颜色搜索也应用到公共图库和管理员2.多模态搜索3.定义颜色的阈值排除不相似的图片
        ThrowUtils.throwIf(spaceId == null || StrUtil.isBlank(pictureColor), ErrorCode.PARAMS_ERROR, "参数错误");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR, "用户未登录");
        // 2. 校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR, "用户未登录");
        // 此时space和loginUser已确定不为null
        if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间访问权限");
        }
        // 3. 查询该空间下的所有图片（必须要有主色调）
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("spaceId", spaceId);
        queryWrapper.isNotNull("pictureColor");
        List<Picture> pictureList = this.list(queryWrapper);
        // 如果没有图片，直接返回空列表
        if (CollUtil.isEmpty(pictureList)) {
            return new ArrayList<>();
        }
        // 将颜色字符串转换为主色调
        Color targetColor = Color.decode(pictureColor);
        // 4. 计算相似度并排序
        List<Picture> sortedPictureList = pictureList.stream()
                .sorted(Comparator.comparingDouble(picture -> {
                    String hexColor = picture.getPictureColor();
                    // 没有主色调的图片会默认排序到最后
                    if (StrUtil.isBlank(hexColor)) {
                        return Double.MAX_VALUE;
                    }
                    Color currentPictureColor = Color.decode(hexColor);
                    // 计算相似度
                    // 越大越相似
                    return -ColorSimilarUtils.calculateSimilarity(targetColor, currentPictureColor);
                }))
                .limit(12) // 取前 12 个
                .collect(Collectors.toList());
        // 5. 返回结果
        return sortedPictureList.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
    }

   @Transactional(rollbackFor = Exception.class)
   @Override
   public void batchEditPictureMetadata(PictureBatchEditRequest request, Long spaceId, Long loginUserId) {
        // 参数校验
       validateBatchEditRequest(request, spaceId, loginUserId);
       // 查询空间下的图片
        List<Picture> pictureList = this.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .in(Picture::getId, request.getPictureIds())
                .list();

         ThrowUtils.throwIf(pictureList.isEmpty(),ErrorCode.NOT_FOUND_ERROR, "指定的图片不存在或不属于该空间");


        int batchSize = 100;
        List<CompletableFuture<Void>> futures = new ArrayList<>();
       log.info("开始批量编辑图片，总图片数: {}, 批次大小: {}", pictureList.size(), batchSize);
        for (int i = 0; i < pictureList.size(); i += batchSize) {
            List<Picture> batch = pictureList.subList(i, Math.min(i + batchSize, pictureList.size()));

            int batchIndex = i / batchSize + 1;
            int totalBatches = (int) Math.ceil((double) pictureList.size() / batchSize);
            log.info("处理第 {}/{} 批次，批次大小: {}", batchIndex, totalBatches, batch.size());

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    log.debug("开始处理批次 {}，包含 {} 张图片", batchIndex, batch.size());

                    batch.forEach(picture -> {
                        // 编辑分类和标签
                        if (request.getCategory() != null) {
                            picture.setCategory(request.getCategory());
                            log.debug("设置图片 {} 的分类为: {}", picture.getId(), request.getCategory());
                        }
                        if (request.getTags() != null) {
                            picture.setTags(String.join(",", request.getTags()));
                        }
                    });
                    log.debug("开始批量更新第 {} 批次数据", batchIndex);
                    boolean result = this.updateBatchById(batch);
                    if (!result) {
                        log.error("批量更新第 {} 批次图片失败", batchIndex);
                        throw new BusinessException(ErrorCode.OPERATION_ERROR, "批量更新图片失败");
                    }
                    log.info("第 {}/{} 批次处理完成，更新 {} 张图片", batchIndex, totalBatches, batch.size());
                } catch (Exception e) {
                    log.error("处理批次 {} 失败", batchIndex, e);
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "处理批次失败");
                }
                }, customExecutor);
            futures.add(future);
        }

       // 等待所有任务完成
       CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        String nameRule = request.getNameRule();
       fillPictureWithNameRule(pictureList, nameRule);

       this.updateBatchById(pictureList);
    }
    private void validateBatchEditRequest(PictureBatchEditRequest request, Long spaceId, Long loginUserId) {
        ThrowUtils.throwIf(request == null || spaceId == null || loginUserId == null, ErrorCode.PARAMS_ERROR, "参数错误");
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        // 此时space和loginUserId已确定不为null
        if (!space.getUserId().equals(loginUserId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间访问权限");
        }
    }
    private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        if (StrUtil.isBlank(nameRule) || CollUtil.isEmpty(pictureList)) {
            return;
        }
        long count = 1;
        try {
            for (Picture picture : pictureList) {
                String pictureName = nameRule.replaceAll("\\{序号}", String.valueOf(count++));
                picture.setName(pictureName);
            }
        } catch (Exception e) {
            log.error("名称解析错误", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "名称解析错误");
        }
    }
    @Override
    public AiOutPaintingResponse aiOutPaintingResponse(AiPictureOutPaintingRequest aiPictureOutPaintingRequest, User loginUser) {
        // 获取图片信息
        //TODO:加入注解鉴权和优化代码
        Long pictureId = aiPictureOutPaintingRequest.getPictureId();
        Picture picture = Optional.ofNullable(this.getById(pictureId))
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在"));
        // 校验权限，已经改为使用注解鉴权
//        checkPictureAuth(loginUser, picture);
        // 创建扩图任务
        AiOutPaintingRequest aiOutPaintingRequest = new AiOutPaintingRequest();
        AiOutPaintingRequest.Input input = new AiOutPaintingRequest.Input();
        input.setImageUrl(picture.getUrl());
        aiOutPaintingRequest.setInput(input);
        aiOutPaintingRequest.setParameters(aiPictureOutPaintingRequest.getParameters());
        // 创建任务
        return aliYunAiApi.aiOutPaintingTask(aiOutPaintingRequest);
    }
   }






