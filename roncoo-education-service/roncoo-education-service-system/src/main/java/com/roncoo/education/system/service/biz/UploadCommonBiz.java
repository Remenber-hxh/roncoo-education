package com.roncoo.education.system.service.biz;

import cn.hutool.core.util.ObjectUtil;
import com.roncoo.education.common.core.base.Result;
import com.roncoo.education.common.core.enums.StoragePlatformEnum;
import com.roncoo.education.common.tools.DocUtil;
import com.roncoo.education.common.tools.FileUtils;
import com.roncoo.education.common.upload.Upload;
import com.roncoo.education.common.upload.UploadFace;
import com.roncoo.education.system.service.biz.resp.UploadDocResp;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Objects;

/**
 * 上传接口
 *
 * @author wuyun
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UploadCommonBiz {

    @NotNull
    private final Map<String, UploadFace> uploadFaceMap;

    @NotNull
    private final SysConfigCommonBiz sysConfigCommonBiz;

    public Result<String> uploadPic(MultipartFile picFile) {
        if (!FileUtils.isPic(picFile)) {
            return Result.error("目前只支持：{}，请选择图片上传".replace("{}", FileUtils.PIC_TYPE_MAP));
        }

        Upload upload = sysConfigCommonBiz.getSysConfig(Upload.class);
        if (ObjectUtil.isEmpty(upload) || ObjectUtil.isEmpty(upload.getStoragePlatform())) {
            return Result.error("上传参数没配置");
        }

        UploadFace uploadFace = uploadFaceMap.get(Objects.requireNonNull(StoragePlatformEnum.byCode(upload.getStoragePlatform())).getMode());
        if (ObjectUtil.isEmpty(uploadFace)) {
            return Result.error("暂不支持该类型");
        }
        String fileUrl = uploadFace.uploadPic(picFile, upload);
        return Result.success(fileUrl);
    }

    /**
     * 音视频上传到本地存储（二开新增）
     * <p>
     * roncoo 原本的音视频是浏览器直传第三方点播云（保利威等），不经过服务端。
     * 选择本地存储后没有第三方云可传，改为传到服务端落盘，
     * 返回的地址直接作为播放地址（见 AuthCourseBiz.localPlayConfig）。
     *
     * @return 视频文件的访问地址
     */
    public Result<String> uploadVideo(MultipartFile videoFile) {
        if (ObjectUtil.isEmpty(videoFile) || videoFile.isEmpty()) {
            return Result.error("请选择文件");
        }

        Upload upload = sysConfigCommonBiz.getSysConfig(Upload.class);
        if (ObjectUtil.isEmpty(upload) || ObjectUtil.isEmpty(upload.getStoragePlatform())) {
            return Result.error("上传参数没配置");
        }
        if (!StoragePlatformEnum.LOCAL.getCode().equals(upload.getStoragePlatform())) {
            return Result.error("该接口仅用于本地存储，当前存储平台不是本地存储");
        }

        UploadFace uploadFace = uploadFaceMap.get(StoragePlatformEnum.LOCAL.getMode());
        if (ObjectUtil.isEmpty(uploadFace)) {
            return Result.error("暂不支持该类型");
        }
        // 落私有目录：平台对公网开放，视频是公司资产，不能给直链。
        // 播放时由 AuthCourseBiz 下发带过期时间的签名地址（见 FileSignUtil）。
        String fileUrl = uploadFace.uploadDoc(videoFile, upload, false);
        if (!StringUtils.hasText(fileUrl)) {
            return Result.error("上传失败，请查看服务端日志");
        }
        return Result.success(fileUrl);
    }

    public Result<UploadDocResp> uploadDoc(MultipartFile docFile, Boolean isPublicRead) {
        if (!isPublicRead && !FileUtils.isDoc(docFile)) {
            return Result.error("目前只支持：{}，请选择文件上传".replace("{}", FileUtils.DOC_TYPE_MAP));
        }
        if (isPublicRead && !FileUtils.isApp(docFile)) {
            return Result.error("目前只支持：{}，请选择文件上传".replace("{}", FileUtils.APP_TYPE_MAP));
        }

        UploadDocResp resp = new UploadDocResp();
        Upload upload = sysConfigCommonBiz.getSysConfig(Upload.class);
        resp.setStoragePlatform(upload.getStoragePlatform());
        if (ObjectUtil.isEmpty(upload) || ObjectUtil.isEmpty(upload.getStoragePlatform())) {
            return Result.error("上传参数没配置");
        }

        UploadFace uploadFace = uploadFaceMap.get(Objects.requireNonNull(StoragePlatformEnum.byCode(upload.getStoragePlatform())).getMode());
        if (ObjectUtil.isEmpty(uploadFace)) {
            return Result.error("暂不支持该类型");
        }
        resp.setDocUrl(uploadFace.uploadDoc(docFile, upload, isPublicRead));
        try {
            resp.setPageCount(DocUtil.getDocPageCount(docFile.getOriginalFilename(), docFile.getInputStream()));
            return Result.success(resp);
        } catch (Exception e) {
            log.error("文档错误", e);
            return Result.error("文档错误");
        }
    }

}
