package com.roncoo.education.course.service.admin.biz;

import com.roncoo.education.common.core.base.Result;
import com.roncoo.education.common.tools.IdWorker;
import com.roncoo.education.common.tools.XlsxUtil;
import com.roncoo.education.course.dao.impl.mapper.ExamQuestionMapper;
import com.roncoo.education.course.dao.impl.mapper.entity.Category;
import com.roncoo.education.course.dao.impl.mapper.entity.CategoryExample;
import com.roncoo.education.course.dao.impl.mapper.entity.Course;
import com.roncoo.education.course.dao.impl.mapper.entity.CourseChapter;
import com.roncoo.education.course.dao.impl.mapper.entity.CourseExample;
import com.roncoo.education.course.dao.impl.mapper.entity.ExamQuestion;
import com.roncoo.education.course.dao.CategoryDao;
import com.roncoo.education.course.dao.CourseChapterDao;
import com.roncoo.education.course.dao.CourseDao;
import com.roncoo.education.course.service.admin.req.AdminExamQuestionPageReq;
import com.roncoo.education.course.service.admin.resp.AdminExamQuestionImportResp;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * ADMIN-题库批量导入导出（二开）
 * <p>
 * 导出的文件就是导入模板：出题人从库里导出一批、在 Excel 里改完再导回来，
 * 不需要在两种格式之间转换。靠首列的 ID 区分新增和修改——
 * 有 ID 就更新那一道题，留空就是新题。
 * <p>
 * 分类、课程、章节在文件里都用<b>名称</b>而不是 ID：
 * 让人对着一串数字填表必然填错，而且导出的文件也没法看。
 * 代价是导入时要按名称反查，重名就无法判断该挂到哪一个，这种情况直接报错，
 * 不猜——猜错了题目会挂到别的课程下面，比报错难发现得多。
 *
 * @author 二开
 */
@Component
@RequiredArgsConstructor
public class AdminExamQuestionImportBiz {

    /** 单次导入行数上限，防止误传大文件把服务拖垮 */
    private static final int MAX_ROWS = 2000;

    /** 单次导出题数上限。题库上万时应该按课程分批导，一次拉全表没有意义 */
    private static final int MAX_EXPORT = 5000;

    /** 选项最多 6 个，与出题表单的 A~F 一致 */
    private static final int MAX_OPTIONS = 6;

    private static final String[] OPTION_KEYS = {"A", "B", "C", "D", "E", "F"};

    private static final int STATUS_NORMAL = 1;
    private static final int STATUS_DISABLED = 0;

    private static final int TYPE_SINGLE = 1;
    private static final int TYPE_MULTI = 2;
    private static final int TYPE_JUDGE = 3;

    /**
     * 列顺序即导出顺序，导入时逐列核对表头。
     * 顺序不能随意调整：已经导出到出题人手里的文件是按这个顺序填的。
     */
    private static final List<String> HEADERS = List.of(
            "ID", "题型", "题干",
            "选项A", "选项B", "选项C", "选项D", "选项E", "选项F",
            "正确答案", "解析", "难度", "所属模块", "所属课程", "所属章节", "状态");

    private static final int C_ID = 0;
    private static final int C_TYPE = 1;
    private static final int C_TITLE = 2;
    private static final int C_OPT_A = 3;
    private static final int C_ANSWER = 9;
    private static final int C_ANALYSIS = 10;
    private static final int C_DIFFICULTY = 11;
    private static final int C_CATEGORY = 12;
    private static final int C_COURSE = 13;
    private static final int C_CHAPTER = 14;
    private static final int C_STATUS = 15;

    private final ExamQuestionMapper questionMapper;
    private final CategoryDao categoryDao;
    private final CourseDao courseDao;
    private final CourseChapterDao chapterDao;

    // ==================== 导出 ====================

