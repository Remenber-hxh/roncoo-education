package com.roncoo.education.course.service.auth.biz;

import com.roncoo.education.common.base.page.Page;
import com.roncoo.education.common.core.base.Result;
import com.roncoo.education.course.dao.impl.mapper.UserNoticeMapper;
import com.roncoo.education.course.dao.impl.mapper.entity.UserNotice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * AUTH-我的消息（二开新增）
 * <p>
 * 目前只有逾期催办会产生消息。所有查询都带 userId 条件，
 * 消息里会出现「你的课程逾期了」这类个人信息，串号是事故。
 *
 * @author 二开
 */
@Component
@RequiredArgsConstructor
public class AuthNoticeBiz {

    private static final int MAX_PAGE_SIZE = 50;

    /** 页码上限，防止 (current-1)*size 溢出成负数 */
    private static final int MAX_PAGE_CURRENT = 100000;

    private final UserNoticeMapper mapper;

    /**
     * 未读数。员工端每页都要拿它显示角标，走 idx_user_read 索引。
     */
    public Result<Integer> unreadCount(Long userId) {
        if (userId == null) {
            return Result.success(0);
        }
        return Result.success(mapper.countUnread(userId));
    }

    /**
     * 消息分页。返回项目统一的 {@link Page} 结构（totalCount / list），
     * 门户的 useTable 按这套字段取值，自定义 Map 会让列表永远是空的。
     */
    public Result<Page<UserNotice>> page(Long userId, Integer pageCurrent, Integer pageSize) {
        if (userId == null) {
            return Result.error("未登录");
        }
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, MAX_PAGE_SIZE);
        // 页码要封顶：(current-1)*size 是 int 运算，页码传得足够大就会溢出成负数，
        // 拼进 LIMIT 里直接是语法错误，接口 500
        int current = pageCurrent == null || pageCurrent < 1 ? 1 : Math.min(pageCurrent, MAX_PAGE_CURRENT);

        int total = mapper.pageCount(userId);
        Page<UserNotice> page = new Page<>();
        page.setPageCurrent(current);
        page.setPageSize(size);
        page.setTotalCount(total);
        page.setTotalPage(total == 0 ? 0 : (total + size - 1) / size);
        page.setList(total == 0 ? new ArrayList<>() : mapper.page(userId, (current - 1) * size, size));
        return Result.success(page);
    }

    /**
     * 标记单条已读。SQL 里带了 user_id 条件，传别人的消息 ID 改不动。
     */
    public Result<String> read(Long userId, Long id) {
        if (userId == null) {
            return Result.error("未登录");
        }
        mapper.markRead(id, userId);
        return Result.success("操作成功");
    }

    public Result<String> readAll(Long userId) {
        if (userId == null) {
            return Result.error("未登录");
        }
        mapper.markAllRead(userId);
        return Result.success("操作成功");
    }
}
