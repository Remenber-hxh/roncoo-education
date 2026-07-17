package com.roncoo.education.course.service.admin.biz;

import cn.hutool.core.date.DateUtil;
import com.roncoo.education.common.base.page.Page;
import com.roncoo.education.common.base.page.PageUtil;
import com.roncoo.education.common.core.base.Result;
import com.roncoo.education.common.tools.IdWorker;
import com.roncoo.education.course.dao.impl.mapper.*;
import com.roncoo.education.course.dao.impl.mapper.entity.*;
import com.roncoo.education.course.service.admin.req.*;
import com.roncoo.education.course.service.admin.resp.AdminExamPaperViewResp;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 考试管理（二开）：题库、试卷、组卷规则、考试记录、课程指派
 */
@Component
@RequiredArgsConstructor
public class AdminExamBiz {

    private final ExamQuestionMapper questionMapper;
    private final ExamPaperMapper paperMapper;
    private final ExamPaperRuleMapper ruleMapper;
    private final ExamRecordMapper recordMapper;
    private final UserCourseAssignMapper assignMapper;

    // ==================== 题库 ====================

    public Result<Page<ExamQuestion>> questionPage(AdminExamQuestionPageReq req) {
        int count = questionMapper.pageCount(req.getCategoryId(), req.getQuestionType(), req.getKeyword());
        int pageSize = PageUtil.checkPageSize(req.getPageSize());
        int pageCurrent = PageUtil.checkPageCurrent(count, pageSize, req.getPageCurrent());
        int totalPage = PageUtil.countTotalPage(count, pageSize);
        List<ExamQuestion> list = questionMapper.page(req.getCategoryId(), req.getQuestionType(), req.getKeyword(),
                PageUtil.countOffset(pageCurrent, pageSize), pageSize);
        return Result.success(new Page<>(count, totalPage, pageCurrent, pageSize, list));
    }

    public Result<String> questionSave(AdminExamQuestionEditReq req) {
        if (!StringUtils.hasText(req.getQuestionTitle()) || !StringUtils.hasText(req.getCorrectAnswer()) || req.getQuestionType() == null) {
            return Result.error("题干、题型、正确答案不能为空");
        }
        ExamQuestion record = new ExamQuestion()
                .setId(IdWorker.getId()).setStatusId(req.getStatusId() == null ? 1 : req.getStatusId())
                .setSort(req.getSort() == null ? 0 : req.getSort())
                .setCategoryId(req.getCategoryId()).setCourseId(req.getCourseId())
                .setQuestionType(req.getQuestionType()).setQuestionTitle(req.getQuestionTitle())
                .setOptionsJson(req.getOptionsJson()).setCorrectAnswer(normalizeAnswer(req.getCorrectAnswer()))
                .setAnalysis(req.getAnalysis()).setDifficulty(req.getDifficulty() == null ? 1 : req.getDifficulty());
        questionMapper.insert(record);
        return Result.success("保存成功");
    }

    public Result<String> questionUpdate(AdminExamQuestionEditReq req) {
        if (req.getId() == null) {
            return Result.error("id不能为空");
        }
        ExamQuestion record = new ExamQuestion()
                .setId(req.getId()).setStatusId(req.getStatusId()).setSort(req.getSort())
                .setCategoryId(req.getCategoryId()).setCourseId(req.getCourseId())
                .setQuestionType(req.getQuestionType()).setQuestionTitle(req.getQuestionTitle())
                .setOptionsJson(req.getOptionsJson())
                .setCorrectAnswer(req.getCorrectAnswer() == null ? null : normalizeAnswer(req.getCorrectAnswer()))
                .setAnalysis(req.getAnalysis()).setDifficulty(req.getDifficulty());
        questionMapper.updateById(record);
        return Result.success("修改成功");
    }

    public Result<String> questionDelete(Long id) {
        questionMapper.deleteById(id);
        return Result.success("删除成功");
    }

    public Result<ExamQuestion> questionView(Long id) {
        return Result.success(questionMapper.getById(id));
    }

    // ==================== 试卷 ====================

