package com.campus.woms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单沟通消息
 *  同一工单不同处理人各自独立对话线，内容保留（人回来旧对话还在），换工单全新对话。
 */
@Data
@TableName("work_order_message")
public class WorkOrderMessage {

    /** ★ 雪花ID（与全项目统一；原 AUTO 但表未建自增，插入必报错） */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 工单 id */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long orderId;

    /** ★ 所属处理人对话线（对话 = orderId + assigneeId；报修人发的也标记当前处理人） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long assigneeId;

    /** 发送人 id */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long senderId;

    /** 发送人姓名 */
    private String senderName;

    /** 发送人角色：super_admin / manager / operator / employee */
    private String senderRole;

    /** 消息内容 */
    private String content;

    private LocalDateTime createTime;
}