    /**
     * 按当前筛选条件导出题目。导出的文件可以直接改完再导入。
     */
    public void export(AdminExamQuestionPageReq req, OutputStream out) throws IOException {
        List<ExamQuestion> list = questionMapper.page(req.getCategoryId(), req.getCourseId(), req.getChapterId(),
                req.getQuestionType(), req.getKeyword(), 0, MAX_EXPORT);

        Dict dict = loadDict();
        List<List<String>> rows = new ArrayList<>();
        rows.add(HEADERS);
        for (ExamQuestion q : list) {
            rows.add(toRow(q, dict));
        }
        // 一道题都没有时只写表头，仍然是一份可填写的空模板，不至于导出个坏文件
        XlsxUtil.write(out, "题库", rows);
    }

    private List<String> toRow(ExamQuestion q, Dict dict) {
        List<String> row = new ArrayList<>(HEADERS.size());
        // ID 用文本原样写出。Excel 会把 19 位的雪花 ID 显示成科学计数并丢掉末尾精度，
        // 那样导回来就找不到原题了，所以 XlsxUtil 统一按 inlineStr 写
        row.add(q.getId() == null ? "" : String.valueOf(q.getId()));
        row.add(typeName(q.getQuestionType()));
        row.add(nvl(q.getQuestionTitle()));

        List<String> opts = parseOptionsJson(q.getOptionsJson());
        for (int i = 0; i < MAX_OPTIONS; i++) {
            row.add(i < opts.size() ? opts.get(i) : "");
        }

        row.add(answerToText(q.getCorrectAnswer(), q.getQuestionType()));
        row.add(nvl(q.getAnalysis()));
        row.add(difficultyName(q.getDifficulty()));
        row.add(q.getCategoryId() == null ? "" : nvl(dict.categoryNameById.get(q.getCategoryId())));
        row.add(q.getCourseId() == null ? "" : nvl(dict.courseNameById.get(q.getCourseId())));
        row.add(q.getChapterId() == null ? "" : nvl(dict.chapterNameById.get(q.getChapterId())));
        row.add(q.getStatusId() != null && q.getStatusId() == STATUS_DISABLED ? "禁用" : "正常");
        return row;
    }

    /**
     * 解析入库的选项 JSON，返回按 A、B、C…顺序排好的选项文本。
     * <p>
     * 手写解析而不是引 JSON 库：这里的结构固定为 [{"key":"A","value":"..."}]，
     * 且导出不能因为某道题的选项是脏数据就整批失败——解析不出来就当没有选项，
     * 让人在导出的文件里一眼看到这道题选项是空的，比抛异常有用。
     */
    static List<String> parseOptionsJson(String json) {
        List<String> result = new ArrayList<>();
        if (!StringUtils.hasText(json)) {
            return result;
        }
        try {
            Map<String, String> byKey = new HashMap<>();
            int i = 0;
            while (i < json.length()) {
                int objStart = json.indexOf('{', i);
                if (objStart < 0) {
                    break;
                }
                int objEnd = json.indexOf('}', objStart);
                if (objEnd < 0) {
                    break;
                }
                String obj = json.substring(objStart, objEnd);
                String key = jsonField(obj, "key");
                String value = jsonField(obj, "value");
                if (key != null) {
                    byKey.put(key.trim().toUpperCase(), value == null ? "" : value);
                }
                i = objEnd + 1;
            }
            for (String k : OPTION_KEYS) {
                if (byKey.containsKey(k)) {
                    result.add(byKey.get(k));
                }
            }
        } catch (Exception e) {
            return new ArrayList<>();
        }
        return result;
    }

    /** 从 {"key":"A","value":"甲"} 这样的片段里取一个字段，处理 \" 转义 */
    private static String jsonField(String obj, String field) {
        String needle = "\"" + field + "\"";
        int p = obj.indexOf(needle);
        if (p < 0) {
            return null;
        }
        int colon = obj.indexOf(':', p + needle.length());
        if (colon < 0) {
            return null;
        }
        int q1 = obj.indexOf('"', colon + 1);
        if (q1 < 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = q1 + 1; i < obj.length(); i++) {
            char c = obj.charAt(i);
            if (c == '\\' && i + 1 < obj.length()) {
                char n = obj.charAt(++i);
                switch (n) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    default -> sb.append(n);
                }
            } else if (c == '"') {
                return sb.toString();
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String toOptionsJson(List<String> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"key\":\"").append(OPTION_KEYS[i]).append("\",\"value\":\"")
                    .append(escapeJson(values.get(i))).append("\"}");
        }
        return sb.append(']').toString();
    }

