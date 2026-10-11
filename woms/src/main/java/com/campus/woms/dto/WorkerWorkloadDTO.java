package com.campus.woms.dto;

import lombok.Data;
import java.math.BigDecimal;

/** 运维人员工作量排行 */
@Data
public class WorkerWorkloadDTO {
    private String workerName;
    private Long finishedCount;         // 已完成工单数
    private BigDecimal avgHandleHours;  // 平均处理时长（小时，接单→提交复核）
}
