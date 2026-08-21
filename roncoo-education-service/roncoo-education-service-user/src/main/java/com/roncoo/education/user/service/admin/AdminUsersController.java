package com.roncoo.education.user.service.admin;

import com.roncoo.education.common.base.page.Page;
import com.roncoo.education.common.core.base.Result;
import com.roncoo.education.common.log.SysLog;
import com.roncoo.education.common.log.SysLogCache;
import com.roncoo.education.user.service.admin.biz.AdminUsersBiz;
import com.roncoo.education.user.service.admin.biz.AdminUsersImportBiz;
import com.roncoo.education.user.service.admin.req.AdminUsersEditReq;
import com.roncoo.education.user.service.admin.req.AdminUsersPageReq;
import com.roncoo.education.user.service.admin.req.AdminUsersProfileEditReq;
import com.roncoo.education.user.service.admin.req.AdminUsersSaveReq;
import com.roncoo.education.user.service.admin.resp.AdminUsersImportResp;
import com.roncoo.education.user.service.admin.resp.AdminUsersPageResp;
import com.roncoo.education.user.service.admin.resp.AdminUsersViewResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * ADMIN-用户信息
 *
 * @author wujing
 */
@Tag(name = "admin-用户信息")
@RestController
@RequiredArgsConstructor
@RequestMapping("/user/admin/users")
public class AdminUsersController {

    @NotNull
    private final AdminUsersBiz biz;

    @NotNull
    private final AdminUsersImportBiz importBiz;

    @Operation(summary = "用户信息分页")
    @PostMapping(value = "/page")
    public Result<Page<AdminUsersPageResp>> page(@RequestBody AdminUsersPageReq req) {
        return biz.page(req);
    }

    @Operation(summary = "用户信息添加")
    @SysLog(value = "用户信息添加")
    @PostMapping(value = "/save")
    public Result<String> save(@RequestBody @Valid AdminUsersSaveReq req) {
        return biz.save(req);
    }

    @Operation(summary = "用户信息查看")
    @Parameter(name = "id", description = "主键ID", required = true)
    @SysLogCache
    @GetMapping(value = "/view")
    public Result<AdminUsersViewResp> view(@RequestParam Long id) {
        return biz.view(id);
    }

    @Operation(summary = "用户信息修改")
    @SysLog(value = "用户信息修改")
    @PutMapping(value = "/edit")
    public Result<String> edit(@RequestBody @Valid AdminUsersEditReq req) {
        return biz.edit(req);
    }

    @Operation(summary = "员工档案编辑")
    @SysLog(value = "员工档案编辑")
    @PutMapping(value = "/profile/edit")
    public Result<String> profileEdit(@RequestBody @Valid AdminUsersProfileEditReq req) {
        return biz.profileEdit(req);
    }

    @Operation(summary = "下载员工导入模板")
    @GetMapping(value = "/import/template")
    public void importTemplate(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("员工导入模板", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        importBiz.writeTemplate(response.getOutputStream());
    }

    @Operation(summary = "员工批量导入")
    @SysLog(value = "员工批量导入")
    @PostMapping(value = "/import")
    public Result<AdminUsersImportResp> importUsers(@RequestParam(value = "file", required = false) MultipartFile file) {
        return importBiz.importUsers(file);
    }

    @Operation(summary = "用户信息删除")
    @Parameter(name = "id", description = "主键ID", required = true)
    @SysLog(value = "用户信息删除")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam Long id) {
        return biz.delete(id);
    }
}
