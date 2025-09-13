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
import com.gzx.gzxpicturebackend.model.dto.vo.space.analyze.*;
import com.gzx.gzxpicturebackend.model.dto.enums.SpaceLevelEnum;
import com.gzx.gzxpicturebackend.model.dto.enums.PictureReviewStatusEnum;
import com.gzx.gzxpicturebackend.service.PictureService;
import com.gzx.gzxpicturebackend.service.SpaceAnalyzeService;
import com.gzx.gzxpicturebackend.service.SpaceService;
import com.gzx.gzxpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
* @author guozhongxing
* @description 针对表【space(空间)】的数据库操作Service实现
* @createDate 2025-09-03 10:22:03
*/
@Slf4j
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
            // 此时space已确定不为null
            if (!space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间访问权限");
            }
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
        // 此时spaceUsageAnalyzeRequest已确定不为null
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
    public List<SpaceUserAnalyzeResponse> spaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest, loginUser);

        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        Long userId = spaceUserAnalyzeRequest.getUserId();
        queryWrapper.eq(ObjUtil.isNotNull(userId) , "userId", userId);
        fillAnalyzeQueryWrapper(spaceUserAnalyzeRequest, queryWrapper);

        String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();

        final String PERIOD_FIELD = "period";
        final String COUNT_FIELD = "count";

        switch (timeDimension) {
            case "day":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m-%d') as " + PERIOD_FIELD, "count(*) as " + COUNT_FIELD);
                break;
            case "week":
                queryWrapper.select("YEARWEEK(createTime) as " + PERIOD_FIELD, "count(*) as " + COUNT_FIELD);
                break;
            case "month":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m') as " + PERIOD_FIELD, "count(*) as " + COUNT_FIELD);
                break;
            default:
                // 日志记录详细错误，对外返回通用错误
                log.warn("Unsupported time dimension: {}", timeDimension);
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "时间维度参数错误");
        }

        // 分组排序
        queryWrapper.groupBy(PERIOD_FIELD).orderByAsc(PERIOD_FIELD);

        // 查询并封装结果
        List<Map<String, Object>> queryResult = pictureService.getBaseMapper().selectMaps(queryWrapper);
        return queryResult
                .stream()
                .map(result -> {
                    Object periodObj = result.get(PERIOD_FIELD);
                    Object countObj = result.get(COUNT_FIELD);

                    if (periodObj == null || countObj == null) {
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "查询结果字段缺失");
                    }

                    String period = periodObj.toString();
                    long count;
                    try {
                        count = ((Number) countObj).longValue();
                    } catch (ClassCastException e) {
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "count字段类型错误");
                    }

                    return new SpaceUserAnalyzeResponse(period, count);
                })
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

    @Override
    public List<Space> getAllSpaces(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 此时loginUser已确定不为null
        if (!"admin".equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问");
        }

        Integer topN = spaceRankAnalyzeRequest.getTopN();
        ThrowUtils.throwIf(topN == null || topN <= 0 || topN > 10000, ErrorCode.PARAMS_ERROR, "topN参数必须在1-10000之间");


        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "spaceName", "totalCount", "totalSize")
                .orderByDesc("totalCount", "totalSize")
                .last("LIMIT" + topN);
        return spaceService.list(queryWrapper);
    }

    @Override
    public List<SpaceLevelAnalyzeResponse> spaceLevelAnalyze(SpaceLevelAnalyzeRequest spaceLevelAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(spaceLevelAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        
        // 只有管理员可以查看空间级别分析
        // 此时loginUser已确定不为null
        if (!userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问");
        }
        
        log.info("开始空间级别分析，用户ID: {}", loginUser.getId());
        
        // 查询所有空间，按级别分组统计
        QueryWrapper<Space> spaceQueryWrapper = new QueryWrapper<>();
        spaceQueryWrapper.select("spaceLevel", "totalSize", "totalCount", "maxSize", "maxCount");
        List<Space> spaceList = spaceService.list(spaceQueryWrapper);
        
        // 按空间级别分组统计
        Map<Integer, List<Space>> spaceLevelMap = spaceList.stream()
                .collect(Collectors.groupingBy(Space::getSpaceLevel));
        
        List<SpaceLevelAnalyzeResponse> resultList = new ArrayList<>();
        
        // 遍历所有空间级别
        for (SpaceLevelEnum levelEnum : SpaceLevelEnum.values()) {
            Integer levelValue = levelEnum.getValue();
            List<Space> spacesInLevel = spaceLevelMap.getOrDefault(levelValue, new ArrayList<>());
            
            SpaceLevelAnalyzeResponse response = new SpaceLevelAnalyzeResponse();
            response.setSpaceLevel(levelValue);
            response.setSpaceLevelName(levelEnum.getText());
            response.setSpaceCount((long) spacesInLevel.size());
            
            // 计算该级别空间的总大小和总图片数量
            long totalSize = spacesInLevel.stream()
                    .mapToLong(space -> space.getTotalSize() != null ? space.getTotalSize() : 0L)
                    .sum();
            long totalPictureCount = spacesInLevel.stream()
                    .mapToLong(space -> space.getTotalCount() != null ? space.getTotalCount() : 0L)
                    .sum();
            
            response.setTotalSize(totalSize);
            response.setTotalPictureCount(totalPictureCount);
            
            // 计算平均使用率
            if (!spacesInLevel.isEmpty()) {
                double avgSizeUsageRatio = spacesInLevel.stream()
                        .filter(space -> space.getMaxSize() != null && space.getMaxSize() > 0)
                        .mapToDouble(space -> (double) space.getTotalSize() / space.getMaxSize() * 100)
                        .average()
                        .orElse(0.0);
                
                double avgCountUsageRatio = spacesInLevel.stream()
                        .filter(space -> space.getMaxCount() != null && space.getMaxCount() > 0)
                        .mapToDouble(space -> (double) space.getTotalCount() / space.getMaxCount() * 100)
                        .average()
                        .orElse(0.0);
                
                response.setAvgSizeUsageRatio(NumberUtil.round(avgSizeUsageRatio, 2).doubleValue());
                response.setAvgCountUsageRatio(NumberUtil.round(avgCountUsageRatio, 2).doubleValue());
            } else {
                response.setAvgSizeUsageRatio(0.0);
                response.setAvgCountUsageRatio(0.0);
            }
            
            resultList.add(response);
        }
        
        log.info("空间级别分析完成，共分析 {} 个级别", resultList.size());
        return resultList;
    }
    
    /**
     * 图片审核状态分析（按时间维度）
     */
    public List<PictureReviewAnalyzeResponse> pictureReviewAnalyze(PictureReviewAnalyzeRequest request, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        
        // 只有管理员可以查看审核状态分析
        // 此时loginUser和request已确定不为null
        if (!userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问");
        }
        
        log.info("开始图片审核状态分析，用户ID: {}, 时间维度: {}", loginUser.getId(), request.getTimeDimension());
        
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(request, queryWrapper);
        
        String timeDimension = request.getTimeDimension();
        final String PERIOD_FIELD = "period";
        
        // 根据时间维度设置查询字段
        switch (timeDimension) {
            case "day":
                queryWrapper.select("DATE_FORMAT(reviewTime, '%Y-%m-%d') as " + PERIOD_FIELD, 
                        "reviewStatus", "count(*) as count");
                break;
            case "week":
                queryWrapper.select("YEARWEEK(reviewTime) as " + PERIOD_FIELD, 
                        "reviewStatus", "count(*) as count");
                break;
            case "month":
                queryWrapper.select("DATE_FORMAT(reviewTime, '%Y-%m') as " + PERIOD_FIELD, 
                        "reviewStatus", "count(*) as count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "时间维度参数错误");
        }
        
        queryWrapper.isNotNull("reviewTime")
                .groupBy(PERIOD_FIELD, "reviewStatus")
                .orderByAsc(PERIOD_FIELD);
        
        List<Map<String, Object>> queryResult = pictureService.getBaseMapper().selectMaps(queryWrapper);
        
        // 按时间维度分组统计
        Map<String, Map<Integer, Long>> periodReviewMap = new LinkedHashMap<>();
        
        for (Map<String, Object> result : queryResult) {
            String period = result.get(PERIOD_FIELD).toString();
            Integer reviewStatus = (Integer) result.get("reviewStatus");
            Long count = ((Number) result.get("count")).longValue();
            
            periodReviewMap.computeIfAbsent(period, k -> new HashMap<>())
                    .put(reviewStatus, count);
        }
        
        // 构建响应结果
        List<PictureReviewAnalyzeResponse> responseList = new ArrayList<>();
        
        for (Map.Entry<String, Map<Integer, Long>> entry : periodReviewMap.entrySet()) {
            String period = entry.getKey();
            Map<Integer, Long> reviewMap = entry.getValue();
            
            Long reviewingCount = reviewMap.getOrDefault(PictureReviewStatusEnum.REVIEWING.getValue(), 0L);
            Long passCount = reviewMap.getOrDefault(PictureReviewStatusEnum.PASS.getValue(), 0L);
            Long rejectCount = reviewMap.getOrDefault(PictureReviewStatusEnum.REJECT.getValue(), 0L);
            Long totalCount = reviewingCount + passCount + rejectCount;
            
            PictureReviewAnalyzeResponse response = new PictureReviewAnalyzeResponse();
            response.setPeriod(period);
            response.setReviewingCount(reviewingCount);
            response.setPassCount(passCount);
            response.setRejectCount(rejectCount);
            response.setTotalCount(totalCount);
            
            if (totalCount > 0) {
                response.setPassRate(NumberUtil.round(passCount * 100.0 / totalCount, 2).doubleValue());
                response.setRejectRate(NumberUtil.round(rejectCount * 100.0 / totalCount, 2).doubleValue());
            } else {
                response.setPassRate(0.0);
                response.setRejectRate(0.0);
            }
            
            responseList.add(response);
        }
        
        log.info("图片审核状态分析完成，共分析 {} 个时间段", responseList.size());
        return responseList;
    }
    
    /**
     * 用户活跃度分析（仅管理员）
     */
    public List<UserActivityAnalyzeResponse> userActivityAnalyze(UserActivityAnalyzeRequest request, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        
        // 只有管理员可以查看用户活跃度分析
        // 此时loginUser和request已确定不为null
        if (!userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问");
        }
        
        log.info("开始用户活跃度分析，用户ID: {}, 时间维度: {}", loginUser.getId(), request.getTimeDimension());
        
        String timeDimension = request.getTimeDimension();
        final String PERIOD_FIELD = "period";
        final String USER_ID_FIELD = "userId";
        final String COUNT_FIELD = "count";
        
        // 分析图片上传活跃度
        QueryWrapper<Picture> pictureQueryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(request, pictureQueryWrapper);
        
        switch (timeDimension) {
            case "day":
                pictureQueryWrapper.select("DATE_FORMAT(createTime, '%Y-%m-%d') as " + PERIOD_FIELD, 
                        "userId as " + USER_ID_FIELD, "count(*) as " + COUNT_FIELD);
                break;
            case "week":
                pictureQueryWrapper.select("YEARWEEK(createTime) as " + PERIOD_FIELD, 
                        "userId as " + USER_ID_FIELD, "count(*) as " + COUNT_FIELD);
                break;
            case "month":
                pictureQueryWrapper.select("DATE_FORMAT(createTime, '%Y-%m') as " + PERIOD_FIELD, 
                        "userId as " + USER_ID_FIELD, "count(*) as " + COUNT_FIELD);
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "时间维度参数错误");
        }
        
        pictureQueryWrapper.groupBy(PERIOD_FIELD, USER_ID_FIELD)
                .orderByAsc(PERIOD_FIELD, USER_ID_FIELD);
        
        List<Map<String, Object>> uploadResult = pictureService.getBaseMapper().selectMaps(pictureQueryWrapper);
        
        // 构建用户活跃度数据
        Map<String, Map<Long, UserActivityAnalyzeResponse>> periodUserMap = new LinkedHashMap<>();
        
        for (Map<String, Object> result : uploadResult) {
            String period = result.get(PERIOD_FIELD).toString();
            Long userId = ((Number) result.get(USER_ID_FIELD)).longValue();
            Long uploadCount = ((Number) result.get(COUNT_FIELD)).longValue();
            
            UserActivityAnalyzeResponse response = periodUserMap
                    .computeIfAbsent(period, k -> new HashMap<>())
                    .computeIfAbsent(userId, k -> {
                        UserActivityAnalyzeResponse newResponse = new UserActivityAnalyzeResponse();
                        newResponse.setUserId(userId);
                        newResponse.setPeriod(period);
                        newResponse.setLoginCount(0L); // 暂时设为0，需要从用户登录日志获取
                        newResponse.setUploadCount(0L);
                        return newResponse;
                    });
            
            response.setUploadCount(uploadCount);
        }
        
        // 计算活跃度评分并识别高活跃度用户
        List<UserActivityAnalyzeResponse> responseList = new ArrayList<>();
        Integer activityThreshold = request.getActivityThreshold() != null ? request.getActivityThreshold() : 10;
        Boolean highActivityOnly = request.getHighActivityOnly() != null ? request.getHighActivityOnly() : false;
        
        for (Map<Long, UserActivityAnalyzeResponse> userMap : periodUserMap.values()) {
            for (UserActivityAnalyzeResponse response : userMap.values()) {
                // 计算活跃度评分（上传数量 * 2 + 登录次数）
                double activityScore = response.getUploadCount() * 2.0 + response.getLoginCount();
                response.setActivityScore(NumberUtil.round(activityScore, 2).doubleValue());
                
                // 判断是否为高活跃度用户
                boolean isHighActivity = activityScore >= activityThreshold;
                response.setIsHighActivity(isHighActivity);
                
                // 获取用户名
                User user = userService.getById(response.getUserId());
                if (user != null) {
                    response.setUserName(user.getUserName());
                }
                
                // 如果只返回高活跃度用户，则过滤
                if (!highActivityOnly || isHighActivity) {
                    responseList.add(response);
                }
            }
        }
        
        // 按活跃度评分降序排序
        responseList.sort((a, b) -> Double.compare(b.getActivityScore(), a.getActivityScore()));
        
        log.info("用户活跃度分析完成，共分析 {} 个用户活跃度记录", responseList.size());
        return responseList;
    }

}





