package com.campus.woms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.woms.dto.WorkOrderMonitorDTO;
import com.campus.woms.dto.WorkOrderScheduleDTO;
import com.campus.woms.entity.SysUser;
import com.campus.woms.entity.WorkOrder;
import com.campus.woms.entity.WorkOrderRecord;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface WorkOrderService {

    WorkOrder createOrder(WorkOrder order, Long reporterId, String reporterName);

    Page<WorkOrder> pageOrders(Page<WorkOrder> page, Long reporterId, Long assigneeId,
                               String status, String keyword,
                               String keywordNo, String keywordTitle, String keywordDesc,
                               boolean sortByStatus, Long excludeAssigneeId,
                               boolean employeePool);

    WorkOrder getOrderDetail(Long orderId);

    List<WorkOrderRecord> listRecords(Long orderId);

    void accept(Long orderId, LocalDate appointDate, String timeSlot, Long operatorId, String operatorName);

    void assign(Long orderId, Long assigneeId, String assigneeName, LocalDate appointDate, String timeSlot,
                Long operatorId, String operatorName);

    void reject(Long orderId, String reason, Long operatorId, String operatorName);

    void cancelAssign(Long orderId, Long userId, String userName);

    List<Long> listRejectedOperatorIds(Long orderId);

    void editOrder(Long orderId, String title, String description, String needType, Long userId);

    void submitReview(Long orderId, String repairContent, BigDecimal repairCost,
                      Long operatorId, String operatorName);

    void review(Long orderId, BigDecimal score, String comment, boolean pass,
                Long operatorId, String operatorName);

    void reopen(Long orderId, Long operatorId, String operatorName);

    void close(Long orderId, Long operatorId, String operatorName);

    void deleteOrder(Long orderId);

    void scrap(Long orderId, Long operatorId, String operatorName);

    Page<WorkOrderMonitorDTO> monitorPage(Page<WorkOrder> page, String status, String needType);

    List<SysUser> listOperators();

    List<WorkOrderScheduleDTO> getOperatorSchedule(Long operatorId, LocalDate start, LocalDate end);

    Map<String, Object> getMySchedule(Long operatorId, LocalDate start, LocalDate end);
}