    private static String escapeJson(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c >= 0x20) {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    // ==================== 模板 ====================

    /**
     * 空模板。第二行是填写说明，故意写成过不了校验的内容——
     * 员工导入那边曾经在示例行里放了一条合法数据，谁把模板原样传回来
     * 就把工号相同的真实员工覆盖了。示例只写在说明文字里，忘删也只是报一条错。
     */
    public void writeTemplate(OutputStream out) throws IOException {
        Dict dict = loadDict();
        String categoryNames = dict.categoryIdByName.keySet().stream().limit(8)
                .reduce((a, b) -> a + "/" + b).orElse("（暂无分类）");

        List<List<String>> rows = new ArrayList<>();
        rows.add(HEADERS);
        rows.add(List.of(
                "新增题目留空，修改已有题目才填",
                "必填，单选/多选/判断",
                "必填，题干正文",
                "单选多选必填，判断题留空则自动填「正确」",
                "单选多选必填，判断题留空则自动填「错误」",
                "可选", "可选", "可选", "可选",
                "必填，单选如 A，多选如 A,C，判断填 正确 或 错误",
                "可选，答错时展示给员工",
                "可选，易/中/难，留空按易",
                "可选，限：" + categoryNames,
                "可选，课程名称，须与课程列表一致",
                "可选，章节名称，填了章节必须先填课程",
                "可选，正常/禁用，留空按正常"));
        XlsxUtil.write(out, "题库导入", rows);
    }

    // ==================== 导入 ====================

    public Result<AdminExamQuestionImportResp> importQuestions(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("请选择要导入的文件");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".xlsx")) {
            return Result.error("只支持 .xlsx 文件。若手上是老的 .xls，请用 Excel 另存为 .xlsx 再导入");
        }

        List<List<String>> raw;
        try {
            raw = XlsxUtil.read(file.getInputStream());
        } catch (Exception e) {
            return Result.error("文件解析失败，请确认是用模板或导出文件另存的 Excel：" + e.getMessage());
        }
        if (raw.size() <= 1) {
            return Result.error("文件里没有数据行");
        }
        String headerErr = checkHeader(raw.get(0));
        if (headerErr != null) {
            return Result.error(headerErr);
        }
        List<List<String>> rows = raw.subList(1, raw.size());
        if (rows.size() > MAX_ROWS) {
            return Result.error("单次最多导入 " + MAX_ROWS + " 行，当前 " + rows.size() + " 行。请按课程分批导入");
        }

        Dict dict = loadDict();
        // 库里已有的题干，用来挡住「同一个文件导了两遍」——
        // 那种情况新增的行没有 ID，会实打实地再建一遍，出题人很难发现
        Map<String, Long> existTitles = loadExistTitles();

        AdminExamQuestionImportResp resp = new AdminExamQuestionImportResp();
        List<AdminExamQuestionImportResp.RowError> errors = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();
        Set<String> seenTitles = new HashSet<>();

        int dataCount = 0;
        for (int i = 0; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            // 表头占第 1 行，数据从第 2 行开始，与打开文件看到的行号对齐
            int rowNum = i + 2;
            if (isBlankRow(row)) {
                // Excel 常在末尾留一堆空行，报成「题干为空」会刷屏、掩盖真正的问题行
                continue;
            }
            dataCount++;
            try {
                String err = handleRow(row, dict, existTitles, seenIds, seenTitles, resp);
                if (err != null) {
                    errors.add(newError(rowNum, row, err));
                }
            } catch (Exception e) {
                // 单行异常不中断整批，否则前面已成功的行会让人以为全没导进去
                errors.add(newError(rowNum, row, "处理异常：" + e.getMessage()));
            }
        }

