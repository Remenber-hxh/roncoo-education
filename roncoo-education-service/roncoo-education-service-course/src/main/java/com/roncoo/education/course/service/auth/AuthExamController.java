package com.roncoo.education.course.service.auth;

import com.roncoo.education.common.base.ThreadContext;
import com.roncoo.education.common.core.base.Result;
import com.roncoo.education.course.dao.impl.mapper.entity.ExamPaper;
import com.roncoo.education.course.dao.impl.mapper.entity.ExamRecord;
import com.roncoo.education.course.dao.impl.mapper.entity.UserCourseAssign;
import com.roncoo.education.course.service.auth.biz.AuthExamBiz;
import com.roncoo.education.course.service.auth.req.AuthExamStartReq;
import com.roncoo.education.course.service.auth.req.AuthExamSubmitReq;
import com.roncoo.education.course.service.auth.resp.AuthExamStartResp;
import com.roncoo.education.course.service.auth.resp.AuthExamSubmitResp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 员工考试接口（二开）
 */
@Tag(name = "auth-在线考试")
@RestController
@RequiredArgsConstructor
@RequestMapping("/course/auth/exam")
public class AuthExamController {

    private final AuthExamBiz biz;

    @Operation(summary = "我的课程任务(必修/选修)")
    @GetMapping("/my/assign/list")
    public Result<List<UserCourseAssign>> myAssignList() {
        return biz.myAssignList(ThreadContext.userId());
    }

    @Operation(summary = "课程下可考试卷")
    @GetMapping("/paper/list")
    public Result<List<ExamPaper>> paperList(@RequestParam Long courseId) {
        return biz.paperList(courseId);
    }

    @Operation(summary = "开始考试(随机组卷,不含答案)")
    @PostMapping("/start")
    public Result<AuthExamStartResp> start(@RequestBody AuthExamStartReq req) {
        return biz.start(ThreadContext.userId(), req);
    }

    @Operation(summary = "交卷(自动评分)")
    @PostMapping("/submit")
    public Result<AuthExamSubmitResp> submit(@RequestBody AuthExamSubmitReq req) {
        return biz.submit(ThreadContext.userId(), req);
    }

    @Operation(summary = "我的考试成绩")
    @GetMapping("/my/record/list")
    public Result<List<ExamRecord>> myRecordList() {
        return biz.myRecordList(ThreadContext.userId());
    }
}
