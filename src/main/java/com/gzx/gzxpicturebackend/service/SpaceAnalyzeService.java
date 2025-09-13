package com.gzx.gzxpicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gzx.gzxpicturebackend.model.dto.entity.Space;
import com.gzx.gzxpicturebackend.model.dto.entity.User;
import com.gzx.gzxpicturebackend.model.dto.space.analyze.*;
import com.gzx.gzxpicturebackend.model.dto.vo.space.analyze.*;

import java.util.List;

public interface SpaceAnalyzeService extends IService<Space> {
    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser);
    List<SpaceCategoryAnalyzeResponse> spaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser);
    List<SpaceTagAnalyzeResponse> spaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser);
    List<SpaceUserAnalyzeResponse> spaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser);
    List<SpaceSizeAnalyzeResponse> spaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser);
    List<Space> getAllSpaces(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest,User loginUser);
    List<SpaceLevelAnalyzeResponse> spaceLevelAnalyze(SpaceLevelAnalyzeRequest spaceLevelAnalyzeRequest, User loginUser);
    
    /**
     * 图片审核状态分析（按时间维度）
     */
    List<PictureReviewAnalyzeResponse> pictureReviewAnalyze(PictureReviewAnalyzeRequest request, User loginUser);
    
    /**
     * 用户活跃度分析（仅管理员）
     */
    List<UserActivityAnalyzeResponse> userActivityAnalyze(UserActivityAnalyzeRequest request, User loginUser);
}
