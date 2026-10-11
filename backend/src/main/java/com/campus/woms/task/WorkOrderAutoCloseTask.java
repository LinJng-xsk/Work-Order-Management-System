package com.campus.woms.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.woms.core.WorkOrderStateMachine;
import com.campus.woms.entity.WorkOrder;
import com.campus.woms.entity.WorkOrderRecord;
import com.campus.woms.enums.WorkOrderActionEnum;
import com.campus.woms.enums.WorkOrderStatusEnum;
import com.campus.woms.mapper.WorkOrderMapper;
import com.campus.woms.mapper.WorkOrderRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单自动归档：已完成(status=4)工单，验收完成 N 天后自动转为已关闭(status=5)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkOrderAutoCloseTask {

    private final WorkOrderMapper workOrderMapper;
    private final WorkOrderRecordMapper workOrderRecordMapper;

    /** 自动关闭天数，application.yml 可配 workorder.auto-close-days，默认 7 */
    @Value("${workorder.auto-close-days:7}")
    private int autoCloseDays;

    /** 每天凌晨 1 点执行 */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void autoCloseCompletedOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(autoCloseDays);
        List<WorkOrder> list = workOrderMapper.selectList(
                Wrappers.<WorkOrder>lambdaQuery()
                        .eq(WorkOrder::getStatus, WorkOrderStatusEnum.COMPLETED.getCode())
                        .le(WorkOrder::getFinishTime, deadline));
        if (list.isEmpty()) {
            return;
        }
        int count = 0;
        for (WorkOrder order : list) {
            try {
                WorkOrderStateMachine.checkTransition(order.getStatus(), WorkOrderStatusEnum.CLOSED.getCode());
                order.setStatus(WorkOrderStatusEnum.CLOSED.getCode());
                order.setCloseTime(LocalDateTime.now());
                workOrderMapper.updateById(order);

                WorkOrderRecord record = new WorkOrderRecord();
                record.setOrderId(order.getId());
                record.setOperatorId(order.getAssigneeId());
                record.setOperatorName(order.getAssigneeName());
                record.setAction(WorkOrderActionEnum.CLOSE.getCode());
                record.setFromStatus(WorkOrderStatusEnum.COMPLETED.getCode());
                record.setToStatus(WorkOrderStatusEnum.CLOSED.getCode());
                record.setRemark("验收完成" + autoCloseDays + "天后自动归档关闭");
                workOrderRecordMapper.insert(record);
                count++;
            } catch (Exception e) {
                log.warn("工单 {} 自动关闭失败: {}", order.getId(), e.getMessage());
            }
        }
        log.info("自动归档完成：{} 张已完成工单转为已关闭", count);
    }
}