        resp.setTotalCount(dataCount);
        resp.setErrors(errors);
        resp.setFailedCount(errors.size());
        return Result.success(resp);
    }

    /**
     * 处理一行。返回 null 表示成功，否则返回失败原因。
     */
    private String handleRow(List<String> row, Dict dict, Map<String, Long> existTitles,
                             Set<Long> seenIds, Set<String> seenTitles, AdminExamQuestionImportResp resp) {
        String title = trim(cell(row, C_TITLE));
        if (!StringUtils.hasText(title)) {
            return "题干为空";
        }

        // ---- ID：决定新增还是更新 ----
        Long id = null;
        String idText = normalizeNumber(trim(cell(row, C_ID)));
        if (StringUtils.hasText(idText)) {
            try {
                id = Long.valueOf(idText);
            } catch (NumberFormatException e) {
                return "ID「" + idText + "」不是数字。ID 由导出文件带出，不要手工编造，新增题目请留空";
            }
            if (!seenIds.add(id)) {
                return "ID「" + id + "」在本文件中出现了多次";
            }
            if (questionMapper.getById(id) == null) {
                // 不静默改成新增：ID 对不上多半是手滑改了这一列，
                // 悄悄插一条新题会让出题人以为改动生效了，实际上原题没变
                return "找不到 ID 为「" + id + "」的题目，可能已被删除。若是新题请把 ID 列清空";
            }
        }

        // ---- 题型 ----
        Integer type = parseType(trim(cell(row, C_TYPE)));
        if (type == null) {
            return "题型「" + nvl(trim(cell(row, C_TYPE))) + "」无法识别，请填 单选 / 多选 / 判断";
        }

        // ---- 选项 ----
        List<String> options = new ArrayList<>();
        for (int i = 0; i < MAX_OPTIONS; i++) {
            String v = trim(cell(row, C_OPT_A + i));
            if (StringUtils.hasText(v)) {
                if (options.size() < i) {
                    // 中间空了一列，比如填了 A、C 没填 B。若照原样收下，
                    // C 会被当成 B 存进去，答案写 C 就再也对不上了
                    return "选项" + OPTION_KEYS[i] + "填了，但前面的选项" + OPTION_KEYS[options.size()] + "是空的，请从 A 开始连续填写";
                }
                options.add(v);
            }
        }
        if (type == TYPE_JUDGE && options.isEmpty()) {
            // 判断题的选项永远是这两个，让出题人重复填没有意义
            options = new ArrayList<>(List.of("正确", "错误"));
        }
        if (options.size() < 2) {
            return "至少要填两个选项（当前 " + options.size() + " 个）";
        }
        if (type == TYPE_JUDGE && options.size() != 2) {
            return "判断题只能有两个选项，当前填了 " + options.size() + " 个";
        }

        // ---- 正确答案 ----
        String answerRaw = trim(cell(row, C_ANSWER));
        if (!StringUtils.hasText(answerRaw)) {
            return "正确答案为空";
        }
        String answer = parseAnswer(answerRaw, type, options.size());
        if (answer == null) {
            return "正确答案「" + answerRaw + "」无法识别。" + answerHint(type, options.size());
        }
        long picked = answer.chars().filter(c -> c == ',').count() + 1;
        if (type == TYPE_MULTI && picked < 2) {
            return "多选题的正确答案至少要选两项，当前只选了「" + answer + "」。只有一个答案请把题型改成单选";
        }
        if (type != TYPE_MULTI && picked > 1) {
            return typeName(type) + "题的正确答案只能有一个，当前是「" + answer + "」";
        }

        // ---- 模块 / 课程 / 章节 ----
        Long categoryId = null;
        String categoryName = trim(cell(row, C_CATEGORY));
        if (StringUtils.hasText(categoryName)) {
            if (dict.ambiguousCategories.contains(categoryName)) {
                return "有多个分类都叫「" + categoryName + "」，无法判断归属。请先在课程分类里改成不重名";
            }
            categoryId = dict.categoryIdByName.get(categoryName);
            if (categoryId == null) {
                return "模块「" + categoryName + "」不存在，请先在课程分类里添加";
            }
        }

        Long courseId = null;
        String courseName = trim(cell(row, C_COURSE));
        if (StringUtils.hasText(courseName)) {
            if (dict.ambiguousCourses.contains(courseName)) {
                return "有多门课程都叫「" + courseName + "」，无法判断归属。请先把课程名称改成不重名";
            }
            courseId = dict.courseIdByName.get(courseName);
            if (courseId == null) {
                return "课程「" + courseName + "」不存在，请先在课程列表里添加";
            }
        }

        Long chapterId = null;
        String chapterName = trim(cell(row, C_CHAPTER));
        if (StringUtils.hasText(chapterName)) {
            if (courseId == null) {
                return "填了章节「" + chapterName + "」却没填课程。章节名在不同课程里会重复，必须先指明课程";
            }
            chapterId = dict.chapterIdByCourseAndName.get(key(courseId, chapterName));
            if (chapterId == null) {
                return "课程「" + courseName + "」下没有名为「" + chapterName + "」的章节";
            }
        }

        // ---- 重复题干 ----
        // 同一门课下题干完全一样，基本可以断定是重复导入或复制粘贴漏改。
        // 判断依据带上课程：不同课程出现同一道通用题是正常的
        String dupKey = key(courseId, title);
        if (!seenTitles.add(dupKey)) {
            return "题干在本文件中重复出现（同一门课下）";
        }
        Long existId = existTitles.get(dupKey);
        if (existId != null && !existId.equals(id)) {
            return "题库里已有题干相同的题（ID " + existId + "）。若想修改它，请把这个 ID 填到 ID 列；"
                    + "若确实要出一道新题，请改一下题干";
        }

        // ---- 落库 ----
        Integer difficulty = parseDifficulty(trim(cell(row, C_DIFFICULTY)));
        if (difficulty == null) {
            return "难度「" + trim(cell(row, C_DIFFICULTY)) + "」无法识别，请填 易 / 中 / 难，或留空";
        }
        Integer statusId = parseStatus(trim(cell(row, C_STATUS)));
        if (statusId == null) {
            return "状态「" + trim(cell(row, C_STATUS)) + "」无法识别，请填 正常 / 禁用，或留空";
        }

        ExamQuestion record = new ExamQuestion()
                .setStatusId(statusId)
                .setSort(0)
                .setCategoryId(categoryId)
                .setCourseId(courseId)
                .setChapterId(chapterId)
                .setQuestionType(type)
                .setQuestionTitle(title)
                .setOptionsJson(toOptionsJson(options))
                .setCorrectAnswer(answer)
                // 空解析存空串而不是 null：库里 null 和 "" 在导出时都显示为空，
                // 用空串可以让「把解析删掉再导回来」真的清掉旧解析
                .setAnalysis(nvl(trim(cell(row, C_ANALYSIS))))
                .setDifficulty(difficulty);

        if (id == null) {
            record.setId(IdWorker.getId());
            if (questionMapper.insert(record) <= 0) {
                return "新增失败";
            }
            // 本次新建的题也要进重复表，避免同一个文件里后面还有一模一样的题干
            existTitles.put(dupKey, record.getId());
            resp.setCreatedCount(resp.getCreatedCount() + 1);
        } else {
            record.setId(id);
            // 走 updateAllById 而不是 updateById：导入的语义是「文件里是什么就存什么」，
            // 清空了某一列就该真的清掉，详见 ExamQuestionMapper.updateAllById 的说明
            if (questionMapper.updateAllById(record) <= 0) {
                return "更新失败";
            }
            resp.setUpdatedCount(resp.getUpdatedCount() + 1);
        }
        return null;
    }

    // ==================== 字典 ====================

    /** 分类、课程、章节的名称与 ID 互查表，避免逐行查库 */
    private static final class Dict {
        final Map<Long, String> categoryNameById = new HashMap<>();
        final Map<String, Long> categoryIdByName = new HashMap<>();
        final Set<String> ambiguousCategories = new HashSet<>();

        final Map<Long, String> courseNameById = new HashMap<>();
        final Map<String, Long> courseIdByName = new HashMap<>();
        final Set<String> ambiguousCourses = new HashSet<>();

        final Map<Long, String> chapterNameById = new HashMap<>();
        /** key = courseId + '\0' + 章节名。章节名只在同一门课内唯一 */
        final Map<String, Long> chapterIdByCourseAndName = new HashMap<>();
    }

    private Dict loadDict() {
        Dict d = new Dict();

        CategoryExample ce = new CategoryExample();
        for (Category c : categoryDao.listByExample(ce)) {
            String name = trim(c.getCategoryName());
            d.categoryNameById.put(c.getId(), name);
            if (StringUtils.hasText(name)) {
                // 重名的分类记下来，导入时直接报错而不是随便挑一个
                if (d.categoryIdByName.put(name, c.getId()) != null) {
                    d.ambiguousCategories.add(name);
                }
            }
        }

        CourseExample coe = new CourseExample();
        List<Course> courses = courseDao.listByExample(coe);
        for (Course c : courses) {
            String name = trim(c.getCourseName());
            d.courseNameById.put(c.getId(), name);
            if (StringUtils.hasText(name)) {
                if (d.courseIdByName.put(name, c.getId()) != null) {
                    d.ambiguousCourses.add(name);
                }
            }
        }

        for (Course c : courses) {
            for (CourseChapter ch : chapterDao.listByCourseId(c.getId())) {
                String name = trim(ch.getChapterName());
                d.chapterNameById.put(ch.getId(), name);
                if (StringUtils.hasText(name)) {
                    d.chapterIdByCourseAndName.put(key(c.getId(), name), ch.getId());
                }
            }
        }
        return d;
    }

    /** key = courseId + '\0' + 题干 */
    private Map<String, Long> loadExistTitles() {
        Map<String, Long> map = new HashMap<>();
        for (ExamQuestion q : questionMapper.listTitles()) {
            String title = trim(q.getQuestionTitle());
            if (StringUtils.hasText(title)) {
                map.putIfAbsent(key(q.getCourseId(), title), q.getId());
            }
        }
        return map;
    }

    // ==================== 取值与转换 ====================

    private String checkHeader(List<String> header) {
        for (int i = 0; i < HEADERS.size(); i++) {
            String expect = HEADERS.get(i);
            String actual = trim(cell(header, i));
            if (!expect.equals(actual)) {
                return "表头第 " + (i + 1) + " 列应为「" + expect + "」，实际是「" + nvl(actual)
                        + "」。请先下载导入模板，或直接导出现有题目再改";
            }
        }
        return null;
    }

    private static boolean isBlankRow(List<String> row) {
        for (String c : row) {
            if (StringUtils.hasText(c)) {
                return false;
            }
        }
        return true;
    }

    private static String cell(List<String> row, int idx) {
        return row != null && idx < row.size() ? row.get(idx) : null;
    }

    private AdminExamQuestionImportResp.RowError newError(int rowNum, List<String> row, String reason) {
        String title = nvl(trim(cell(row, C_TITLE)));
        if (title.length() > 40) {
            title = title.substring(0, 40) + "…";
        }
        return new AdminExamQuestionImportResp.RowError()
                .setRowNum(rowNum)
                .setQuestionTitle(title)
                .setReason(reason);
    }

    static Integer parseType(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String s = normalizeNumber(raw);
        return switch (s) {
            case "单选", "单选题", "1" -> TYPE_SINGLE;
            case "多选", "多选题", "2" -> TYPE_MULTI;
            case "判断", "判断题", "3" -> TYPE_JUDGE;
            default -> null;
        };
    }

    private static String typeName(Integer type) {
        if (type == null) {
            return "单选";
        }
        return switch (type) {
            case TYPE_MULTI -> "多选";
            case TYPE_JUDGE -> "判断";
            default -> "单选";
        };
    }

    /** 难度留空按「易」，不报错——大多数题不需要标难度 */
    static Integer parseDifficulty(String raw) {
        if (!StringUtils.hasText(raw)) {
            return 1;
        }
        return switch (normalizeNumber(raw)) {
            case "易", "简单", "1" -> 1;
            case "中", "中等", "2" -> 2;
            case "难", "困难", "3" -> 3;
            default -> null;
        };
    }

    private static String difficultyName(Integer d) {
        if (d == null) {
            return "易";
        }
        return switch (d) {
            case 2 -> "中";
            case 3 -> "难";
            default -> "易";
        };
    }

    static Integer parseStatus(String raw) {
        if (!StringUtils.hasText(raw)) {
            return STATUS_NORMAL;
        }
        return switch (normalizeNumber(raw)) {
            case "正常", "启用", "1" -> STATUS_NORMAL;
            case "禁用", "停用", "0" -> STATUS_DISABLED;
            default -> null;
        };
    }

    /**
     * 解析正确答案，返回去重排序后的「A」或「A,C」。无法识别返回 null。
     * <p>
     * 判断题额外认「正确/错误」这类写法：导出时判断题的答案就是写成中文的，
     * 出题人也更习惯这么填，只认 A/B 的话导出的文件反而导不回来。
     */
    static String parseAnswer(String raw, int type, int optionCount) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String s = raw.trim().replace("，", ",").replace("、", ",").replace(" ", "").toUpperCase();

        if (type == TYPE_JUDGE) {
            switch (s) {
                case "正确", "对", "是", "T", "TRUE", "√", "Y", "YES" -> {
                    return "A";
                }
                case "错误", "错", "否", "F", "FALSE", "×", "X", "N", "NO" -> {
                    return "B";
                }
                default -> {
                    // 落到下面按字母解析，判断题填 A/B 同样合法
                }
            }
        }

        // TreeSet 顺带完成去重和排序，与 AdminExamBiz.normalizeAnswer 存进去的形式保持一致，
        // 否则「C,A」和「A,C」在库里会是两种写法，判分时对不上
        Set<String> picked = new TreeSet<>();
        for (String part : s.split(",")) {
            if (part.isEmpty()) {
                continue;
            }
            if (part.length() != 1) {
                return null;
            }
            int idx = part.charAt(0) - 'A';
            if (idx < 0 || idx >= optionCount) {
                // 答案指向了没填内容的选项。这是最容易出的错：
                // 选项只填了 A、B，答案却写 C，考试时这题谁都答不对
                return null;
            }
            picked.add(part);
        }
        if (picked.isEmpty()) {
            return null;
        }
        return String.join(",", picked);
    }

    private static String answerHint(int type, int optionCount) {
        String range = "A~" + OPTION_KEYS[Math.min(optionCount, MAX_OPTIONS) - 1];
        if (type == TYPE_JUDGE) {
            return "判断题请填「正确」或「错误」";
        }
        if (type == TYPE_MULTI) {
            return "多选题请填 " + range + " 之间的字母，用逗号分隔，如 A,C";
        }
        return "单选题请填 " + range + " 之间的一个字母";
    }

    /** 导出时把库里的 A/B 还原成判断题更好读的中文 */
    private static String answerToText(String answer, Integer type) {
        if (!StringUtils.hasText(answer)) {
            return "";
        }
        if (type != null && type == TYPE_JUDGE) {
            if ("A".equals(answer)) {
                return "正确";
            }
            if ("B".equals(answer)) {
                return "错误";
            }
        }
        return answer;
    }

    /**
     * Excel 把纯数字单元格读出来常带小数尾巴（「1」变「1.0」、ID 变「1.23E+18」）。
     * 尾巴是 .0 的去掉即可；科学计数还原不回来，原样返回让调用方报错。
     */
    static String normalizeNumber(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.matches("^\\d+\\.0+$")) {
            return s.substring(0, s.indexOf('.'));
        }
        return s;
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    /**
     * 复合键：课程 ID + 分隔符 + 名称。
     * <p>
     * 章节名、题干都只在同一门课内才唯一，单用名称做键会串课。
     * 分隔符用 \0 而不是空格或短横线：名称里可能带这些字符，
     * 那样 courseId=1 的「2 章」会和 courseId=12 的「章」撞成同一个键。
     */
    private static String key(Long courseId, String name) {
        return (courseId == null ? "" : courseId) + "\0" + name;
    }
}
