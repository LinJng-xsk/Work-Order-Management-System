package com.campus.woms.file;

import com.campus.woms.common.exception.BusinessException;
import com.campus.woms.config.FileUploadProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

/** 文件上传：类型白名单 + 大小限制 + UUID 防重名 + 按日期归档 */
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileUploadProperties properties;

    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        // 1. 类型白名单校验
        String original = file.getOriginalFilename();
        String ext = original == null ? "" : original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!properties.getAllowedTypes().contains(ext)) {
            throw new BusinessException("不支持的文件类型：" + ext);
        }
        // 2. 大小限制
        long maxBytes = properties.getMaxSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new BusinessException("文件大小超过 " + properties.getMaxSizeMb() + "MB 限制");
        }
        // 3. 按日期分目录 + UUID 防重名
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String newName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        File dir = new File(properties.getDir(), datePath);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new BusinessException("创建上传目录失败");
        }
        try {
            file.transferTo(new File(dir, newName));
        } catch (Exception e) {
            throw new BusinessException("文件保存失败");
        }
        // 4. 返回访问 URL（/upload/** 已做静态资源映射）
        return "/upload/" + datePath + "/" + newName;
    }
}
