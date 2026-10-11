package com.campus.woms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/** 上传配置：application.yml 的 woms.upload.* */
@Data
@Component
@ConfigurationProperties(prefix = "woms.upload")
public class FileUploadProperties {

    /** 存储根目录 */
    private String dir = "./upload";

    /** 大小上限（MB） */
    private long maxSizeMb = 5;

    /** 允许的扩展名白名单 */
    private List<String> allowedTypes = List.of("jpg", "jpeg", "png", "gif");
}
