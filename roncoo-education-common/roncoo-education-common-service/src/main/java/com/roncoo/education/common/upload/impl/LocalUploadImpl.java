package com.roncoo.education.common.upload.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.roncoo.education.common.upload.Upload;
import com.roncoo.education.common.upload.UploadFace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 本地磁盘存储
 * <p>
 * 二开新增。roncoo 原本只提供 MinIO 和阿里云 OSS 两种实现，
 * 内网自建场景不想额外维护一个对象存储服务，故补一个落本地磁盘的实现。
 * <p>
 * 文件存到 {localStoragePath}/public|private/ 下，文件名用 UUID，
 * 对外地址为 {localStorageDomain}/public|private/{文件名}，
 * 由 system 服务的 LocalFileController 提供读取。
 *
 * @see com.roncoo.education.common.core.enums.StoragePlatformEnum#LOCAL
 */
@Slf4j
@Component(value = "local")
public class LocalUploadImpl implements UploadFace {

    private static final String PUBLIC = "public";
    private static final String PRIVATE = "private";

    @Override
    public String uploadPic(MultipartFile file, Upload upload) {
        return save(file, upload, PUBLIC);
    }

    @Override
    public String uploadDoc(MultipartFile file, Upload upload, Boolean isPublicRead) {
        return save(file, upload, Boolean.FALSE.equals(isPublicRead) ? PRIVATE : PUBLIC);
    }

    /**
     * 本地存储没有预签名机制，文件地址本身就是下载地址
     */
    @Override
    public String getDownloadUrl(String docUrl, int expireSeconds, Upload upload) {
        return docUrl;
    }

    /**
     * 本地存储未接入 kkFileView 之类的预览服务
     */
    @Override
    public String getPreviewConfig(String docUrl, int expireSeconds, Upload upload) {
        return "";
    }

    private String save(MultipartFile file, Upload upload, String dir) {
        if (!StringUtils.hasText(upload.getLocalStoragePath()) || !StringUtils.hasText(upload.getLocalStorageDomain())) {
            log.error("本地存储未配置，localStoragePath 或 localStorageDomain 为空，请在【系统管理-参数配置-存储】中填写");
            return "";
        }
        try {
            String suffix = FileUtil.getSuffix(file.getOriginalFilename());
            String fileName = IdUtil.simpleUUID() + (StringUtils.hasText(suffix) ? "." + suffix : "");

            Path dirPath = Paths.get(upload.getLocalStoragePath()).resolve(dir);
            Files.createDirectories(dirPath);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dirPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            }

            String domain = upload.getLocalStorageDomain();
            if (domain.endsWith("/")) {
                domain = domain.substring(0, domain.length() - 1);
            }
            return domain + "/" + dir + "/" + fileName;
        } catch (Exception e) {
            log.error("本地存储上传失败", e);
            return "";
        }
    }
}
