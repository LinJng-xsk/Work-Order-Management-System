package com.campus.woms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 运维排期项：某天某时段已被占用 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderScheduleDTO {
    private String date;
    private String timeSlot;
}
