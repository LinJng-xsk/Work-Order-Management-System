package com.campus.woms.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 设备状态枚举（与 device.status 字段对应）
 */
@Getter
@AllArgsConstructor
public enum DeviceStatusEnum {

    ONLINE(0, "在线"),
    OFFLINE(1, "离线"),
    REPAIRING(2, "维修中"),
    SCRAPPED(3, "报废");

    private final Integer code;
    private final String desc;
}
