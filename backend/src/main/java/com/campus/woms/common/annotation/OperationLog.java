package com.campus.woms.common.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解：标注在 Controller 方法上，AOP 自动记录
 * 用法：@OperationLog(module = "工单管理", operation = "创建工单")
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /** 模块名：工单管理 / 设备管理 / 用户管理 / 角色管理 / 菜单管理 / 系统 */
    String module();

    /** 操作描述：创建工单 / 接单 / 提交复核 / 验收 / 驳回 ... */
    String operation();
}
