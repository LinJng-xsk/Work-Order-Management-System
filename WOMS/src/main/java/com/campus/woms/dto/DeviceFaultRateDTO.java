package com.campus.woms.dto;

import lombok.Data;
import java.math.BigDecimal;

/** 设备故障率统计：totalCount=该设备累计工单数；faultRate=平均每台设备的故障次数 */
@Data
public class DeviceFaultRateDTO {
    private Long deviceId;
    private String deviceName;
    private Long totalCount;
    private BigDecimal faultRate;
}
