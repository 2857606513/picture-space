package com.gzx.gzxpicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gzx.gzxpicturebackend.model.dto.entity.Space;
import com.gzx.gzxpicturebackend.model.dto.entity.User;
import com.gzx.gzxpicturebackend.model.dto.space.analyze.SpaceCategoryAnalyzeRequest;
import com.gzx.gzxpicturebackend.model.dto.space.analyze.SpaceSizeAnalyzeRequest;
import com.gzx.gzxpicturebackend.model.dto.space.analyze.SpaceTagAnalyzeRequest;
import com.gzx.gzxpicturebackend.model.dto.space.analyze.SpaceUsageAnalyzeRequest;
import com.gzx.gzxpicturebackend.model.dto.vo.space.analyze.SpaceCategoryAnalyzeResponse;
import com.gzx.gzxpicturebackend.model.dto.vo.space.analyze.SpaceSizeAnalyzeResponse;
import com.gzx.gzxpicturebackend.model.dto.vo.space.analyze.SpaceTagAnalyzeResponse;
import com.gzx.gzxpicturebackend.model.dto.vo.space.analyze.SpaceUsageAnalyzeResponse;

import java.util.List;

public interface SpaceAnalyzeService extends IService<Space> {
    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser);
    List<SpaceCategoryAnalyzeResponse> spaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser);
    List<SpaceTagAnalyzeResponse> spaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser);
    List<SpaceSizeAnalyzeResponse> spaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser);
}
