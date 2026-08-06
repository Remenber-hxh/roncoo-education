package com.roncoo.education.common.upload;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 上传接口
 *
 * @author LYQ
 */
@Data
@Accessors(chain = true)
public class Upload implements Serializable {

    private static final long serialVersionUID = 1195869049655301491L;

    /**
     * 本地存储
     * localStoragePath   文件落盘的根目录，如 D:/视频培训/uploads
     * localStorageDomain 对外访问前缀，如 http://localhost:7700/system/images
     */
    private String localStoragePath;
    private String localStorageDomain;

    /**
     * MinIO
     */
    private String minioEndpoint;
    private String minioAccessKey;
    private String minioSecretKey;
    private String minioDomain;
    private String minioBucket;
    private String minioPreviewUrl;

    /**
     * OSS
     */
    private String aliyunOssEndpoint;
    private String aliyunAccessKeyId;
    private String aliyunAccessKeySecret;
    private String aliyunOssUrl;
    private String aliyunOssBucket;


    /**
     * 存储平台，参考：StoragePlatformEnum
     */
    private Integer storagePlatform;

    /**
     * 站点域名
     */
    private String websiteDomain;
}
