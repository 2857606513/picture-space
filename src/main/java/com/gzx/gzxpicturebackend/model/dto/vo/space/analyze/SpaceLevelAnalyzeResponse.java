package com.gzx.gzxpicturebackend.model.dto.vo.space.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 空间级别分析响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpaceLevelAnalyzeResponse implements Serializable {

    /**
     * 空间级别
     */
    private Integer spaceLevel;

    /**
     * 空间级别名称
     */
    private String spaceLevelName;

    /**
     * 该级别空间数量
     */
    private Long spaceCount;

    /**
     * 该级别空间总大小
     */
    private Long totalSize;

    /**
     * 该级别空间总图片数量
     */
    private Long totalPictureCount;

    /**
     * 该级别空间平均使用率（大小）
     */
    private Double avgSizeUsageRatio;

    /**
     * 该级别空间平均使用率（数量）
     */
    private Double avgCountUsageRatio;

    private static final long serialVersionUID = 1L;
}