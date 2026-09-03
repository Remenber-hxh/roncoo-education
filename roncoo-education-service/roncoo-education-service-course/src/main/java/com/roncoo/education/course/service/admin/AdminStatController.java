package com.roncoo.education.course.service.admin;

import com.roncoo.education.common.core.base.Result;
import com.roncoo.education.course.service.admin.biz.AdminRemindBiz;
import com.roncoo.education.course.service.admin.biz.AdminStatBiz;
import com.roncoo.education.course.service.admin.req.AdminRemindReq;
import com.roncoo.education.course.service.admin.resp.AdminRemindResp;
import com.roncoo.education.course.service.admin.resp.AdminStatOverviewResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学习统计接口（二开）
 */
@Tag(name = "admin-学习统计")
@RestController
@RequiredArgsConstructor
@RequestMapping("/course/admin/stat")
public class AdminStatController {

    private final AdminStatBiz biz;

    private final AdminRemindBiz remindBiz;

    private final com.roncoo.education.course.job.AutoAssignJob autoAssignJob;

    @Operation(summary = "学习统计看板")
    @GetMapping("/overview")
    public Result<AdminStatOverviewResp> overview(@RequestParam(required = false) Integer days) {
        return biz.overview(days);
    }

    @Operation(summary = "批量催办逾期未完成人员")
    @PostMapping("/remind")
    public Result<AdminRemindResp> remind(@RequestBody AdminRemindReq req) {
        return remindBiz.remind(req);
    }

    /**
     * 自动排课平时由定时任务凌晨触发。这里提供手工触发，
     * 用于配完推送规则后立刻验证效果，不必等到第二天。
     */
    @Operation(summary = "立即执行一次自动排课")
    @PostMapping("/auto-assign/run")
    public Result<String> runAutoAssign() {
        int n = autoAssignJob.doAssign();
        return Result.success("已执行，新增指派 " + n + " 条");
    }
}
