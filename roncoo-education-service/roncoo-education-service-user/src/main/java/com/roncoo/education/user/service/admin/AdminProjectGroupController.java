package com.roncoo.education.user.service.admin;

import com.roncoo.education.common.base.page.Page;
import com.roncoo.education.common.core.base.Result;
import com.roncoo.education.common.log.SysLog;
import com.roncoo.education.user.service.admin.biz.AdminProjectGroupBiz;
import com.roncoo.education.user.service.admin.req.AdminProjectGroupEditReq;
import com.roncoo.education.user.service.admin.req.AdminProjectGroupPageReq;
import com.roncoo.education.user.service.admin.req.AdminProjectGroupSaveReq;
import com.roncoo.education.user.service.admin.resp.AdminProjectGroupListResp;
import com.roncoo.education.user.service.admin.resp.AdminProjectGroupPageResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ADMIN-项目组字典
 *
 * @author 二开
 */
@Tag(name = "admin-项目组管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/user/admin/project/group")
public class AdminProjectGroupController {

    @NotNull
    private final AdminProjectGroupBiz biz;

    @Operation(summary = "项目组分页")
    @PostMapping(value = "/page")
    public Result<Page<AdminProjectGroupPageResp>> page(@RequestBody AdminProjectGroupPageReq req) {
        return biz.page(req);
    }

    @Operation(summary = "项目组下拉列表")
    @GetMapping(value = "/list")
    public Result<List<AdminProjectGroupListResp>> list() {
        return biz.list();
    }

    @Operation(summary = "项目组添加")
    @SysLog(value = "项目组添加")
    @PostMapping(value = "/save")
    public Result<String> save(@RequestBody @Valid AdminProjectGroupSaveReq req) {
        return biz.save(req);
    }

    @Operation(summary = "项目组查看")
    @Parameter(name = "id", description = "主键ID", required = true)
    @GetMapping(value = "/view")
    public Result<AdminProjectGroupPageResp> view(@RequestParam Long id) {
        return biz.view(id);
    }

    @Operation(summary = "项目组修改")
    @SysLog(value = "项目组修改")
    @PutMapping(value = "/edit")
    public Result<String> edit(@RequestBody @Valid AdminProjectGroupEditReq req) {
        return biz.edit(req);
    }

    @Operation(summary = "项目组删除")
    @Parameter(name = "id", description = "主键ID", required = true)
    @SysLog(value = "项目组删除")
    @DeleteMapping(value = "/delete")
    public Result<String> delete(@RequestParam Long id) {
        return biz.delete(id);
    }
}
