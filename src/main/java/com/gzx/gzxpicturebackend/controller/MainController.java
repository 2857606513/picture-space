package com.gzx.gzxpicturebackend.controller;

import com.gzx.gzxpicturebackend.common.BaseResponse;
import com.gzx.gzxpicturebackend.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class MainController {
    @GetMapping("/health")
    public BaseResponse<String> healthCheck(){
        return ResultUtils.success("success");
    }
}
