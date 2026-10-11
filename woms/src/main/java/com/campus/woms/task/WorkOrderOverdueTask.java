package com.campus.woms.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.woms.entity.WorkOrder;
import com.campus.woms.entity.WorkOrderRecord;
import com.campus.woms.enums.WorkOrderActionEnum;
import com.campus.woms.enums.WorkOrderStatusEnum;
import com.campus.woms.mapper.WorkOrderMapper;
import com.campus.woms.mapper.WorkOrderRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 超时未接单标记：待接单工单建单超 2 天（自然日）自动标记 overdue=1 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkOrderOverdueTask {

    private final WorkOrderMapper workOrderMapper;
    private final WorkOrderRecordMapper workOrderRecordMapper;

    /** 每小时扫描一次 */
    @Scheduled(cron = "0 0 * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void markOverdue() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(2);
        List<WorkOrder> list = workOrderMapper.selectList(Wrappers.<WorkOrder>lambdaQuery()
                .eq(WorkOrder::getStatus, WorkOrderStatusEnum.PENDING_ACCEPT.getCode())
                .eq(WorkOrder::getOverdue, 0)
                .lt(WorkOrder::getCreateTime, deadline));
        for (WorkOrder order : list) {
            order.setOverdue(1);
            workOrderMapper.updateById(order);
            WorkOrderRecord record = new WorkOrderRecord();
            record.setOrderId(order.getId());
            record.setOperatorName("系统");
            record.setAction(WorkOrderActionEnum.TIMEOUT.getCode());
            record.setFromStatus(WorkOrderStatusEnum.PENDING_ACCEPT.getCode());
            record.setToStatus(WorkOrderStatusEnum.PENDING_ACCEPT.getCode());
            record.setRemark("建单超过2天未接单，标记为超时");
            workOrderRecordMapper.insert(record);
            log.warn("工单 {} 建单超2天未接单，已标记超时", order.getOrderNo());
        }
    }
}
