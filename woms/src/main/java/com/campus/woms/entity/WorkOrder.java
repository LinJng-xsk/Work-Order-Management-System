package com.campus.woms.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.time.LocalDate;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 工单实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work_order")
public class WorkOrder extends BaseEntity {

    /** 工单编号，如 WO20260910001 */
    private String orderNo;

    /** 故障标题 */
    private String title;

    /** 故障类型 */
    private String faultType;

    /** 优先级：1低 2中 3高 4紧急 */
    private Integer priority;


    /** 需求情况：红/蓝/绿 */
    private String needType;

    /** 超时未接单标记：0否 1是 */
    private Integer overdue;




    /** 状态：0待提交 1待接单 2处理中 3待复核 4已完成 5已关闭 6已驳回 */
    private String status;

    /** 报修人ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long reporterId;

    /** 报修人姓名 */
    private String reporterName;

    /** 报修人电话 */
    private String reporterPhone;

    /** 关联设备ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long deviceId;

    /** 设备名称（冗余） */
    private String deviceName;

    /** 故障位置 */
    private String location;

    /** 故障截图URL，多个逗号分隔 */
    private String images;

    /** 处理人ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long assigneeId;

    /** 处理人姓名 */
    private String assigneeName;

    /** 维修方案 */
    private String repairContent;

    /** 维修费用 */
    private BigDecimal repairCost;

    /** 验收评分（1-5） */
    private BigDecimal evaluateScore;

    /** 验收评价 */
    private String evaluateComment;

    /** 接单时间 */
    private LocalDateTime acceptTime;

    /** 处理完成（提交复核）时间 */
    private LocalDateTime finishTime;

    /** 关闭时间 */
    private LocalDateTime closeTime;

    /** 处理时限（超时预警依据） */
    private LocalDateTime deadline;


    /** 计划处理日期 */
    private LocalDate appointDate;

    /** 时间段（上午 9:00-11:00 / 下午 14:30-17:00） */
    private String timeSlot;


    /** 故障描述 */
    private String description;


    /** 押金金额 */
    private BigDecimal deposit;

    /** 押金退回状态 0在途 1已退 */
    private Integer depositRefunded;

    /** ★ 我的拒接次数：仅拒接过该单的运维可见（返回层填充，不落库） */
    @TableField(exist = false)
    private Long myRejectCount;


    /** 乐观锁版本号（防并发重复提交） */
    @Version
    private Integer version;

    /** 非表字段：驳回次数 */
    @TableField(exist = false)
    private Long rejectCount;

}
