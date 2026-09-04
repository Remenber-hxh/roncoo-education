package com.roncoo.education.common.tools;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 企业微信群机器人（二开新增）
 * <p>
 * 在企业微信群里添加「群机器人」会得到一个 Webhook 地址，往它 POST 一段 JSON
 * 就能把消息发到那个群。它是<b>单向外发</b>的：
 * <ul>
 * <li>不需要 CorpID / Secret，也不需要可信域名——是我们主动往腾讯的地址发请求，
 *     不是腾讯来访问我们。所以在没有公网服务器和域名的情况下也能用。</li>
 * <li>机器人只能往群里发，收不到回复，也没法给某个人单独发私信。
 *     要提醒到具体的人，靠 @ ——text 类型支持按手机号 @，
 *     这正好用得上员工档案里已有的手机号。</li>
 * </ul>
 * <p>
 * 注意 markdown 类型不支持按手机号 @人，需要 @ 的消息必须用 text 类型。
 * 官方对单个机器人有每分钟 20 条的频率限制，批量发送要自己控速。
 *
 * @author 二开
 */
@Slf4j
public final class WeComBotUtil {

    private WeComBotUtil() {
    }

    /** 单条消息的正文上限，官方限制 4096 字节，留些余量按字符数粗算 */
    private static final int MAX_CONTENT = 1800;

    private static final int TIMEOUT_MS = 8000;

    /**
     * 发文本消息，可按手机号 @人。
     *
     * @param webhook 群机器人的 Webhook 完整地址
     * @param content 正文
     * @param mobiles 需要 @ 的成员手机号，可为空
     * @return 是否发送成功
     */
    public static boolean sendText(String webhook, String content, List<String> mobiles) {
        if (!StringUtils.hasText(webhook) || !StringUtils.hasText(content)) {
            return false;
        }
        JSONObject text = new JSONObject().set("content", trim(content));
        if (CollUtil.isNotEmpty(mobiles)) {
            JSONArray arr = new JSONArray();
            mobiles.stream().filter(StringUtils::hasText).distinct().forEach(arr::add);
            if (!arr.isEmpty()) {
                text.set("mentioned_mobile_list", arr);
            }
        }
        return post(webhook, new JSONObject().set("msgtype", "text").set("text", text));
    }

    /**
     * 发 markdown 消息。适合发带格式的汇总（标题、列表、数字着色）。
     * <b>不支持 @人</b>——需要 @ 的用 {@link #sendText}。
     */
    public static boolean sendMarkdown(String webhook, String content) {
        if (!StringUtils.hasText(webhook) || !StringUtils.hasText(content)) {
            return false;
        }
        return post(webhook, new JSONObject().set("msgtype", "markdown")
                .set("markdown", new JSONObject().set("content", trim(content))));
    }

    private static boolean post(String webhook, JSONObject body) {
        try {
            String resp = HttpRequest.post(webhook)
                    .header("Content-Type", "application/json")
                    .body(JSONUtil.toJsonStr(body))
                    .timeout(TIMEOUT_MS)
                    .execute()
                    .body();
            JSONObject json = JSONUtil.parseObj(resp);
            int code = json.getInt("errcode", -1);
            if (code == 0) {
                return true;
            }
            // 把腾讯返回的错误原样记下来。常见的是 webhook 失效（机器人被移除）
            // 和 45009 触发频率限制，两者的处理方式完全不同，不能笼统记「发送失败」
            log.warn("企业微信机器人发送失败 errcode={} errmsg={}", code, json.getStr("errmsg"));
            return false;
        } catch (Exception e) {
            // 通知发不出去不该让业务流程失败：催办的主体是站内消息，
            // 群通知是附加的一层触达
            log.warn("企业微信机器人请求异常", e);
            return false;
        }
    }

    private static String trim(String s) {
        return s.length() <= MAX_CONTENT ? s : s.substring(0, MAX_CONTENT) + "…";
    }
}
