package com.roncoo.education.user.service.admin.biz;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.roncoo.education.common.base.BaseBiz;
import com.roncoo.education.common.core.base.Result;
import com.roncoo.education.user.dao.ProjectGroupDao;
import com.roncoo.education.user.dao.TeamDao;
import com.roncoo.education.user.dao.UsersDao;
import com.roncoo.education.user.dao.impl.mapper.entity.ProjectGroup;
import com.roncoo.education.user.dao.impl.mapper.entity.Team;
import com.roncoo.education.user.dao.impl.mapper.entity.Users;
import com.roncoo.education.user.dao.impl.mapper.entity.UsersExample;
import com.roncoo.education.user.service.admin.resp.AdminUsersImportResp;
import com.roncoo.education.common.tools.XlsxUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * ADMIN-员工批量导入
 *
 * @author 二开
 */
@Component
@RequiredArgsConstructor
public class AdminUsersImportBiz extends BaseBiz {

    /** 工号：纯数字。现有工号是 1~98 的流水号且不连续，故不校验位数与连续性 */
    private static final Pattern EMP_NO = Pattern.compile("^\\d+$");

    /** 手机号即登录账号，必须是标准 11 位 */
    private static final Pattern MOBILE = Pattern.compile("^1[3-9]\\d{9}$");

    /** Excel 日期序列号的起点。1900 年 1 月 0 日，即 1899-12-30 */
    private static final LocalDate EXCEL_EPOCH = LocalDate.of(1899, 12, 30);

    /** 单次导入的行数上限，防止误传大文件把服务拖垮 */
    private static final int MAX_ROWS = 2000;

    private static final int STATUS_NORMAL = 1;

    @NotNull
    private final UsersDao usersDao;

    @NotNull
    private final TeamDao teamDao;

    @NotNull
    private final ProjectGroupDao projectGroupDao;

    /** 模板列顺序，与《人员信息登记表》对齐，HR 可直接复制粘贴 */
    private static final List<String> HEADERS = List.of("工号", "姓名", "手机号", "班组", "项目组", "岗位职务", "入职日期");

    private static final int C_EMP_NO = 0;
    private static final int C_NICKNAME = 1;
    private static final int C_MOBILE = 2;
    private static final int C_TEAM = 3;
    private static final int C_GROUP = 4;
    private static final int C_POSITION = 5;
    private static final int C_HIRE_DATE = 6;

    /**
     * 生成导入模板。带一行示例和一行填写说明，
     * 并把班组、项目组的可选值直接写进说明里，免得管理员猜名字写错再被打回来。
     */
    public void writeTemplate(OutputStream out) throws IOException {
        String teamNames = teamDao.listByStatusId(STATUS_NORMAL).stream()
                .map(Team::getTeamName).collect(Collectors.joining("/"));
        String groupNames = projectGroupDao.listByStatusId(STATUS_NORMAL).stream()
                .map(ProjectGroup::getGroupName).collect(Collectors.joining("/"));

        List<List<String>> rows = new ArrayList<>();
        rows.add(HEADERS);
        // 说明行故意不写成合法数据。
        // 之前这里放了一行「16/张三/13800138000」当示例，结果谁把模板原样传回来，
        // 工号 16 的真实员工就被张三覆盖掉了。示例只能写在说明文字里，
        // 保证这一行无论如何都过不了校验，忘删也只是报一条错。
        rows.add(List.of(
                "必填 纯数字",
                "必填",
                "必填 11位手机号",
                "可选",
                "可选",
                "可选",
                "可选"));

        // 详细说明单独一张表：写在数据表里会被当成一行数据去校验
        List<List<String>> tips = new ArrayList<>();
        tips.add(List.of("填写说明"));
        tips.add(List.of(""));
        tips.add(List.of("从「员工导入」表的第 3 行开始填写。第 2 行是字段说明，导入前删掉；忘了删也只会多一条报错，不影响其它行。"));
        tips.add(List.of(""));
        tips.add(List.of("工号", "必填，纯数字，例：16。工号是识别员工的依据——工号已存在就更新那个人的资料，不存在才新建。"));
        tips.add(List.of("姓名", "必填，例：张三。"));
        tips.add(List.of("手机号", "必填，11 位。这是员工登录平台的账号，初始密码为手机号后六位。"));
        tips.add(List.of("班组", "可选。可填：" + teamNames));
        tips.add(List.of("项目组", "可选。可填：" + groupNames));
        tips.add(List.of("岗位职务", "可选，例：强电技工。"));
        tips.add(List.of("入职日期", "可选，例：2023/6/12，留空也行。"));
        tips.add(List.of(""));
        tips.add(List.of("导入后如有错误行，页面会逐行列出行号和原因，改完那几行再传一次即可，已成功的不会重复导入。"));

        XlsxUtil.write(out, List.of(
                new XlsxUtil.Sheet("员工导入", rows, new int[]{12, 14, 18, 16, 18, 18, 16}),
                new XlsxUtil.Sheet("填写说明", tips, new int[]{14, 42})));
    }

