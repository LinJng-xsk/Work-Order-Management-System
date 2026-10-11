package com.campus.woms.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 工单监控 DTO：基础字段 + 累计维修日 + 超时天数 */
@Data
public class WorkOrderMonitorDTO {

    private Long id;
    private String orderNo;
    private String title;
    private String deviceName;
    private String assigneeName;
    /** 需求情况：红/蓝/绿 */
    private String needType;
    /** 预约处理日期（已派遣才有） */
    private LocalDate appointDate;
    private LocalDateTime createTime;
    /** 工单状态 */
    private String status;
    /** 超时未接单标记：0否 1是 */
    private Integer overdue;
    /** 累计维修日（工作日，剔除周末） */
    private Long workDays;
    /** 超时天数：红=超当天、蓝=超3天、绿=超5天开始累计 */
    private Long overDays;
}
