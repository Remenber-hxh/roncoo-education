package com.roncoo.education.course.dao.impl.mapper.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Course implements Serializable {
    private Long id;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;

    private Integer statusId;

    private Integer sort;

    private Long lecturerId;

    private Long categoryId;

    private String courseName;

    private String courseLogo;




    private Integer isPutaway;

    private Integer courseSort;


    private Integer countStudy;

    private Integer speedDouble;

    private Integer speedDrag;

    private String introduce;

    private static final long serialVersionUID = 1L;
    /**
     * 入职后第几天推送；为空表示不自动推送，只能手工指派
     */
    private Integer pushDay;
    /**
     * 推送范围 1全员 2指定班组
     */
    private Integer pushScope;
    /**
     * push_scope=2 时生效，班组ID逗号分隔
     */
    private String pushTeamIds;
    /**
     * 推送后多少天内需完成
     */
    private Integer deadlineDays;
    /**
     * 是否按顺序解锁课时 0否 1是（需求里的「闯关」）
     */
    private Integer needSequential;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(LocalDateTime gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public LocalDateTime getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(LocalDateTime gmtModified) {
        this.gmtModified = gmtModified;
    }

    public Integer getStatusId() {
        return statusId;
    }

    public void setStatusId(Integer statusId) {
        this.statusId = statusId;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public Long getLecturerId() {
        return lecturerId;
    }

    public void setLecturerId(Long lecturerId) {
        this.lecturerId = lecturerId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName == null ? null : courseName.trim();
    }

    public String getCourseLogo() {
        return courseLogo;
    }

    public void setCourseLogo(String courseLogo) {
        this.courseLogo = courseLogo == null ? null : courseLogo.trim();
    }







    public Integer getIsPutaway() {
        return isPutaway;
    }

    public void setIsPutaway(Integer isPutaway) {
        this.isPutaway = isPutaway;
    }

    public Integer getCourseSort() {
        return courseSort;
    }

    public void setCourseSort(Integer courseSort) {
        this.courseSort = courseSort;
    }



    public Integer getCountStudy() {
        return countStudy;
    }

    public void setCountStudy(Integer countStudy) {
        this.countStudy = countStudy;
    }

    public Integer getSpeedDouble() {
        return speedDouble;
    }

    public void setSpeedDouble(Integer speedDouble) {
        this.speedDouble = speedDouble;
    }

    public Integer getSpeedDrag() {
        return speedDrag;
    }

    public void setSpeedDrag(Integer speedDrag) {
        this.speedDrag = speedDrag;
    }

    public String getIntroduce() {
        return introduce;
    }

    public void setIntroduce(String introduce) {
        this.introduce = introduce == null ? null : introduce.trim();
    }

    public Integer getPushDay() {
        return pushDay;
    }

    public void setPushDay(Integer pushDay) {
        this.pushDay = pushDay;
    }

    public Integer getPushScope() {
        return pushScope;
    }

    public void setPushScope(Integer pushScope) {
        this.pushScope = pushScope;
    }

    public String getPushTeamIds() {
        return pushTeamIds;
    }

    public void setPushTeamIds(String pushTeamIds) {
        this.pushTeamIds = pushTeamIds == null ? null : pushTeamIds.trim();
    }

    public Integer getDeadlineDays() {
        return deadlineDays;
    }

    public void setDeadlineDays(Integer deadlineDays) {
        this.deadlineDays = deadlineDays;
    }

    public Integer getNeedSequential() {
        return needSequential;
    }

    public void setNeedSequential(Integer needSequential) {
        this.needSequential = needSequential;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", gmtCreate=").append(gmtCreate);
        sb.append(", gmtModified=").append(gmtModified);
        sb.append(", statusId=").append(statusId);
        sb.append(", sort=").append(sort);
        sb.append(", lecturerId=").append(lecturerId);
        sb.append(", categoryId=").append(categoryId);
        sb.append(", courseName=").append(courseName);
        sb.append(", courseLogo=").append(courseLogo);
        sb.append(", isPutaway=").append(isPutaway);
        sb.append(", courseSort=").append(courseSort);
        sb.append(", countStudy=").append(countStudy);
        sb.append(", speedDouble=").append(speedDouble);
        sb.append(", speedDrag=").append(speedDrag);
        sb.append(", introduce=").append(introduce);
        sb.append(", pushDay=").append(pushDay);
        sb.append(", pushScope=").append(pushScope);
        sb.append(", pushTeamIds=").append(pushTeamIds);
        sb.append(", deadlineDays=").append(deadlineDays);
        sb.append(", needSequential=").append(needSequential);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}