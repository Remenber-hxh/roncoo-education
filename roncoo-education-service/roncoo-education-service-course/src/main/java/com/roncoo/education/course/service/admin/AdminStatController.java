package com.roncoo.education.course.service.admin;

import com.roncoo.education.common.core.base.Result;
import com.roncoo.education.course.service.admin.biz.AdminStatBiz;
import com.roncoo.education.course.service.admin.resp.AdminStatOverviewResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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

    @Operation(summary = "学习统计看板")
    @GetMapping("/overview")
    public Result<AdminStatOverviewResp> overview(@RequestParam(required = false) Integer days) {
        return biz.overview(days);
    }
}
