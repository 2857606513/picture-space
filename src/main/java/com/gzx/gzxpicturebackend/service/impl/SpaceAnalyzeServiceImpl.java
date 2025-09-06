package com.gzx.gzxpicturebackend.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gzx.gzxpicturebackend.exception.BusinessException;
import com.gzx.gzxpicturebackend.exception.ErrorCode;
import com.gzx.gzxpicturebackend.exception.ThrowUtils;
import com.gzx.gzxpicturebackend.mapper.SpaceMapper;
import com.gzx.gzxpicturebackend.model.dto.entity.Picture;
import com.gzx.gzxpicturebackend.model.dto.entity.Space;
import com.gzx.gzxpicturebackend.model.dto.entity.User;
import com.gzx.gzxpicturebackend.model.dto.space.analyze.*;
import com.gzx.gzxpicturebackend.model.dto.vo.space.analyze.SpaceCategoryAnalyzeResponse;
import com.gzx.gzxpicturebackend.model.dto.vo.space.analyze.SpaceSizeAnalyzeResponse;
import com.gzx.gzxpicturebackend.model.dto.vo.space.analyze.SpaceTagAnalyzeResponse;
import com.gzx.gzxpicturebackend.model.dto.vo.space.analyze.SpaceUsageAnalyzeResponse;
import com.gzx.gzxpicturebackend.service.PictureService;
import com.gzx.gzxpicturebackend.service.SpaceAnalyzeService;
import com.gzx.gzxpicturebackend.service.SpaceService;
import com.gzx.gzxpicturebackend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
* @author guozhongxing
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2025-09-03 10:22:03
*/
@Service
public class SpaceAnalyzeServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceAnalyzeService {
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private UserService userService;
    @Resource
    private SpaceService spaceService;
    @Resource
    private PictureService pictureService;
    Map<Long ,Object> lockMap = new ConcurrentHashMap<>();
    private void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        // 全空间分析或者公共图库权限校验：仅管理员可访问
        if (queryAll || queryPublic) {
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "无权限访问");
        } else {
            // 分析特定空间，仅本人或管理员可以访问
            Long spaceId = spaceAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            ThrowUtils.throwIf(!space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser),ErrorCode.NO_AUTH_ERROR,"没有空间访问权限");
        }
    }

    private void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper) {
        // 全空间分析
        boolean queryAll = spaceAnalyzeRequest.isQueryAll();
        if (queryAll) {
            return;
        }
        // 公共图库
        boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
        if (queryPublic) {
            queryWrapper.isNull("spaceId");
            return;
        }
        // 分析特定空间
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        if (spaceId != null) {
            queryWrapper.eq("spaceId", spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定查询范围");
    }

    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser) {

        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);

        checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest, loginUser);
        if (spaceUsageAnalyzeRequest.isQueryAll() || spaceUsageAnalyzeRequest.isQueryPublic()) {



            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("pictureSize");

            fillAnalyzeQueryWrapper(spaceUsageAnalyzeRequest, queryWrapper);
            List<Object> pictureObjList = pictureService.getBaseMapper().selectObjs(queryWrapper);
            long usedSize = 0L;
            long usedCount = 0L;
            if (pictureObjList != null && !pictureObjList.isEmpty()) {
                try {
                    usedSize = pictureObjList.stream()
                            .filter(Objects::nonNull)
                            .mapToLong(obj -> ((Number) obj).longValue())
                            .sum();
                    usedCount = pictureObjList.size();
                } catch (ClassCastException e) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片大小字段类型错误");
                }
            }

            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(usedSize);
            spaceUsageAnalyzeResponse.setUsedCount(usedCount);

            spaceUsageAnalyzeResponse.setMaxSize(null);
            spaceUsageAnalyzeResponse.setSizeUsageRatio(null);
            spaceUsageAnalyzeResponse.setMaxCount(null);
            spaceUsageAnalyzeResponse.setCountUsageRatio(null);
            return spaceUsageAnalyzeResponse;
        } else {

            Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);

            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");



            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(space.getTotalSize());
            spaceUsageAnalyzeResponse.setUsedCount(space.getTotalCount());
            spaceUsageAnalyzeResponse.setMaxSize(space.getMaxSize());
            spaceUsageAnalyzeResponse.setMaxCount(space.getMaxCount());

            double sizeUsageRatio = (space.getMaxSize() != null && space.getMaxSize() > 0)
                    ? NumberUtil.round(space.getTotalSize() * 100.0 /space.getMaxSize(), 2).doubleValue()
                    : 0.0;

            double countUsageRatio = (space.getMaxCount() != null && space.getMaxCount() > 0)
                    ? NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue()
                    : 0.0;
            spaceUsageAnalyzeResponse.setSizeUsageRatio(sizeUsageRatio);
            spaceUsageAnalyzeResponse.setCountUsageRatio(countUsageRatio);
            return spaceUsageAnalyzeResponse;
        }
    }

    @Override
    public List<SpaceCategoryAnalyzeResponse> spaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest, loginUser);
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest, queryWrapper);
        queryWrapper.select("category as category", "count(*) as count", "sum(pictureSize) as totalSize").groupBy("category");
        List<SpaceCategoryAnalyzeResponse> spaceCategoryAnalyzeResponses = pictureService.getBaseMapper().selectMaps(queryWrapper)
                .stream()
                .map(result -> {
                    String category = (String) result.get("category");
                    Long count = ((Number) result.get("count")).longValue();
                    Long totalSize = ((Number) result.get("totalSize")).longValue();
                    return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
                })
                .collect(Collectors.toList());
        return  spaceCategoryAnalyzeResponses;
    }

    @Override
    public List<SpaceTagAnalyzeResponse> spaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser) {
     ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
     ThrowUtils.throwIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
     checkSpaceAnalyzeAuth(spaceTagAnalyzeRequest, loginUser);
     QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest, queryWrapper);

        queryWrapper.select("tags");
        List<String> tagsJsonList = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .filter(ObjUtil::isNotNull)
                .map(Object::toString)
                .collect(Collectors.toList());

        Map<String, Long> tagCountMap = tagsJsonList.stream()
                // ["Java", "Python"], ["Java", "PHP"] => "Java", "Python", "Java", "PHP"
                .flatMap(tagsJson -> {
                    try {
                        List<String> tags = JSONUtil.toList(tagsJson, String.class);
                        return tags != null ? tags.stream() : Stream.empty();
                    } catch (Exception e) {
                        return Stream.empty();
                    }
                })
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));


        return tagCountMap.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()) // 降序排序
                .map(entry -> new SpaceTagAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @Override
    public List<SpaceSizeAnalyzeResponse> spaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
      ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
      ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
      checkSpaceAnalyzeAuth(spaceSizeAnalyzeRequest, loginUser);
      QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
      fillAnalyzeQueryWrapper(spaceSizeAnalyzeRequest, queryWrapper);
      queryWrapper.select("pictureSize");
      List<Long> pictureSizeList = pictureService.getBaseMapper().selectObjs(queryWrapper)
              .stream()
              .filter(ObjUtil::isNotNull)
              .filter(obj -> obj instanceof Number)
              .map(size -> ((Number) size).longValue())
              .collect(Collectors.toList());
        final long KB = 1024;
        final long MB = 1024 * KB;

        Map<String, Long> sizeRanges = new LinkedHashMap<>();
        sizeRanges.put("<100KB", 0L);
        sizeRanges.put("100KB-1MB", 0L);
        sizeRanges.put("1MB-5MB", 0L);
        sizeRanges.put("5MB-10MB", 0L);
        sizeRanges.put(">=10MB", 0L); // 增加兜底区间，防止遗漏

        // 单次遍历完成统计
        for (Long size : pictureSizeList) {
            if (size < 100 * KB) {
                sizeRanges.put("<100KB", sizeRanges.get("<100KB") + 1);
            } else if (size < 1 * MB) {
                sizeRanges.put("100KB-1MB", sizeRanges.get("100KB-1MB") + 1);
            } else if (size < 5 * MB) {
                sizeRanges.put("1MB-5MB", sizeRanges.get("1MB-5MB") + 1);
            } else if (size < 10 * MB) {
                sizeRanges.put("5MB-10MB", sizeRanges.get("5MB-10MB") + 1);
            } else {
                sizeRanges.put(">=10MB", sizeRanges.get(">=10MB") + 1);
            }
        }
      return sizeRanges.entrySet().stream()
              .map(entry -> new SpaceSizeAnalyzeResponse(entry.getKey(), entry.getValue()))
              .collect(Collectors.toList());
    }


}





