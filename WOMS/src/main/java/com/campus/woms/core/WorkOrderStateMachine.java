package com.campus.woms.core;

import com.campus.woms.common.exception.BusinessException;
import com.campus.woms.enums.WorkOrderStatusEnum;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 工单状态机：集中定义"合法流转表"，所有流转必须先过校验
 * 0待提交 → 1待接单 → 2处理中 → 3待复核 → 4已完成 → 5已关闭
 *                        3待复核 → 6已驳回 → 1待接单（重新派单）
 */
public class WorkOrderStateMachine {

    /** 合法流转表：from -> {允许的 to 集合} */
    private static final Map<String, Set<String>> TRANSITIONS = new HashMap<>();

    static {
        allow(WorkOrderStatusEnum.PENDING_SUBMIT.getCode(), WorkOrderStatusEnum.PENDING_ACCEPT.getCode());
        allow(WorkOrderStatusEnum.PENDING_ACCEPT.getCode(), WorkOrderStatusEnum.PROCESSING.getCode());
        allow(WorkOrderStatusEnum.PROCESSING.getCode(), WorkOrderStatusEnum.PROCESSING.getCode());      // 处理中可重新指派
        allow(WorkOrderStatusEnum.PROCESSING.getCode(), WorkOrderStatusEnum.PENDING_REVIEW.getCode());
        allow(WorkOrderStatusEnum.PENDING_REVIEW.getCode(), WorkOrderStatusEnum.COMPLETED.getCode());   // 验收通过
        allow(WorkOrderStatusEnum.PENDING_REVIEW.getCode(), WorkOrderStatusEnum.REJECTED.getCode());    // 验收不通过
        allow(WorkOrderStatusEnum.REJECTED.getCode(), WorkOrderStatusEnum.PENDING_ACCEPT.getCode());    // ★ 已驳回 → 待接单（重新打开后重新派单）
        allow(WorkOrderStatusEnum.COMPLETED.getCode(), WorkOrderStatusEnum.CLOSED.getCode());
    }

    private static void allow(String from, String to) {
        TRANSITIONS.computeIfAbsent(from, k -> new HashSet<>()).add(to);
    }

    /** 是否允许 from -> to */
    public static boolean canTransition(String from, String to) {
        return TRANSITIONS.getOrDefault(from, Collections.emptySet()).contains(to);
    }

    /** 不允许则抛业务异常（Service 调用入口统一用这个） */
    public static void checkTransition(String from, String to) {
        if (!canTransition(from, to)) {
            throw new BusinessException("非法状态流转：" + WorkOrderStatusEnum.getDescByCode(from)
                    + " 不能流转为 " + WorkOrderStatusEnum.getDescByCode(to));
        }
    }
}
