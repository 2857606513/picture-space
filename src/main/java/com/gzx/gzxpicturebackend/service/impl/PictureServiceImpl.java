package com.gzx.gzxpicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gzx.gzxpicturebackend.exception.ErrorCode;
import com.gzx.gzxpicturebackend.exception.ThrowUtils;
import com.gzx.gzxpicturebackend.model.dto.entity.Picture;
import com.gzx.gzxpicturebackend.model.dto.entity.User;
import com.gzx.gzxpicturebackend.model.dto.enums.PictureReviewStatusEnum;
import com.gzx.gzxpicturebackend.model.dto.picture.PictureQueryRequest;
import com.gzx.gzxpicturebackend.model.dto.picture.PictureReviewRequest;
import com.gzx.gzxpicturebackend.model.dto.picture.PictureUploadRequest;
import com.gzx.gzxpicturebackend.model.dto.vo.PictureVO;
import com.gzx.gzxpicturebackend.service.PictureService;
import com.gzx.gzxpicturebackend.mapper.PictureMapper;
import com.gzx.gzxpicturebackend.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
* @author guozhongxing
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2025-09-01 11:16:23
*/
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

    @Resource
    private UserService userService;
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
       //TODO：添加图片上传功能
        return null;
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
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
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
    public void deletePicture(Long pictureId, User loginUser) {
//todo：开启事务删除使用额度
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
        ThrowUtils.throwIf(id==null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(pictureReviewStatusEnum.equals(PictureReviewStatusEnum.REVIEWING) , ErrorCode.OPERATION_ERROR, "请勿重复操作");
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
    public void fillReviewParams(Picture picture, User loginUser) {
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


}





