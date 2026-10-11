package com.campus.woms.dto;

import lombok.Data;

/** 月度工单总量统计 */
@Data
public class MonthlyOrderStatDTO {
    private String month;   // 如 2026-09
    private Long total;     // 当月工单总数
}
