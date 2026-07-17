package com.roncoo.education.course.service.admin;

import com.roncoo.education.common.base.page.Page;
import com.roncoo.education.common.core.base.Result;
import com.roncoo.education.course.dao.impl.mapper.entity.ExamPaper;
import com.roncoo.education.course.dao.impl.mapper.entity.ExamQuestion;
import com.roncoo.education.course.dao.impl.mapper.entity.ExamRecord;
import com.roncoo.education.course.dao.impl.mapper.entity.UserCourseAssign;
import com.roncoo.education.course.service.admin.biz.AdminExamBiz;
import com.roncoo.education.course.service.admin.req.*;
import com.roncoo.education.course.service.admin.resp.AdminExamPaperViewResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 考试管理接口（二开）
 */
@Tag(name = "admin-考试管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/course/admin/exam")
public class AdminExamController {

    private final AdminExamBiz biz;

    // ===== 题库 =====

    @Operation(summary = "题库分页")
    @PostMapping("/question/page")
    public Result<Page<ExamQuestion>> questionPage(@RequestBody AdminExamQuestionPageReq req) {
        return biz.questionPage(req);
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