    public Result<AdminUsersImportResp> importUsers(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("请选择要导入的文件");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".xlsx") && !name.endsWith(".xls")) {
            return Result.error("只支持 .xlsx 或 .xls 文件");
        }

        List<List<String>> raw;
        try {
            raw = XlsxUtil.read(file.getInputStream());
        } catch (Exception e) {
            return Result.error("文件解析失败，请确认是用模板另存的 Excel：" + e.getMessage());
        }
        if (raw.size() <= 1) {
            return Result.error("文件里没有数据行");
        }
        // 第 1 行是表头，校验列名对得上，避免 HR 拿错文件导进来一堆乱数据
        String headerErr = checkHeader(raw.get(0));
        if (headerErr != null) {
            return Result.error(headerErr);
        }
        List<List<String>> rows = raw.subList(1, raw.size());
        if (rows.size() > MAX_ROWS) {
            return Result.error("单次最多导入 " + MAX_ROWS + " 行，当前 " + rows.size() + " 行");
        }

        // 字典按名称建索引，避免逐行查库
        Map<String, Long> teamMap = new HashMap<>();
        for (Team t : teamDao.listByStatusId(STATUS_NORMAL)) {
            teamMap.put(t.getTeamName().trim(), t.getId());
        }
        Map<String, Long> groupMap = new HashMap<>();
        for (ProjectGroup g : projectGroupDao.listByStatusId(STATUS_NORMAL)) {
            groupMap.put(g.getGroupName().trim(), g.getId());
        }

        AdminUsersImportResp resp = new AdminUsersImportResp();
        resp.setTotalCount(rows.size());
        // 文件内部的重复也要挡，否则同一批里两行同工号会互相覆盖
        Set<String> seenEmpNo = new HashSet<>();
        Set<String> seenMobile = new HashSet<>();
        List<AdminUsersImportResp.RowError> errors = new ArrayList<>();

        int dataCount = 0;
        for (int i = 0; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            // 表头占第 1 行，数据从第 2 行开始，与用户打开文件看到的行号对齐
            int rowNum = i + 2;
            if (isBlankRow(row)) {
                // 整行空白直接跳过。Excel 常在末尾留一堆空行，
                // 报成「姓名为空」会刷屏，掩盖真正的问题行
                continue;
            }
            dataCount++;
            try {
                String err = handleRow(row, teamMap, groupMap, seenEmpNo, seenMobile, resp);
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

    private String checkHeader(List<String> header) {
        for (int i = 0; i < HEADERS.size(); i++) {
            String expect = HEADERS.get(i);
            String actual = trim(cell(header, i));
            if (!expect.equals(actual)) {
                return "表头第 " + (i + 1) + " 列应为「" + expect + "」，实际是「" + nvl(actual)
                        + "」。请先下载导入模板，按模板的列顺序填写";
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

    private AdminUsersImportResp.RowError newError(int rowNum, List<String> row, String reason) {
        return new AdminUsersImportResp.RowError()
                .setRowNum(rowNum)
                .setNickname(trim(cell(row, C_NICKNAME)))
                .setEmpNo(trim(cell(row, C_EMP_NO)))
                .setReason(reason);
    }

    /**
     * 处理一行。返回 null 表示成功，否则返回失败原因。
     */
    private String handleRow(List<String> row, Map<String, Long> teamMap, Map<String, Long> groupMap,
                             Set<String> seenEmpNo, Set<String> seenMobile, AdminUsersImportResp resp) {
        String nickname = trim(cell(row, C_NICKNAME));
        String empNo = normalizeEmpNo(trim(cell(row, C_EMP_NO)));
        String mobile = normalizeMobile(trim(cell(row, C_MOBILE)));

        if (!StringUtils.hasText(nickname)) {
            return "姓名为空";
        }
        if (!StringUtils.hasText(empNo)) {
            return "工号为空";
        }
        if (!EMP_NO.matcher(empNo).matches()) {
            return "工号「" + empNo + "」不是纯数字";
        }
        if (!seenEmpNo.add(empNo)) {
            return "工号「" + empNo + "」在本文件中重复";
        }
        if (!StringUtils.hasText(mobile)) {
            return "手机号为空。手机号是登录账号，不能省略";
        }
        if (!MOBILE.matcher(mobile).matches()) {
            return "手机号「" + mask(mobile) + "」格式不对（应为 11 位，当前 " + mobile.length() + " 位）";
        }
        if (!seenMobile.add(mobile)) {
            return "手机号在本文件中重复";
        }

        // 班组 / 项目组：填了就必须能对上字典
        Long teamId = null;
        String teamName = trim(cell(row, C_TEAM));
        if (StringUtils.hasText(teamName)) {
            teamId = teamMap.get(teamName);
            if (teamId == null) {
                return "班组「" + teamName + "」不存在，请先在班组管理里添加";
            }
        }
        Long groupId = null;
        String groupName = trim(cell(row, C_GROUP));
        if (StringUtils.hasText(groupName)) {
            groupId = groupMap.get(groupName);
            if (groupId == null) {
                return "项目组「" + groupName + "」不存在，请先在项目组管理里添加";
            }
        }

        LocalDate hireDate;
        try {
            hireDate = parseDate(trim(cell(row, C_HIRE_DATE)));
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }

        // 工号已存在 -> 更新档案；否则按手机号找；再否则新建
        Users exist = getByEmpNo(empNo);
        if (exist == null) {
            exist = usersDao.getByMobile(mobile);
            if (exist != null && StringUtils.hasText(exist.getEmpNo()) && !empNo.equals(exist.getEmpNo())) {
                return "手机号已绑定工号「" + exist.getEmpNo() + "」，与本行工号「" + empNo + "」冲突";
            }
        } else if (!mobile.equals(exist.getMobile())) {
            // 工号对上了但手机号变了：允许改，但要确认新手机号没被别人占用
            Users other = usersDao.getByMobile(mobile);
            if (other != null && !other.getId().equals(exist.getId())) {
                return "手机号已被工号「" + nvl(other.getEmpNo()) + "」的员工占用";
            }
        }

        Users record = new Users();
        record.setEmpNo(empNo);
        record.setNickname(nickname);
        record.setMobile(mobile);
        record.setTeamId(teamId);
        record.setProjectGroupId(groupId);
        record.setPosition(trim(cell(row, C_POSITION)));
        record.setHireDate(hireDate);

        if (exist == null) {
            // 新建账号：初始密码取手机号后 6 位，员工首次登录后可自行修改
            String initPwd = mobile.substring(mobile.length() - 6);
            record.setStatusId(STATUS_NORMAL);
            record.setMobileSalt(IdUtil.simpleUUID());
            record.setMobilePsw(DigestUtil.sha1Hex(record.getMobileSalt() + initPwd));
            if (usersDao.save(record) <= 0) {
                return "新建账号失败";
            }
            resp.setCreatedCount(resp.getCreatedCount() + 1);
        } else {
            record.setId(exist.getId());
            if (usersDao.updateById(record) <= 0) {
                return "更新档案失败";
            }
            resp.setUpdatedCount(resp.getUpdatedCount() + 1);
        }
        return null;
    }

    private Users getByEmpNo(String empNo) {
        UsersExample example = new UsersExample();
        example.createCriteria().andEmpNoEqualTo(empNo);
        List<Users> list = usersDao.page(1, 1, example).getList();
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 入职日期。HR 那份表同一列里混了两种写法：
     * 「2022/7/2」这样的文本，和「45839」这样的 Excel 日期序列号
     * （序列号是单元格设了日期格式、但导出成 xlsx 后存的是数字）。
     * 两种都要认，空值允许——傅强那行就是空的。
     */
    private LocalDate parseDate(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        if (raw.matches("^\\d+(\\.\\d+)?$")) {
            double serial = Double.parseDouble(raw);
            if (serial < 1 || serial > 100000) {
                return throwBad(raw);
            }
            return EXCEL_EPOCH.plusDays((long) serial);
        }
        String norm = raw.replace('.', '-').replace('/', '-');
        for (String pattern : new String[]{"yyyy-M-d", "yyyy-MM-dd"}) {
            try {
                return LocalDate.parse(norm, DateTimeFormatter.ofPattern(pattern));
            } catch (Exception ignored) {
                // 换下一种格式再试
            }
        }
        return throwBad(raw);
    }

    private LocalDate throwBad(String raw) {
        throw new IllegalArgumentException("入职日期「" + raw + "」无法识别，请用 2022/7/2 这样的格式或留空");
    }

    /**
     * 工号列如果被 Excel 当成数字存，读出来会是「16.0」这种。
     * 直接拿去做纯数字校验会被判不合格，所以先把无意义的小数尾巴去掉。
     */
    private static String normalizeEmpNo(String raw) {
        if (raw == null) {
            return null;
        }
        if (raw.matches("^\\d+\\.0+$")) {
            return raw.substring(0, raw.indexOf('.'));
        }
        return raw;
    }

    /**
     * 手机号同理会被存成数字，11 位会变成「13800138000」没问题，
     * 但一旦被 Excel 记成科学计数（1.38E+10）就还原不回来，明确报错让人改单元格格式。
     */
    private static String normalizeMobile(String raw) {
        if (raw == null) {
            return null;
        }
        if (raw.matches("^\\d+\\.0+$")) {
            return raw.substring(0, raw.indexOf('.'));
        }
        // 去掉常见的空格和连字符
        return raw.replace(" ", "").replace("-", "");
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    /** 报错信息里不回显完整手机号 */
    private static String mask(String mobile) {
        if (mobile.length() <= 4) {
            return mobile;
        }
        return mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 2);
    }
}
