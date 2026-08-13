package com.roncoo.education.common.tools;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 本地存储文件的访问签名（二开新增）
 * <p>
 * 背景：为了让 img / video 标签能直接加载，文件读取接口 /system/images/** 走的是
 * 网关免鉴权路径。内网无所谓，但平台要对公网开放，等于把培训视频公开——
 * 任何人拿到链接就能下载。
 * <p>
 * 方案：public 目录（课程封面等图片）保持免签，private 目录（视频、私有文档）
 * 必须带签名和过期时间。签名 = HMAC-SHA256(相对路径 + "|" + 过期时间戳)，
 * 由服务端在下发播放地址时生成，读取接口校验。
 * 密钥泄露前无法伪造，过期后链接自动失效，外传的链接活不过有效期。
 */
@Slf4j
public final class FileSignUtil {

    private static final String ALGORITHM = "HmacSHA256";

    private FileSignUtil() {
    }

    /**
     * 生成签名
     *
     * @param relativePath 相对路径，如 private/abc.mp4
     * @param expireAt     过期时间（epoch 秒）
     * @param secret       签名密钥
     */
    public static String sign(String relativePath, long expireAt, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] raw = mac.doFinal((relativePath + "|" + expireAt).getBytes(StandardCharsets.UTF_8));
            // URL 安全的 Base64，去掉填充，可直接放进查询串
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) {
            log.error("生成文件签名失败", e);
            return "";
        }
    }

    /**
     * 校验签名是否有效（含过期判断）
     */
    public static boolean verify(String relativePath, long expireAt, String signature, String secret) {
        if (signature == null || signature.isEmpty()) {
            return false;
        }
        if (System.currentTimeMillis() / 1000 > expireAt) {
            // 已过期
            return false;
        }
        String expected = sign(relativePath, expireAt, secret);
        if (expected.isEmpty()) {
            return false;
        }
        // 定长比较，避免通过响应时间差逐字节猜测签名
        return constantTimeEquals(expected, signature);
    }

    private static boolean constantTimeEquals(String a, String b) {
        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);
        if (x.length != y.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < x.length; i++) {
            diff |= x[i] ^ y[i];
        }
        return diff == 0;
    }

    /**
     * 给文件地址追加签名参数
     *
     * @param fileUrl       完整地址，如 https://域名/system/images/private/abc.mp4
     * @param expireSeconds 有效期（秒）
     */
    public static String signUrl(String fileUrl, int expireSeconds, String secret) {
        int idx = fileUrl.indexOf("/system/images/");
        if (idx < 0) {
            // 不是本地存储的地址，原样返回
            return fileUrl;
        }
        String relativePath = fileUrl.substring(idx + "/system/images/".length());
        long expireAt = System.currentTimeMillis() / 1000 + expireSeconds;
        String s = sign(relativePath, expireAt, secret);
        return fileUrl + (fileUrl.contains("?") ? "&" : "?") + "e=" + expireAt + "&s=" + s;
    }
}
