package com.spzx.channel.controller;

import com.spzx.channel.service.IIndexService;
import com.spzx.common.core.web.controller.BaseController;
import com.spzx.common.core.web.domain.AjaxResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@Tag(name = "首页")
@RestController
@RequestMapping("/index")
public class IndexController extends BaseController {

    @Autowired
    IIndexService indexService;

    @Operation(summary = "首页")
    @GetMapping
    public AjaxResult index(){
        //业务层需要远程获取两个部分数据，封装成map
        Map<String,Object> indexData = indexService.index();
        return success(indexData);
    }
}
