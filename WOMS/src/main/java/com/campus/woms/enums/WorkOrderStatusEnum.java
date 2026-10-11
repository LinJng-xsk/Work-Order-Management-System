package com.campus.woms.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工单状态枚举（与 work_order.status 字段对应，存编码）
 */
@Getter
@AllArgsConstructor
public enum WorkOrderStatusEnum {

    PENDING_SUBMIT("0", "待提交"),
    PENDING_ACCEPT("1", "待接单"),
    PROCESSING("2", "处理中"),
    PENDING_REVIEW("3", "待复核"),
    COMPLETED("4", "已完成"),
    CLOSED("5", "已关闭"),
    REJECTED("6", "已驳回");

    /** 状态编码（存数据库） */
    private final String code;

    /** 状态描述 */
    private final String desc;

    /** 根据编码获取描述 */
    public static String getDescByCode(String code) {
        for (WorkOrderStatusEnum e : values()) {
            if (e.code.equals(code)) {
                return e.desc;
            }
        }
        return null;
    }
}
