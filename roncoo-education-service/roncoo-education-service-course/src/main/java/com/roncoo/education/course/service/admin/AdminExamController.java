package com.roncoo.education.course.service.admin;

import com.roncoo.education.common.base.page.Page;
import com.roncoo.education.common.core.base.Result;
import com.roncoo.education.course.dao.impl.mapper.entity.ExamPaper;
import com.roncoo.education.course.dao.impl.mapper.entity.ExamQuestion;
import com.roncoo.education.course.dao.impl.mapper.entity.ExamRecord;
import com.roncoo.education.course.dao.impl.mapper.entity.UserCourseAssign;
import com.roncoo.education.course.service.admin.biz.AdminExamBiz;
import com.roncoo.education.course.service.admin.biz.AdminExamQuestionImportBiz;
import com.roncoo.education.course.service.admin.req.*;
import com.roncoo.education.course.service.admin.resp.AdminExamPaperViewResp;
import com.roncoo.education.course.service.admin.resp.AdminExamQuestionImportResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 考试管理接口（二开）
 */
@Tag(name = "admin-考试管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/course/admin/exam")
public class AdminExamController {

    private final AdminExamBiz biz;
    private final AdminExamQuestionImportBiz importBiz;

    // ===== 题库 =====

    @Operation(summary = "题库分页")
    @PostMapping("/question/page")
    public Result<Page<ExamQuestion>> questionPage(@RequestBody AdminExamQuestionPageReq req) {
        return biz.questionPage(req);
    }

    @Operation(summary = "按章节统计题量")
    @GetMapping("/question/count/chapter")
    public Result<java.util.List<java.util.Map<String, Object>>> questionCountByChapter(@RequestParam Long courseId) {
        return biz.questionCountByChapter(courseId);
    }

    @Operation(summary = "题目新增")
    @PostMapping("/question/save")
    public Result<String> questionSave(@RequestBody AdminExamQuestionEditReq req) {
        return biz.questionSave(req);
    }

    @Operation(summary = "题目修改")
    @PutMapping("/question/update")
    public Result<String> questionUpdate(@RequestBody AdminExamQuestionEditReq req) {
        return biz.questionUpdate(req);
    }

    @Operation(summary = "题目删除")
    @DeleteMapping("/question/delete")
    public Result<String> questionDelete(@RequestParam Long id) {
        return biz.questionDelete(id);
    }

    @Operation(summary = "题目详情")
    @GetMapping("/question/view")
    public Result<ExamQuestion> questionView(@RequestParam Long id) {
        return biz.questionView(id);
    }

    // ===== 题库导入导出 =====
    // 导出的文件就是导入模板，出题人导出改完直接导回来，不用在两种格式之间转换

    @Operation(summary = "导出题目(按当前筛选条件)")
    @GetMapping("/question/export")
    public void questionExport(AdminExamQuestionPageReq req, HttpServletResponse response) throws IOException {
        writeXlsxHeader(response, "题库导出");
        importBiz.export(req, response.getOutputStream());
    }

    @Operation(summary = "下载题库导入模板")
    @GetMapping("/question/import/template")
    public void questionImportTemplate(HttpServletResponse response) throws IOException {
        writeXlsxHeader(response, "题库导入模板");
        importBiz.writeTemplate(response.getOutputStream());
    }

    @Operation(summary = "批量导入题目")
    @PostMapping("/question/import")
    public Result<AdminExamQuestionImportResp> questionImport(@RequestParam(value = "file", required = false) MultipartFile file) {
        return importBiz.importQuestions(file);
    }

    /**
     * 文件名用 RFC 5987 的 filename* 传，不能直接塞进 filename=。
     * 中文文件名在 header 里必须是 URL 编码的，否则浏览器存下来是一串乱码。
     * URLEncoder 会把空格编成 +，而 + 在这里不会被还原成空格，所以再换成 %20。
     */
    private static void writeXlsxHeader(HttpServletResponse response, String fileName) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encoded + ".xlsx");
    }

    // ===== 试卷 =====

    @Operation(summary = "试卷分页")
    @PostMapping("/paper/page")
    public Result<Page<ExamPaper>> paperPage(@RequestBody AdminExamPaperPageReq req) {
        return biz.paperPage(req);
    }

    @Operation(summary = "试卷新增(含组卷规则)")
    @PostMapping("/paper/save")
    public Result<String> paperSave(@RequestBody AdminExamPaperEditReq req) {
        return biz.paperSave(req);
    }

    @Operation(summary = "试卷修改(rules非空则全量替换)")
    @PutMapping("/paper/update")
    public Result<String> paperUpdate(@RequestBody AdminExamPaperEditReq req) {
        return biz.paperUpdate(req);
    }

    @Operation(summary = "试卷删除")
    @DeleteMapping("/paper/delete")
    public Result<String> paperDelete(@RequestParam Long id) {
        return biz.paperDelete(id);
    }

    @Operation(summary = "试卷详情(含规则)")
    @GetMapping("/paper/view")
    public Result<AdminExamPaperViewResp> paperView(@RequestParam Long id) {
        return biz.paperView(id);
    }

    // ===== 考试记录 =====

    @Operation(summary = "考试记录分页")
    @PostMapping("/record/page")
    public Result<Page<ExamRecord>> recordPage(@RequestBody AdminExamRecordPageReq req) {
        return biz.recordPage(req);
    }

    // ===== 课程指派 =====

    @Operation(summary = "批量指派课程")
    @PostMapping("/assign/batch")
    public Result<String> assignBatch(@RequestBody AdminExamAssignBatchReq req) {
        return biz.assignBatch(req);
    }

    @Operation(summary = "指派分页")
    @PostMapping("/assign/page")
    public Result<Page<UserCourseAssign>> assignPage(@RequestBody AdminExamAssignPageReq req) {
        return biz.assignPage(req);
    }

    @Operation(summary = "指派删除")
    @DeleteMapping("/assign/delete")
    public Result<String> assignDelete(@RequestParam Long id) {
        return biz.assignDelete(id);
    }
}
