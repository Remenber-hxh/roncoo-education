package com.roncoo.education.course.service.admin;

import com.roncoo.education.common.core.base.Result;
import com.roncoo.education.course.service.admin.biz.AdminScheduleBiz;
import com.roncoo.education.course.service.admin.req.AdminScheduleSaveReq;
import com.roncoo.education.course.service.admin.resp.AdminScheduleRowResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 排课配置（二开）
 */
@Tag(name = "admin-排课配置")
@RestController
@RequiredArgsConstructor
@RequestMapping("/course/admin/schedule")
public class AdminScheduleController {

    private final AdminScheduleBiz biz;

    @Operation(summary = "全部课程的排课配置")
    @GetMapping("/list")
    public Result<List<AdminScheduleRowResp>> list() {
        return biz.list();
    }

    @Operation(summary = "批量保存排课配置")
    @PostMapping("/save")
    public Result<String> save(@RequestBody AdminScheduleSaveReq req) {
        return biz.save(req);
    }
}
