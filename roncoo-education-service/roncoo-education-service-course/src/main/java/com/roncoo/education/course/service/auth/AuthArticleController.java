package com.roncoo.education.course.service.auth;

import com.roncoo.education.common.core.base.Result;
import com.roncoo.education.common.tools.IpUtil;
import com.roncoo.education.course.service.auth.biz.AuthArticleBiz;
import com.roncoo.education.course.service.auth.req.AuthArticleReadReq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AUTH-图文课时（二开新增）
 */
@Tag(name = "auth-图文课时")
@RestController
@RequiredArgsConstructor
@RequestMapping("/course/auth/article")
public class AuthArticleController {

    @NotNull
    private final AuthArticleBiz biz;

    @Operation(summary = "上报阅读进度", description = "按滚动比例上报，滚动到底且停留够时长才算完成")
    @PostMapping("/read")
    public Result<String> read(@RequestBody @Valid AuthArticleReadReq req) {
        return biz.read(req);
    }

    @Operation(summary = "签署确认", description = "员工手册等需签署的课时，读完后确认，留痕含时间与IP")
    @PostMapping("/sign")
    public Result<String> sign(@RequestParam Long periodId, HttpServletRequest request) {
        return biz.sign(periodId, IpUtil.getIpAddress(request));
    }
}
