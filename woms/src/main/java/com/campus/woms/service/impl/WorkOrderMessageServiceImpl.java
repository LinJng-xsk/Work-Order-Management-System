package com.campus.woms.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.woms.common.exception.BusinessException;
import com.campus.woms.entity.WorkOrder;
import com.campus.woms.entity.WorkOrderMessage;
import com.campus.woms.mapper.WorkOrderMapper;
import com.campus.woms.mapper.WorkOrderMessageMapper;
import com.campus.woms.service.WorkOrderMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkOrderMessageServiceImpl implements WorkOrderMessageService {

    private final WorkOrderMessageMapper messageMapper;
    private final WorkOrderMapper workOrderMapper;

    /**
     * ★ 对话按「工单 + 处理人」绑定（QQ 好友模型）：
     *  - 管理员（super_admin/manager）：看全部消息
     *  - 运维：只看自己任内 (orderId, 我) 的消息
     *  - 报修人：只看当前处理人的消息（当前是谁处理就显示和谁的对话；那人回来旧对话仍在）
     *  - 待接单/未指派：非管理员不给查看（问题1：没被接没有对话）
     */
    @Override
    public List<WorkOrderMessage> listMessages(Long orderId, Long userId, List<String> roles) {
        WorkOrder order = checkAccess(orderId, userId, roles);
        boolean admin = isAdmin(roles);
        LambdaQueryWrapper<WorkOrderMessage> qw = Wrappers.<WorkOrderMessage>lambdaQuery()
                .eq(WorkOrderMessage::getOrderId, orderId)
                .orderByAsc(WorkOrderMessage::getCreateTime);
        if (!admin) {
            if (roles.contains("operator")) {
                // ★ 运维：自己的对话线（自己发的 + 报修人发给自己的）
                qw.eq(WorkOrderMessage::getAssigneeId, userId);
            } else {
                // ★ 报修人：当前处理人的对话线；无人处理时不给对话
                if (order.getAssigneeId() == null) {
                    throw new BusinessException("工单尚未被接，暂无可查看的沟通记录");
                }
                qw.eq(WorkOrderMessage::getAssigneeId, order.getAssigneeId());
            }
        }
        return messageMapper.selectList(qw);
    }

    @Override
    public void sendMessage(WorkOrderMessage message, Long orderId, Long userId, String userName, List<String> roles) {
        WorkOrder order = checkAccess(orderId, userId, roles);
        boolean admin = isAdmin(roles);
        // ★ 待接单/未指派：禁止发送（前后端双保险）
        if (order.getAssigneeId() == null) {
            throw new BusinessException("工单尚未被接，暂不能发送沟通消息");
        }
        // ★ 运维只能与自己处理的工单对话
        if (!admin && roles.contains("operator")) {
            if (!order.getAssigneeId().equals(userId)) {
                throw new BusinessException("只能与本人处理的工单沟通");
            }
        }
        message.setId(null);
        message.setOrderId(orderId);
        // ★ 标记这条消息属于当前处理人的对话线
        message.setAssigneeId(order.getAssigneeId());
        message.setSenderId(userId);
        message.setSenderName(userName);
        // ★ 角色取第一个有效值（super_admin > manager > operator > employee）
        String role = roles.contains("super_admin") ? "super_admin"
                : roles.contains("manager") ? "manager"
                : roles.contains("operator") ? "operator" : "employee";
        message.setSenderRole(role);
        message.setCreateTime(LocalDateTime.now());
        messageMapper.insert(message);
    }

    /** ★ 访问控制：超管/管理员全部；否则仅报修人 / 处理人本人；返回工单供后续使用 */
    private WorkOrder checkAccess(Long orderId, Long userId, List<String> roles) {
        WorkOrder order = workOrderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("工单不存在");
        boolean admin = isAdmin(roles);
        if (admin) return order;
        if (order.getReporterId() != null && order.getReporterId().equals(userId)) return order;
        if (order.getAssigneeId() != null && order.getAssigneeId().equals(userId)) return order;
        throw new BusinessException("无权查看该工单的沟通记录");
    }

    private boolean isAdmin(List<String> roles) {
        return roles != null && (roles.contains("super_admin") || roles.contains("manager"));
    }
}
