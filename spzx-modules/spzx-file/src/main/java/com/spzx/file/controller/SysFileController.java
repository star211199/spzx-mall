package com.spzx.file.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.spzx.common.core.domain.R;
import com.spzx.common.core.utils.file.FileUtils;
import com.spzx.file.service.ISysFileService;
import com.spzx.system.api.domain.SysFile;

/**
 * 文件请求处理
 *
 * @author spzx
 */
@RestController
public class SysFileController {
    private static final Logger log = LoggerFactory.getLogger(SysFileController.class);

    @Autowired
    @Qualifier("minioSysFileService")
    private ISysFileService sysFileService;

    /**
     * 文件上传请求
     * 1.必须post请求
     * 2.参数名称必须 “file”,因为前端使用Element-Plus模板技术，upload组件提交请求参数名称就是"file"
     * <p>
     * <p>
     * http://139.198.127.41:9000/spzx/2024/09/23/p1_20240923105524A202.jpg
     *
     *
     * <input class="el-upload__input" name="file" accept="" type="file">
     */
    @PostMapping("upload")
    public R<SysFile> upload(/*@RequestParam("file")*/ MultipartFile file) {
        try {
            // 上传并返回访问地址
            String url = sysFileService.uploadFile(file);
            SysFile sysFile = new SysFile();
            sysFile.setName(FileUtils.getName(url));
            sysFile.setUrl(url); //页面通过图片地址回显图片
            return R.ok(sysFile);
        } catch (Exception e) {
            log.error("上传文件失败", e);
            return R.fail(e.getMessage());
        }
    }
}
