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
    // TODO: 根据空间级别进行分析，对图片的审核状态进行分析分类统计（按时间维度分析审核量的变化），按照时间统计用户登录次数、图片上传量和活跃度仅管理员调用识别高活跃度用户
}
