package com.campus.woms.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志实体（AOP 自动记录，不继承 BaseEntity）
 */
@Data
@TableName("sys_operation_log")
public class SysOperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 模块名 */
    private String module;

    /** 操作类型：新增/修改/删除/查询/其他 */
    private String operationType;

    /** 操作明细：具体做了什么（如"设备：666 666"） */
    private String detail;

    /** 操作描述 */
    private String description;

    /** 请求方法：类名#方法名 */
    private String method;

    /** HTTP方法：GET/POST/PUT/DELETE */
    private String requestMethod;

    /** 请求参数（JSON） */
    private String params;

    /** 操作IP */
    private String ip;

    /** 操作人ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** 状态：0成功 1失败 */
    private Integer status;

    /** 异常信息 */
    private String errorMsg;

    /** 耗时（毫秒） */
    private Long costTime;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
