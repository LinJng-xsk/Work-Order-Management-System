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
 * 工单流转记录实体（全链路留痕，不继承 BaseEntity）
 */
@Data
@TableName("work_order_record")
public class WorkOrderRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 工单ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long orderId;

    /** 操作人ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** 操作：submit/accept/assign/process/review/reject/close */
    private String action;

    /** 原状态 */
    private String fromStatus;

    /** 目标状态 */
    private String toStatus;

    /** 备注 */
    private String remark;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
