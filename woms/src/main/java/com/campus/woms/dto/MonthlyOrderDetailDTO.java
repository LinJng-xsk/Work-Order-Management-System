package com.campus.woms.dto;

import lombok.Data;

/**
 * 月度派单明细：某月中每天由谁处理了工单
 */
@Data
public class MonthlyOrderDetailDTO {

    /** 日（01~31） */
    private String day;

    /** 运维人员用户名 */
    private String assigneeName;

    /** 当日派单（工单）数 */
    private Integer cnt;

    /** 计划处理日期（2026-09-14） */
    private String appointDate;

    /** 时间段（上午 9:00-11:00 / 下午 14:30-17:00） */
    private String timeSlot;



}
