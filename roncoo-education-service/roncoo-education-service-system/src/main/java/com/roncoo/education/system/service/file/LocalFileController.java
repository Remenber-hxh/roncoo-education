package com.roncoo.education.system.service.file;

import com.roncoo.education.common.upload.Upload;
import com.roncoo.education.system.service.biz.SysConfigCommonBiz;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地存储的文件读取接口（二开新增）
 * <p>
 * 与 {@link com.roncoo.education.common.upload.impl.LocalUploadImpl} 配套。
 * 路径特意放在 /images 下，网关 FilterUtil.IMAGES 对该前缀免鉴权，
 * 这样门户和后台的 img 标签可以直接引用。
 *
 * @author 二开
 */
@Slf4j
@Tag(name = "本地存储文件读取")
@RestController
@RequiredArgsConstructor
@RequestMapping("/system/images")
public class LocalFileController {

    private final SysConfigCommonBiz sysConfigCommonBiz;

    @GetMapping("/**")
    public ResponseEntity<Resource> get(HttpServletRequest request) {
        String relative = extractRelativePath(request);
        if (!StringUtils.hasText(relative)) {
            return ResponseEntity.notFound().build();
        }

        Upload upload = sysConfigCommonBiz.getSysConfig(Upload.class);
        if (upload == null || !StringUtils.hasText(upload.getLocalStoragePath())) {
            log.error("本地存储未配置 localStoragePath");
            return ResponseEntity.notFound().build();
        }

        Path base = Paths.get(upload.getLocalStoragePath()).toAbsolutePath().normalize();
        Path target = base.resolve(relative).normalize();

        // 防目录穿越：解析后必须仍在根目录内，否则一律拒绝
        if (!target.startsWith(base)) {
            log.warn("拒绝越界的文件访问请求：{}", relative);
            return ResponseEntity.notFound().build();
        }
        if (!Files.isRegularFile(target) || !Files.isReadable(target)) {
            return ResponseEntity.notFound().build();
        }

        FileSystemResource resource = new FileSystemResource(target);
        MediaType mediaType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                .body(resource);
    }

    /**
     * 取出 /system/images/ 之后的部分，如 public/xxx.png
     */
    @Nullable
    private String extractRelativePath(HttpServletRequest request) {
        String uri = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        if (!StringUtils.hasText(uri)) {
            uri = request.getRequestURI();
        }
        int idx = uri.indexOf("/system/images/");
        if (idx < 0) {
            return null;
        }
        return uri.substring(idx + "/system/images/".length());
    }
}
