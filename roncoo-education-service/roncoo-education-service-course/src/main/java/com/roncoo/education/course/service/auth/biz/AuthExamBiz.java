package com.roncoo.education.course.service.auth.biz;

import cn.hutool.json.JSONUtil;
import com.roncoo.education.common.core.base.Result;
import com.roncoo.education.common.tools.IdWorker;
import com.roncoo.education.course.dao.impl.mapper.*;
import com.roncoo.education.course.dao.impl.mapper.entity.*;
import com.roncoo.education.course.service.admin.biz.AdminExamBiz;
import com.roncoo.education.course.service.auth.req.AuthExamStartReq;
import com.roncoo.education.course.service.auth.req.AuthExamSubmitReq;
import com.roncoo.education.course.service.auth.resp.AuthExamStartResp;
import com.roncoo.education.course.service.auth.resp.AuthExamSubmitResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 员工考试流程（二开）：开考（随机组卷+快照）、交卷（自动评分+补考资格）、我的任务/成绩
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthExamBiz {

    /** 交卷宽限（毫秒）：超过 时长+宽限 视为超时交卷 */
    private static final long GRACE_MILLIS = 60_000L;

    private final ExamQuestionMapper questionMapper;
    private final ExamPaperMapper paperMapper;
    private final ExamPaperRuleMapper ruleMapper;
    private final ExamRecordMapper recordMapper;
    private final UserCourseAssignMapper assignMapper;

    /**
     * 我的课程任务（必修/选修）
     */
    public Result<List<UserCourseAssign>> myAssignList(Long userId) {
        return Result.success(assignMapper.listByUserId(userId));
    }

    /**
     * 课程下可考的试卷
     */
    public Result<List<ExamPaper>> paperList(Long courseId) {
        return Result.success(paperMapper.listByCourseId(courseId));
    }

    /**
     * 我的考试成绩
     */
    public Result<List<ExamRecord>> myRecordList(Long userId) {
        return Result.success(recordMapper.listByUserId(userId));
    }

    /**
     * 开始考试：进行中未超时则续考（返回原题），否则按规则随机抽题生成新记录
     */
    public Result<AuthExamStartResp> start(Long userId, AuthExamStartReq req) {
        if (req.getPaperId() == null) {
            return Result.error("试卷不能为空");
        }
        ExamPaper paper = paperMapper.getById(req.getPaperId());
        if (paper == null || !Integer.valueOf(1).equals(paper.getStatusId())) {
            return Result.error("试卷不存在或已停用");
        }
        if (recordMapper.countPassed(userId, paper.getId()) > 0) {
            return Result.error("已通过考试，无需再考");
        }

        // 进行中的考试：未超时续考；已超时按 0 分超时交卷，消耗一次机会
        ExamRecord ongoing = recordMapper.getOngoing(userId, paper.getId());
        if (ongoing != null) {
            long deadline = ongoing.getStartTime().getTime() + paper.getDurationMinutes() * 60_000L + GRACE_MILLIS;
            if (System.currentTimeMillis() <= deadline) {
                List<ExamSnapshotQuestion> snapshot = JSONUtil.toList(ongoing.getQuestionsJson(), ExamSnapshotQuestion.class);
                return Result.success(buildStartResp(ongoing, paper, snapshot));
            }
            ongoing.setSubmitTime(new Date()).setScore(0).setIsPass(0).setExamStatus(3).setAnswersJson("[]");
            recordMapper.updateSubmit(ongoing);
        }

        int attempts = recordMapper.countSubmitted(userId, paper.getId());
        int maxAttempts = 1 + (paper.getRetakeLimit() == null ? 0 : paper.getRetakeLimit());
        if (attempts >= maxAttempts) {
            return Result.error("考试次数已用完（正考1次+补考" + (maxAttempts - 1) + "次）");
        }

        // 按规则随机抽题
        List<ExamPaperRule> rules = ruleMapper.listByPaperId(paper.getId());
        if (CollectionUtils.isEmpty(rules)) {
            return Result.error("试卷未配置组卷规则");
        }
        List<ExamSnapshotQuestion> snapshot = new ArrayList<>();
        Set<Long> picked = new HashSet<>();
        int seq = 1;
        for (ExamPaperRule rule : rules) {
            List<ExamQuestion> questions = questionMapper.randomPick(rule.getCategoryId(), rule.getQuestionType(), rule.getQuestionCount());
            int need = rule.getQuestionCount();
            int got = 0;
            for (ExamQuestion q : questions) {
                if (picked.contains(q.getId())) {
                    continue;
                }
                picked.add(q.getId());
                snapshot.add(new ExamSnapshotQuestion()
                        .setQuestionId(q.getId()).setQuestionType(q.getQuestionType())
                        .setQuestionTitle(q.getQuestionTitle()).setOptionsJson(q.getOptionsJson())
                        .setCorrectAnswer(q.getCorrectAnswer()).setAnalysis(q.getAnalysis())
                        .setScore(rule.getScorePerQuestion()).setSeq(seq++));
                got++;
            }
            if (got < need) {
                return Result.error("题库题目不足：规则要求" + need + "题，实际可抽" + got + "题");
            }
        }

        ExamRecord record = new ExamRecord()
                .setId(IdWorker.getId()).setStatusId(1)
                .setUserId(userId).setPaperId(paper.getId()).setCourseId(paper.getCourseId())
                .setAttemptNo(attempts + 1).setStartTime(new Date()).setExamStatus(1)
                .setQuestionsJson(JSONUtil.toJsonStr(snapshot));
        recordMapper.insert(record);

        return Result.success(buildStartResp(record, paper, snapshot));
    }

    private AuthExamStartResp buildStartResp(ExamRecord record, ExamPaper paper, List<ExamSnapshotQuestion> snapshot) {
        AuthExamStartResp resp = new AuthExamStartResp()
                .setRecordId(record.getId()).setPaperName(paper.getPaperName())
                .setDurationMinutes(paper.getDurationMinutes()).setTotalScore(paper.getTotalScore())
                .setPassScore(paper.getPassScore()).setAttemptNo(record.getAttemptNo())
                .setStartTime(record.getStartTime());
        List<AuthExamStartResp.QuestionItem> items = new ArrayList<>();
        for (ExamSnapshotQuestion q : snapshot) {
            // 注意：不下发 correctAnswer / analysis
            items.add(new AuthExamStartResp.QuestionItem()
                    .setQuestionId(q.getQuestionId()).setQuestionType(q.getQuestionType())
                    .setQuestionTitle(q.getQuestionTitle()).setOptionsJson(q.getOptionsJson())
                    .setScore(q.getScore()).setSeq(q.getSeq()));
        }
        resp.setQuestions(items);
        return resp;
    }

    /**
     * 交卷：自动评分、判定通过、更新指派状态、计算补考资格
     */
    public Result<AuthExamSubmitResp> submit(Long userId, AuthExamSubmitReq req) {
        if (req.getRecordId() == null) {
            return Result.error("考试记录不能为空");
        }
        ExamRecord record = recordMapper.getById(req.getRecordId());
        if (record == null || !record.getUserId().equals(userId)) {
            return Result.error("考试记录不存在");
        }
        if (!Integer.valueOf(1).equals(record.getExamStatus())) {
            return Result.error("该考试已交卷");
        }
        ExamPaper paper = paperMapper.getById(record.getPaperId());
        if (paper == null) {
            return Result.error("试卷不存在");
        }

        // 作答映射
        Map<Long, String> answerMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(req.getAnswers())) {
            for (AuthExamSubmitReq.AnswerItem item : req.getAnswers()) {
                if (item.getQuestionId() != null) {
                    answerMap.put(item.getQuestionId(), item.getAnswer());
                }
            }
        }

        // 评分
        List<ExamSnapshotQuestion> snapshot = JSONUtil.toList(record.getQuestionsJson(), ExamSnapshotQuestion.class);
        int score = 0;
        List<AuthExamSubmitResp.WrongItem> wrongList = new ArrayList<>();
        for (ExamSnapshotQuestion q : snapshot) {
            String yourAnswer = answerMap.get(q.getQuestionId());
            if (AdminExamBiz.normalizeAnswer(yourAnswer).equals(AdminExamBiz.normalizeAnswer(q.getCorrectAnswer()))
                    && cn.hutool.core.util.StrUtil.isNotBlank(yourAnswer)) {
                score += q.getScore() == null ? 0 : q.getScore();
            } else {
                wrongList.add(new AuthExamSubmitResp.WrongItem()
                        .setQuestionId(q.getQuestionId()).setQuestionTitle(q.getQuestionTitle())
                        .setYourAnswer(yourAnswer).setCorrectAnswer(q.getCorrectAnswer())
                        .setAnalysis(q.getAnalysis()));
            }
        }
        boolean isPass = score >= (paper.getPassScore() == null ? 80 : paper.getPassScore());

        // 超时判定
        long deadline = record.getStartTime().getTime() + paper.getDurationMinutes() * 60_000L + GRACE_MILLIS;
        int examStatus = System.currentTimeMillis() > deadline ? 3 : 2;

        record.setSubmitTime(new Date()).setScore(score).setIsPass(isPass ? 1 : 0)
                .setExamStatus(examStatus).setAnswersJson(JSONUtil.toJsonStr(req.getAnswers()));
        recordMapper.updateSubmit(record);

        // 通过则更新课程指派状态
        if (isPass && record.getCourseId() != null) {
            assignMapper.updateFinish(userId, record.getCourseId(), 3);
        }

        int maxAttempts = 1 + (paper.getRetakeLimit() == null ? 0 : paper.getRetakeLimit());
        boolean canRetake = !isPass && record.getAttemptNo() < maxAttempts;

        return Result.success(new AuthExamSubmitResp()
                .setScore(score).setTotalScore(paper.getTotalScore()).setPassScore(paper.getPassScore())
                .setIsPass(isPass).setCanRetake(canRetake).setAttemptNo(record.getAttemptNo())
                .setWrongList(wrongList));
    }
}
