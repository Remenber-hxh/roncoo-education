package com.roncoo.education.course.service.auth;

import com.roncoo.education.common.base.ThreadContext;
import com.roncoo.education.common.base.page.Page;
import com.roncoo.education.common.core.base.Result;
import com.roncoo.education.course.dao.impl.mapper.entity.UserNotice;
import com.roncoo.education.course.service.auth.biz.AuthNoticeBiz;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 我的消息（二开）
 */
@Tag(name = "auth-我的消息")
@RestController
@RequiredArgsConstructor
@RequestMapping("/course/auth/notice")
public class AuthNoticeController {

    private final AuthNoticeBiz biz;

    @Operation(summary = "未读消息数")
    @GetMapping("/unread")
    public Result<Integer> unread() {
        return biz.unreadCount(ThreadContext.userId());
    }

    @Operation(summary = "我的消息列表")
    @GetMapping("/list")
    public Result<Page<UserNotice>> list(@RequestParam(required = false) Integer pageCurrent,
                                         @RequestParam(required = false) Integer pageSize) {
        return biz.page(ThreadContext.userId(), pageCurrent, pageSize);
    }

    @Operation(summary = "标记已读")
    @PutMapping("/read/{id}")
    public Result<String> read(@PathVariable Long id) {
        return biz.read(ThreadContext.userId(), id);
    }

    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public Result<String> readAll() {
        return biz.readAll(ThreadContext.userId());
    }
}
