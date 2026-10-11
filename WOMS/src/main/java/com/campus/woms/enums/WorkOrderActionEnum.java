package com.campus.woms.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WorkOrderActionEnum {

    SUBMIT("submit", "提交工单"),
    ACCEPT("accept", "接单"),
    ASSIGN("assign", "指派处理人"),
    REQUEST_ASSIGN("requestAssign", "请求指派"),
    REJECT_ASSIGN("rejectAssign", "拒绝接单"),
    CANCEL_ASSIGN("cancelAssign", "取消接单"),
    PROCESS("process", "处理工单"),
    SUBMIT_REVIEW("submitReview", "提交复核"),
    REVIEW("review", "验收评价"),
    REJECT("reject", "驳回"),
    REOPEN("reopen", "重新处理"),
    CLOSE("close", "归档关闭"),
    SCRAP("scrap", "报废"),
    TIMEOUT("timeout", "超时未接单");

    private final String code;
    private final String desc;
}
