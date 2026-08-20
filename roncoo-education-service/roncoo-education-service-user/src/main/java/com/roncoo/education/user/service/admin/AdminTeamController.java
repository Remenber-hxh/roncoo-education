package com.roncoo.education.user.service.admin;

import com.roncoo.education.common.base.page.Page;
import com.roncoo.education.common.core.base.Result;
import com.roncoo.education.common.log.SysLog;
import com.roncoo.education.user.service.admin.biz.AdminTeamBiz;
import com.roncoo.education.user.service.admin.req.AdminTeamEditReq;
import com.roncoo.education.user.service.admin.req.AdminTeamPageReq;
import com.roncoo.education.user.service.admin.req.AdminTeamSaveReq;
import com.roncoo.education.user.service.admin.resp.AdminTeamListResp;
import com.roncoo.education.user.service.admin.resp.AdminTeamPageResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ADMIN-班组字典
 *
 * @author 二开
 */
@Tag(name = "admin-班组管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/user/admin/team")
public class AdminTeamController {

    @NotNull
    private final AdminTeamBiz biz;

    @Operation(summary = "班组分页")
    @PostMapping(value = "/page")
    public Result<Page<AdminTeamPageResp>> page(@RequestBody AdminTeamPageReq req) {
        return biz.page(req);
    }

    @Operation(summary = "班组下拉列表")
    @GetMapping(value = "/list")
    public Result<List<AdminTeamListResp>> list() {
        return biz.list();
    }

    @Operation(summary = "班组添加")
    @SysLog(value = "班组添加")
    @PostMapping(value = "/save")
    public Result<String> save(@RequestBody @Valid AdminTeamSaveReq req) {
        return biz.save(req);
    }

    @Operation(summary = "班组查看")
    @Parameter(name = "id", description = "主键ID", required = true)
    @GetMapping(value = "/view")
    public Result<AdminTeamPageResp> view(@RequestParam Long id) {
        return biz.view(id);
    }

    @Operation(summary = "班组修改")
    @SysLog(value = "班组修改")
    @PutMapping(value = "/edit")
    public Result<String> edit(@RequestBody @Valid AdminTeamEditReq req) {
        return biz.edit(req);
    }

    @Operation(summary = "班组删除")
    @Parameter(name = "id", description = "主键ID", required = true)
    @SysLog(value = "班组删除")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam Long id) {
        return biz.delete(id);
    }
}
