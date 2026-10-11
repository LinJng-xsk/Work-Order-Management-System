package com.campus.woms.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.woms.entity.WorkOrder;
import com.campus.woms.enums.WorkOrderStatusEnum;
import com.campus.woms.mapper.WorkOrderMapper;
import com.campus.woms.service.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FinanceServiceImpl implements FinanceService {

    private final WorkOrderMapper workOrderMapper;

    @Override
    public Map<String, Object> getOverview(Long operatorId) {
        List<WorkOrder> all = workOrderMapper.selectList(Wrappers.<WorkOrder>lambdaQuery()
                .eq(WorkOrder::getAssigneeId, operatorId)
                .eq(WorkOrder::getStatus, WorkOrderStatusEnum.COMPLETED.getCode()));
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        BigDecimal total = all.stream()
                .map(WorkOrder::getRepairCost).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal monthIncome = all.stream()
                .filter(o -> o.getFinishTime() != null && !o.getFinishTime().toLocalDate().isBefore(monthStart))
                .map(WorkOrder::getRepairCost).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long monthOrders = all.stream()
                .filter(o -> o.getFinishTime() != null && !o.getFinishTime().toLocalDate().isBefore(monthStart))
                .count();
        Map<String, Object> map = new HashMap<>();
        map.put("totalIncome", total);
        map.put("monthIncome", monthIncome);
        map.put("monthOrders", monthOrders);
        return map;
    }

    @Override
    public Page<WorkOrder> getDaily(Long operatorId, Page<WorkOrder> page) {
        LocalDate start = LocalDate.now().minusDays(29);
        return workOrderMapper.selectPage(page, Wrappers.<WorkOrder>lambdaQuery()
                .eq(WorkOrder::getAssigneeId, operatorId)
                .eq(WorkOrder::getStatus, WorkOrderStatusEnum.COMPLETED.getCode())
                .ge(WorkOrder::getFinishTime, start.atStartOfDay())
                .orderByDesc(WorkOrder::getFinishTime));
    }

    @Override
    public Map<String, Object> getDeposit(Long reporterId) {
        List<WorkOrder> all = workOrderMapper.selectList(Wrappers.<WorkOrder>lambdaQuery()
                .eq(WorkOrder::getReporterId, reporterId));
        BigDecimal pending = all.stream()
                .filter(o -> o.getDepositRefunded() != null && o.getDepositRefunded() == 0)
                .map(WorkOrder::getDeposit).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal refunded = all.stream()
                .filter(o -> o.getDepositRefunded() != null && o.getDepositRefunded() == 1)
                .map(WorkOrder::getDeposit).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = all.stream()
                .map(WorkOrder::getDeposit).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> map = new HashMap<>();
        map.put("pendingDeposit", pending);
        map.put("refundedDeposit", refunded);
        map.put("totalDeposit", total);
        return map;
    }
}
