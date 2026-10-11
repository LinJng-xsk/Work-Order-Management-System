package com.campus.woms.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.woms.entity.WorkOrder;
import com.campus.woms.enums.WorkOrderStatusEnum;
import com.campus.woms.mapper.WorkOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单超时预警定时任务
 * 注意：需在主启动类加 @EnableScheduling 注解才会执行
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkOrderTimeoutTask {

    private final WorkOrderMapper workOrderMapper;

    /** 每 10 分钟扫描一次待接单/处理中且超过处理时限的工单 */
    @Scheduled(cron = "0 */10 * * * ?")
    public void timeoutWarning() {
        LocalDateTime now = LocalDateTime.now();
        List<WorkOrder> timeoutOrders = workOrderMapper.selectList(Wrappers.<WorkOrder>lambdaQuery()
                .in(WorkOrder::getStatus,
                        WorkOrderStatusEnum.PENDING_ACCEPT.getCode(),
                        WorkOrderStatusEnum.PROCESSING.getCode())
                .isNotNull(WorkOrder::getDeadline)
                .lt(WorkOrder::getDeadline, now));
        for (WorkOrder order : timeoutOrders) {
            log.warn("工单 {} 已超时：状态={}, 处理时限={}", order.getOrderNo(), order.getStatus(), order.getDeadline());
            // TODO 预警动作：生成站内消息 / 推送 Android（通知表为可选扩展，本 11 表方案先记录日志）
        }
    }
}
