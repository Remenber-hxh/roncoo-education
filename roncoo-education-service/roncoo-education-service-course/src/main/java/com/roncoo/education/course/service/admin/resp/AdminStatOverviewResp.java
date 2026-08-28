package com.roncoo.education.course.service.admin.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 学习统计看板数据（二开）
 * <p>
 * 完成度一律按 user_study 的实际进度现算，不读 user_course_assign.finish_status。
 * 那个字段是冗余状态，历史上只有「考试通过」这一条路会写它，
 * 学完课程并不会更新，照它统计会把已经学完的人算成未开始。
 *
 * @author 二开
 */
@Data
@Accessors(chain = true)
@Schema(description = "ADMIN-学习统计看板")
public class AdminStatOverviewResp implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "统计区间天数")
    private Integer days;

    @Schema(description = "在册员工数")
    private Integer employeeCount = 0;

    @Schema(description = "有必修任务的员工数")
    private Integer assignedUserCount = 0;

    @Schema(description = "必修任务总数(人次)")
    private Integer requiredTotal = 0;

    @Schema(description = "必修已完成(人次)")
    private Integer requiredDone = 0;

    @Schema(description = "逾期未完成(人次)")
    private Integer overdueCount = 0;

    @Schema(description = "区间内学习总时长(秒)")
    private Long studySeconds = 0L;

    @Schema(description = "区间内有学习记录的员工数")
    private Integer activeUserCount = 0;

    @Schema(description = "学习时长趋势(按日，无记录的日期补零)")
    private List<TrendPoint> trend = new ArrayList<>();

    @Schema(description = "按班组的完成情况")
    private List<GroupStat> teamStats = new ArrayList<>();

    @Schema(description = "按项目组的完成情况")
    private List<GroupStat> projectGroupStats = new ArrayList<>();

    @Schema(description = "按课程的完成情况")
    private List<CourseStat> courseStats = new ArrayList<>();

    @Schema(description = "逾期未完成名单")
    private List<OverdueRow> overdueList = new ArrayList<>();

    @Data
    @Accessors(chain = true)
    @Schema(description = "趋势上的一天")
    public static class TrendPoint implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "日期 yyyy-MM-dd")
        private String date;

        @Schema(description = "当日学习总时长(秒)。单位换算交给前端，"
                + "后端换成分钟会把不足半分钟的活动直接抹成 0，图上看着像没人学")
        private Long seconds = 0L;

        @Schema(description = "当日学习人数")
        private Integer userCount = 0;
    }

    /**
     * 班组 / 项目组维度的一行。两个维度结构相同，共用一个类。
     */
    @Data
    @Accessors(chain = true)
    @Schema(description = "分组完成情况")
    public static class GroupStat implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "分组名称，未归属的归到「未分配」")
        private String name;

        @Schema(description = "该组人数")
        private Integer userCount = 0;

        @Schema(description = "必修任务数(人次)")
        private Integer total = 0;

        @Schema(description = "已完成(人次)")
        private Integer done = 0;

        @Schema(description = "逾期未完成(人次)")
        private Integer overdue = 0;

        @Schema(description = "完成率(0-100)")
        private Integer rate = 0;
    }

    @Data
    @Accessors(chain = true)
    @Schema(description = "课程完成情况")
    public static class CourseStat implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long courseId;

        private String courseName;

        @Schema(description = "指派人数")
        private Integer total = 0;

        @Schema(description = "未开始")
        private Integer notStarted = 0;

        @Schema(description = "学习中")
        private Integer learning = 0;

        @Schema(description = "已学完(含已通过考试)")
        private Integer finished = 0;

        @Schema(description = "完成率(0-100)")
        private Integer rate = 0;
    }

    @Data
    @Accessors(chain = true)
    @Schema(description = "逾期未完成的一行")
    public static class OverdueRow implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long userId;

        private String empNo;

        private String nickname;

        private String teamName;

        private String groupName;

        private Long courseId;

        private String courseName;

        @Schema(description = "截止日期 yyyy-MM-dd")
        private String deadline;

        @Schema(description = "已逾期天数")
        private Integer overdueDays = 0;

        @Schema(description = "当前进度(0-100)")
        private Integer progress = 0;
    }
}