    public Result<Page<ExamPaper>> paperPage(AdminExamPaperPageReq req) {
        int count = paperMapper.pageCount(req.getCourseId(), req.getKeyword());
        int pageSize = PageUtil.checkPageSize(req.getPageSize());
        int pageCurrent = PageUtil.checkPageCurrent(count, pageSize, req.getPageCurrent());
        int totalPage = PageUtil.countTotalPage(count, pageSize);
        List<ExamPaper> list = paperMapper.page(req.getCourseId(), req.getKeyword(),
                PageUtil.countOffset(pageCurrent, pageSize), pageSize);
        return Result.success(new Page<>(count, totalPage, pageCurrent, pageSize, list));
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<String> paperSave(AdminExamPaperEditReq req) {
        if (!StringUtils.hasText(req.getPaperName())) {
            return Result.error("试卷名称不能为空");
        }
        Long paperId = IdWorker.getId();
        ExamPaper paper = new ExamPaper()
                .setId(paperId).setStatusId(req.getStatusId() == null ? 1 : req.getStatusId())
                .setSort(req.getSort() == null ? 0 : req.getSort())
                .setCourseId(req.getCourseId()).setPaperName(req.getPaperName())
                .setTotalScore(req.getTotalScore() == null ? 100 : req.getTotalScore())
                .setPassScore(req.getPassScore() == null ? 80 : req.getPassScore())
                .setDurationMinutes(req.getDurationMinutes() == null ? 60 : req.getDurationMinutes())
                .setRetakeLimit(req.getRetakeLimit() == null ? 1 : req.getRetakeLimit())
                .setNeedFinishCourse(req.getNeedFinishCourse() == null ? 0 : req.getNeedFinishCourse());
        paperMapper.insert(paper);
        saveRules(paperId, req.getRules());
        return Result.success(String.valueOf(paperId));
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<String> paperUpdate(AdminExamPaperEditReq req) {
        if (req.getId() == null) {
            return Result.error("id不能为空");
        }
        ExamPaper paper = new ExamPaper()
                .setId(req.getId()).setStatusId(req.getStatusId()).setSort(req.getSort())
                .setCourseId(req.getCourseId()).setPaperName(req.getPaperName())
                .setTotalScore(req.getTotalScore()).setPassScore(req.getPassScore())
                .setDurationMinutes(req.getDurationMinutes()).setRetakeLimit(req.getRetakeLimit())
                .setNeedFinishCourse(req.getNeedFinishCourse());
        paperMapper.updateById(paper);
        if (!CollectionUtils.isEmpty(req.getRules())) {
            ruleMapper.deleteByPaperId(req.getId());
            saveRules(req.getId(), req.getRules());
        }
        return Result.success("修改成功");
    }

    private void saveRules(Long paperId, List<AdminExamPaperEditReq.RuleItem> rules) {
        if (CollectionUtils.isEmpty(rules)) {
            return;
        }
        int sort = 1;
        for (AdminExamPaperEditReq.RuleItem item : rules) {
            ExamPaperRule rule = new ExamPaperRule()
                    .setId(IdWorker.getId()).setStatusId(1).setSort(sort++)
                    .setPaperId(paperId).setCategoryId(item.getCategoryId())
                    .setQuestionType(item.getQuestionType())
                    .setQuestionCount(item.getQuestionCount() == null ? 10 : item.getQuestionCount())
                    .setScorePerQuestion(item.getScorePerQuestion() == null ? 4 : item.getScorePerQuestion());
            ruleMapper.insert(rule);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<String> paperDelete(Long id) {
        paperMapper.deleteById(id);
        ruleMapper.deleteByPaperId(id);
        return Result.success("删除成功");
    }

    public Result<AdminExamPaperViewResp> paperView(Long id) {
        ExamPaper paper = paperMapper.getById(id);
        if (paper == null) {
            return Result.error("试卷不存在");
        }
        return Result.success(new AdminExamPaperViewResp().setPaper(paper).setRules(ruleMapper.listByPaperId(id)));
    }

    // ==================== 考试记录 ====================

    public Result<Page<ExamRecord>> recordPage(AdminExamRecordPageReq req) {
        int count = recordMapper.pageCount(req.getUserId(), req.getPaperId(), req.getIsPass());
        int pageSize = PageUtil.checkPageSize(req.getPageSize());
        int pageCurrent = PageUtil.checkPageCurrent(count, pageSize, req.getPageCurrent());
        int totalPage = PageUtil.countTotalPage(count, pageSize);
        List<ExamRecord> list = recordMapper.page(req.getUserId(), req.getPaperId(), req.getIsPass(),
                PageUtil.countOffset(pageCurrent, pageSize), pageSize);
        return Result.success(new Page<>(count, totalPage, pageCurrent, pageSize, list));
    }

    // ==================== 课程指派 ====================

    public Result<String> assignBatch(AdminExamAssignBatchReq req) {
        if (req.getCourseId() == null || CollectionUtils.isEmpty(req.getUserIds())) {
            return Result.error("课程和用户不能为空");
        }
        int added = 0;
        for (Long userId : req.getUserIds()) {
            if (assignMapper.getByUserAndCourse(userId, req.getCourseId()) != null) {
                continue;
            }
            UserCourseAssign assign = new UserCourseAssign()
                    .setId(IdWorker.getId()).setStatusId(1)
                    .setUserId(userId).setCourseId(req.getCourseId())
                    .setAssignType(req.getAssignType() == null ? 1 : req.getAssignType())
                    .setFinishStatus(0);
            if (StringUtils.hasText(req.getDeadline())) {
                assign.setDeadline(DateUtil.parse(req.getDeadline(), "yyyy-MM-dd"));
            }
            assignMapper.insert(assign);
            added++;
        }
        return Result.success("指派成功 " + added + " 人");
    }

    public Result<Page<UserCourseAssign>> assignPage(AdminExamAssignPageReq req) {
        int count = assignMapper.pageCount(req.getCourseId(), req.getUserId(), req.getFinishStatus());
        int pageSize = PageUtil.checkPageSize(req.getPageSize());
        int pageCurrent = PageUtil.checkPageCurrent(count, pageSize, req.getPageCurrent());
        int totalPage = PageUtil.countTotalPage(count, pageSize);
        List<UserCourseAssign> list = assignMapper.page(req.getCourseId(), req.getUserId(), req.getFinishStatus(),
                PageUtil.countOffset(pageCurrent, pageSize), pageSize);
        return Result.success(new Page<>(count, totalPage, pageCurrent, pageSize, list));
    }

    public Result<String> assignDelete(Long id) {
        assignMapper.deleteById(id);
        return Result.success("删除成功");
    }

    /**
     * 答案标准化：去空格、大写、多选按字母排序（A,C 与 C,A 等价）
     */
    public static String normalizeAnswer(String answer) {
        if (answer == null) {
            return "";
        }
        String[] parts = answer.toUpperCase().replace("，", ",").replace(" ", "").split(",");
        java.util.Arrays.sort(parts);
        return String.join(",", parts);
    }
}
