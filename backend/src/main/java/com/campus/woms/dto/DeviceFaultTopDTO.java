package com.campus.woms.dto;

import lombok.Data;

/** 故障设备 TOP 排行 */
@Data
public class DeviceFaultTopDTO {
    private String deviceName;
    private Long faultCount;
}
