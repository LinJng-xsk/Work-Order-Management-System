package com.campus.woms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.woms.common.exception.BusinessException;
import com.campus.woms.core.WorkOrderStateMachine;
import com.campus.woms.dto.WorkOrderMonitorDTO;
import com.campus.woms.dto.WorkOrderScheduleDTO;
import com.campus.woms.entity.Device;
import com.campus.woms.entity.SysUser;
import com.campus.woms.entity.WorkOrder;
import com.campus.woms.entity.WorkOrderRecord;
import com.campus.woms.enums.WorkOrderActionEnum;
import com.campus.woms.enums.WorkOrderStatusEnum;
import com.campus.woms.mapper.DeviceMapper;
import com.campus.woms.mapper.SysUserMapper;
import com.campus.woms.mapper.WorkOrderMapper;
import com.campus.woms.mapper.WorkOrderRecordMapper;
import com.campus.woms.security.LoginUser;
import com.campus.woms.security.SecurityUtils;
import com.campus.woms.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkOrderServiceImpl implements WorkOrderService {

    private final WorkOrderMapper workOrderMapper;
    private final WorkOrderRecordMapper workOrderRecordMapper;
    private final DeviceMapper deviceMapper;
    private final SysUserMapper sysUserMapper;

    // ==================== 提交工单 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WorkOrder createOrder(WorkOrder order, Long reporterId, String reporterName) {
        order.setId(null);
        order.setOrderNo(generateOrderNo());
        order.setReporterId(reporterId);
        order.setReporterName(reporterName);
        order.setStatus(WorkOrderStatusEnum.PENDING_ACCEPT.getCode());
        order.setPriority(order.getPriority() == null ? 2 : order.getPriority());
        order.setNeedType(order.getNeedType() == null ? "蓝" : order.getNeedType());
        order.setOverdue(0);
        order.setVersion(0);
        // ★ 押金：默认 20，提交即缴纳
        if (order.getDeposit() == null) {
            order.setDeposit(new BigDecimal("20"));
        }
        order.setDepositRefunded(0);
        // ★ 设备名兜底
        if (order.getDeviceId() != null && (order.getDeviceName() == null || order.getDeviceName().isBlank())) {
            Device d = deviceMapper.selectById(order.getDeviceId());
            if (d != null) order.setDeviceName(d.getDeviceName());
        }
        // ★ 指定运维：校验工作日，状态保持待接单（等运维确认）
        if (order.getAssigneeId() != null) {
            checkWorkDay(order.getAssigneeId(), order.getAppointDate(), order.getTimeSlot());
            checkTimeSlotConflict(null, order.getAssigneeId(), order.getAppointDate(), order.getTimeSlot());
            order.setAcceptTime(null);
        }
        workOrderMapper.insert(order);
        String remark = order.getAssigneeId() != null
                ? "报修人提交工单并指定运维：" + order.getAssigneeName() + "，待其确认"
                : "报修人提交工单（进待接单池）";
        saveRecord(order.getId(), reporterId, reporterName, WorkOrderActionEnum.SUBMIT,
                null, order.getStatus(), remark);
        syncDeviceStatus(order.getDeviceId());
        return order;
    }

    private String generateOrderNo() {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String rand = UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase();
        return "WO" + time + rand;
    }

    // ==================== 分页/详情 ====================

    /**
     * ★ 分页查询：
     *  keyword（旧接口，兼容）
     *  keywordNo/keywordTitle/keywordDesc（三字段搜索，同时生效 AND）
     *  sortByStatus=true（工单管理页）：状态分组排序
     *  excludeAssigneeId（运维接单大厅）：排除"指派给我"的单（我在我的接单里接/不接）
     *  employeePool=true（员工工单管理）：待接单池 ∪ 自己报修的全部
     */
    @Override
    public Page<WorkOrder> pageOrders(Page<WorkOrder> page, Long reporterId, Long assigneeId,
                                      String status, String keyword,
                                      String keywordNo, String keywordTitle, String keywordDesc,
                                      boolean sortByStatus, Long excludeAssigneeId,
                                      boolean employeePool) {
        LambdaQueryWrapper<WorkOrder> qw = Wrappers.lambdaQuery();
        if (reporterId != null) {
            qw.eq(WorkOrder::getReporterId, reporterId);
        }
        if (assigneeId != null) {
            qw.eq(WorkOrder::getAssigneeId, assigneeId);
        }
        // ★ 运维接单大厅：排除"指派给我"的单，但未指派（NULL）的待接单必须保留（MySQL 中 NULL != 3 不是 TRUE，会误杀公共池单）
        if (excludeAssigneeId != null) {
            qw.and(w -> w.ne(WorkOrder::getAssigneeId, excludeAssigneeId)
                    .or().isNull(WorkOrder::getAssigneeId));
        }
        // ★ 员工视图：待接单池（status=1）∪ 自己报修的全部单
        if (employeePool) {
            LoginUser me = SecurityUtils.getLoginUser();
            if (me != null) {
                qw.and(w -> w.eq(WorkOrder::getReporterId, me.getUserId())
                        .or().eq(WorkOrder::getStatus, WorkOrderStatusEnum.PENDING_ACCEPT.getCode()));
            }
        }
        qw.eq(status != null && !status.isBlank(), WorkOrder::getStatus, status);
        // ★ 兼容旧搜索：一个框同时搜 标题/编号/描述
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(WorkOrder::getTitle, keyword)
                    .or().like(WorkOrder::getOrderNo, keyword)
                    .or().apply("id LIKE {0}", "%" + keyword + "%")
                    .or().like(WorkOrder::getDescription, keyword));
        }
        // ★ 新三字段搜索：编号（id 或 WO 号）
        if (keywordNo != null && !keywordNo.isBlank()) {
            qw.and(w -> w.like(WorkOrder::getOrderNo, keywordNo)
                    .or().apply("id LIKE {0}", "%" + keywordNo + "%"));
        }
        // ★ 新三字段搜索：标题
        if (keywordTitle != null && !keywordTitle.isBlank()) {
            qw.like(WorkOrder::getTitle, keywordTitle);
        }
        // ★ 新三字段搜索：问题描述
        if (keywordDesc != null && !keywordDesc.isBlank()) {
            qw.like(WorkOrder::getDescription, keywordDesc);
        }
        if (sortByStatus) {
            qw.last("ORDER BY CASE status WHEN '6' THEN 0 WHEN '1' THEN 1 WHEN '2' THEN 2 " +
                    "WHEN '3' THEN 3 WHEN '4' THEN 4 ELSE 5 END, create_time DESC");
        } else {
            qw.orderByDesc(WorkOrder::getCreateTime);
        }
        Page<WorkOrder> result = workOrderMapper.selectPage(page, qw);
        fillRejectCount(result.getRecords());
        // ★ 我的拒接次数：仅当前登录运维可见（拒接过才有值，其他人恒 0）
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser != null) {
            fillMyRejectCount(result.getRecords(), loginUser.getUserId());
        }
        // ★ 报废标记：有 SCRAP 记录的工单，返回层 status 置为 "7"（前端显示"已报废"），不落库
        fillScrapped(result.getRecords());
        if (sortByStatus && result.getRecords() != null) {
            result.getRecords().sort((a, b) -> {
                int wa = "1".equals(a.getStatus()) && a.getRejectCount() != null && a.getRejectCount() > 0 ? -1 : 0;
                int wb = "1".equals(b.getStatus()) && b.getRejectCount() != null && b.getRejectCount() > 0 ? -1 : 0;
                return Integer.compare(wb, wa);
            });
        }
        return result;
    }

    /** ★ 驳回次数：只统计【用户验收驳回】(REJECT)，运维拒单(REJECT_ASSIGN)不计入 */
    private void fillRejectCount(List<WorkOrder> records) {
        if (records == null || records.isEmpty()) return;
        List<Long> ids = records.stream().map(WorkOrder::getId).collect(Collectors.toList());
        Map<Long, Long> countMap = workOrderRecordMapper.selectList(
                        Wrappers.<WorkOrderRecord>lambdaQuery()
                                .in(WorkOrderRecord::getOrderId, ids)
                                // ★ 只算验收驳回，去掉 REJECT_ASSIGN
                                .eq(WorkOrderRecord::getAction, WorkOrderActionEnum.REJECT.getCode()))
                .stream()
                .collect(Collectors.groupingBy(WorkOrderRecord::getOrderId, Collectors.counting()));
        records.forEach(o -> o.setRejectCount(countMap.getOrDefault(o.getId(), 0L)));
    }

    /** ★ 我的拒接次数：只统计【我】的运维拒单(REJECT_ASSIGN 且 operatorId=当前登录人)，其他人恒为 0 */
    private void fillMyRejectCount(List<WorkOrder> records, Long operatorId) {
        if (records == null || records.isEmpty() || operatorId == null) return;
        List<Long> ids = records.stream().map(WorkOrder::getId).collect(Collectors.toList());
        Map<Long, Long> countMap = workOrderRecordMapper.selectList(
                        Wrappers.<WorkOrderRecord>lambdaQuery()
                                .in(WorkOrderRecord::getOrderId, ids)
                                .eq(WorkOrderRecord::getAction, WorkOrderActionEnum.REJECT_ASSIGN.getCode())
                                .eq(WorkOrderRecord::getOperatorId, operatorId))
                .stream()
                .collect(Collectors.groupingBy(WorkOrderRecord::getOrderId, Collectors.counting()));
        records.forEach(o -> o.setMyRejectCount(countMap.getOrDefault(o.getId(), 0L)));
    }

    /** ★ 报废标记：存在 SCRAP 记录的工单 → status 临时置 "7"（前端显示"已报废"），不落库 */
    private void fillScrapped(List<WorkOrder> records) {
        if (records == null || records.isEmpty()) return;
        List<Long> ids = records.stream().map(WorkOrder::getId).collect(Collectors.toList());
        List<Long> scrappedIds = workOrderRecordMapper.selectList(
                        Wrappers.<WorkOrderRecord>lambdaQuery()
                                .in(WorkOrderRecord::getOrderId, ids)
                                .eq(WorkOrderRecord::getAction, WorkOrderActionEnum.SCRAP.getCode()))
                .stream().map(WorkOrderRecord::getOrderId).distinct().collect(Collectors.toList());
        if (scrappedIds.isEmpty()) return;
        records.forEach(o -> {
            if (scrappedIds.contains(o.getId())) o.setStatus("7");
        });
    }

    @Override
    public WorkOrder getOrderDetail(Long orderId) {
        WorkOrder order = getAndCheck(orderId);
        fillDeviceName(order);
        return order;
    }

    private void fillDeviceName(WorkOrder order) {
        if (order.getDeviceId() != null && (order.getDeviceName() == null || order.getDeviceName().isBlank())) {
            Device d = deviceMapper.selectById(order.getDeviceId());
            if (d != null) order.setDeviceName(d.getDeviceName());
        }
    }

    @Override
    public List<WorkOrderRecord> listRecords(Long orderId) {
        return workOrderRecordMapper.selectList(
                Wrappers.<WorkOrderRecord>lambdaQuery()
                        .eq(WorkOrderRecord::getOrderId, orderId)
                        .orderByAsc(WorkOrderRecord::getCreateTime));
    }

    // ==================== 接单/指派/拒单/取消 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void accept(Long orderId, LocalDate appointDate, String timeSlot, Long operatorId, String operatorName) {
        // ★ 兼容：前端未传操作人时，从登录态兜底
        LoginUser me = SecurityUtils.getLoginUser();
        if (operatorId == null && me != null) {
            operatorId = me.getUserId();
        }
        if (operatorName == null || operatorName.isBlank()) {
            if (me != null) {
                SysUser u = sysUserMapper.selectById(operatorId);
                operatorName = (u != null && u.getNickname() != null && !u.getNickname().isBlank())
                        ? u.getNickname() : me.getUsername();
            }
        }
        WorkOrder order = getAndCheck(orderId);
        // ★ 防假单：不能接自己报修的工单
        if (order.getReporterId() != null && order.getReporterId().equals(operatorId)) {
            throw new BusinessException("不能接自己报修的工单");
        }
        checkWorkDay(operatorId, appointDate, timeSlot);
        checkTimeSlotConflict(orderId, operatorId, appointDate, timeSlot);
        order.setAssigneeId(operatorId);
        order.setAssigneeName(operatorName);
        order.setAppointDate(appointDate);
        order.setTimeSlot(timeSlot);
        order.setAcceptTime(LocalDateTime.now());
        order.setOverdue(0);
        changeStatus(order, WorkOrderStatusEnum.PROCESSING.getCode(), operatorId, operatorName,
                WorkOrderActionEnum.ACCEPT, "接单并开始处理，预约：" + appointDate + " " + timeSlot);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assign(Long orderId, Long assigneeId, String assigneeName, LocalDate appointDate, String timeSlot,
                       Long operatorId, String operatorName) {
        WorkOrder order = getAndCheck(orderId);
        checkWorkDay(assigneeId, appointDate, timeSlot);
        checkTimeSlotConflict(orderId, assigneeId, appointDate, timeSlot);
        order.setAssigneeId(assigneeId);
        order.setAssigneeName(assigneeName);
        order.setAppointDate(appointDate);
        order.setTimeSlot(timeSlot);
        order.setOverdue(0);
        order.setStatus(WorkOrderStatusEnum.PENDING_ACCEPT.getCode());
        int rows = workOrderMapper.updateById(order);
        if (rows == 0) throw new BusinessException("操作冲突，工单状态已被他人修改，请刷新后重试");
        saveRecord(order.getId(), operatorId, operatorName, WorkOrderActionEnum.ASSIGN,
                WorkOrderStatusEnum.PENDING_ACCEPT.getCode(), WorkOrderStatusEnum.PENDING_ACCEPT.getCode(),
                "指定处理人：" + assigneeName + "，预约：" + appointDate + " " + timeSlot + "，待其确认");
        syncDeviceStatus(order.getDeviceId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long orderId, String reason, Long operatorId, String operatorName) {
        WorkOrder order = getAndCheck(orderId);
        if (order.getAssigneeId() == null || !order.getAssigneeId().equals(operatorId)) {
            throw new BusinessException("只能拒绝指派给您的工单");
        }
        String from = order.getStatus();
        clearAssignee(order);
        order.setStatus(WorkOrderStatusEnum.PENDING_ACCEPT.getCode());
        order.setOverdue(0);
        // ★ 修复：updateById 默认忽略 null 字段，必须用 UpdateWrapper 显式 set null 才能真正清掉处理人
        workOrderMapper.update(null, Wrappers.<WorkOrder>lambdaUpdate()
                .set(WorkOrder::getAssigneeId, null)
                .set(WorkOrder::getAssigneeName, null)
                .set(WorkOrder::getAcceptTime, null)
                .set(WorkOrder::getAppointDate, null)
                .set(WorkOrder::getTimeSlot, null)
                .set(WorkOrder::getStatus, WorkOrderStatusEnum.PENDING_ACCEPT.getCode())
                .set(WorkOrder::getOverdue, 0)
                .eq(WorkOrder::getId, orderId));
        saveRecord(order.getId(), operatorId, operatorName, WorkOrderActionEnum.REJECT_ASSIGN,
                from, WorkOrderStatusEnum.PENDING_ACCEPT.getCode(),
                "运维拒绝接单：" + (reason == null || reason.isBlank() ? "无原因" : reason) + "，工单回到待接单池");
        syncDeviceStatus(order.getDeviceId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelAssign(Long orderId, Long userId, String userName) {
        WorkOrder order = getAndCheck(orderId);
        if (order.getReporterId() == null || !order.getReporterId().equals(userId)) {
            throw new BusinessException("只能取消自己工单的接单");
        }
        if (order.getAssigneeId() == null) {
            throw new BusinessException("该工单尚未指派处理人");
        }
        String from = order.getStatus();
        String oldName = order.getAssigneeName();
        clearAssignee(order);
        order.setStatus(WorkOrderStatusEnum.PENDING_ACCEPT.getCode());
        order.setOverdue(0);
        // ★ 修复：updateById 默认忽略 null 字段，必须用 UpdateWrapper 显式 set null 才能真正清掉处理人
        workOrderMapper.update(null, Wrappers.<WorkOrder>lambdaUpdate()
                .set(WorkOrder::getAssigneeId, null)
                .set(WorkOrder::getAssigneeName, null)
                .set(WorkOrder::getAcceptTime, null)
                .set(WorkOrder::getAppointDate, null)
                .set(WorkOrder::getTimeSlot, null)
                .set(WorkOrder::getStatus, WorkOrderStatusEnum.PENDING_ACCEPT.getCode())
                .set(WorkOrder::getOverdue, 0)
                .eq(WorkOrder::getId, orderId));
        saveRecord(order.getId(), userId, userName, WorkOrderActionEnum.CANCEL_ASSIGN,
                from, WorkOrderStatusEnum.PENDING_ACCEPT.getCode(),
                "用户取消运维接单（原处理人：" + oldName + "），工单回到待接单池");
        syncDeviceStatus(order.getDeviceId());
    }

    private void clearAssignee(WorkOrder order) {
        order.setAssigneeId(null);
        order.setAssigneeName(null);
        order.setAcceptTime(null);
        order.setAppointDate(null);
        order.setTimeSlot(null);
    }

    @Override
    public List<Long> listRejectedOperatorIds(Long orderId) {
        return workOrderRecordMapper.selectList(Wrappers.<WorkOrderRecord>lambdaQuery()
                        .eq(WorkOrderRecord::getOrderId, orderId)
                        .eq(WorkOrderRecord::getAction, WorkOrderActionEnum.REJECT_ASSIGN.getCode()))
                .stream().map(WorkOrderRecord::getOperatorId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    @Override
    public void editOrder(Long orderId, String title, String description, String needType, Long userId) {
        WorkOrder order = getAndCheck(orderId);
        if (order.getReporterId() == null || !order.getReporterId().equals(userId)) {
            throw new BusinessException("只能编辑自己提交的工单");
        }
        if (!WorkOrderStatusEnum.PENDING_ACCEPT.getCode().equals(order.getStatus())) {
            throw new BusinessException("仅待接单状态的工单可编辑");
        }
        if (title != null && !title.isBlank()) order.setTitle(title);
        order.setDescription(description);
        if (needType != null && !needType.isBlank()) order.setNeedType(needType);
        workOrderMapper.updateById(order);
    }

    // ==================== 处理/验收/重开/关闭/删除/报废 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitReview(Long orderId, String repairContent, BigDecimal repairCost,
                             Long operatorId, String operatorName) {
        WorkOrder order = getAndCheck(orderId);
        order.setRepairContent(repairContent);
        order.setRepairCost(repairCost);
        order.setFinishTime(LocalDateTime.now());
        changeStatus(order, WorkOrderStatusEnum.PENDING_REVIEW.getCode(), operatorId, operatorName,
                WorkOrderActionEnum.SUBMIT_REVIEW, "提交复核");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void review(Long orderId, BigDecimal score, String comment, boolean pass,
                       Long operatorId, String operatorName) {
        WorkOrder order = getAndCheck(orderId);
        order.setEvaluateScore(score);
        order.setEvaluateComment(comment);
        String from = order.getStatus();
        if (pass) {
            changeStatus(order, WorkOrderStatusEnum.COMPLETED.getCode(), operatorId, operatorName,
                    WorkOrderActionEnum.REVIEW, "验收通过，评分：" + score);
        } else {
            // ★ 验收不通过：直接回待接单池 + 清空处理人 + 驳回次数+1
            clearAssignee(order);
            order.setStatus(WorkOrderStatusEnum.PENDING_ACCEPT.getCode());
            order.setOverdue(0);
            // ★ 修复：updateById 默认忽略 null 字段，必须用 UpdateWrapper 显式 set null 才能真正清掉处理人
            int rows = workOrderMapper.update(null, Wrappers.<WorkOrder>lambdaUpdate()
                    .set(WorkOrder::getAssigneeId, null)
                    .set(WorkOrder::getAssigneeName, null)
                    .set(WorkOrder::getAcceptTime, null)
                    .set(WorkOrder::getAppointDate, null)
                    .set(WorkOrder::getTimeSlot, null)
                    .set(WorkOrder::getStatus, WorkOrderStatusEnum.PENDING_ACCEPT.getCode())
                    .set(WorkOrder::getOverdue, 0)
                    .eq(WorkOrder::getId, orderId));
            if (rows == 0) throw new BusinessException("操作冲突，请刷新后重试");
            saveRecord(order.getId(), operatorId, operatorName, WorkOrderActionEnum.REJECT,
                    from, WorkOrderStatusEnum.PENDING_ACCEPT.getCode(),
                    "验收不通过，驳回：" + (comment == null || comment.isBlank() ? "无" : comment) + "，重新进入待接单池");
            syncDeviceStatus(order.getDeviceId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reopen(Long orderId, Long operatorId, String operatorName) {
        WorkOrder order = getAndCheck(orderId);
        changeStatus(order, WorkOrderStatusEnum.PENDING_ACCEPT.getCode(), operatorId, operatorName,
                WorkOrderActionEnum.REOPEN, "重新打开，进入待接单池");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void close(Long orderId, Long operatorId, String operatorName) {
        WorkOrder order = getAndCheck(orderId);
        order.setCloseTime(LocalDateTime.now());
        changeStatus(order, WorkOrderStatusEnum.CLOSED.getCode(), operatorId, operatorName,
                WorkOrderActionEnum.CLOSE, "归档关闭");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long orderId) {
        WorkOrder order = getAndCheck(orderId);
        workOrderRecordMapper.delete(Wrappers.<WorkOrderRecord>lambdaQuery()
                .eq(WorkOrderRecord::getOrderId, orderId));
        workOrderMapper.deleteById(orderId);
        syncDeviceStatus(order.getDeviceId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void scrap(Long orderId, Long operatorId, String operatorName) {
        WorkOrder order = getAndCheck(orderId);
        // ★ 管理角色全权；运维仅能报废自己接的单
        boolean admin = hasRole("super_admin") || hasRole("manager");
        if (!admin && (order.getAssigneeId() == null || !order.getAssigneeId().equals(operatorId))) {
            throw new BusinessException("仅能报废自己接单的工单");
        }
        // ★ 非管理员：仅待复核阶段可报废（与前端按钮一致）
        if (!admin && !WorkOrderStatusEnum.PENDING_REVIEW.getCode().equals(order.getStatus())) {
            throw new BusinessException("仅待复核阶段的工单可报废");
        }
        Long deviceId = order.getDeviceId();
        List<WorkOrder> active = workOrderMapper.selectList(Wrappers.<WorkOrder>lambdaQuery()
                .eq(WorkOrder::getDeviceId, deviceId)
                .ne(WorkOrder::getStatus, WorkOrderStatusEnum.CLOSED.getCode()));
        for (WorkOrder o : active) {
            String from = o.getStatus();
            o.setStatus(WorkOrderStatusEnum.CLOSED.getCode());
            o.setDepositRefunded(1);   // ★ 报废 → 押金退回
            o.setCloseTime(LocalDateTime.now());
            workOrderMapper.updateById(o);
            saveRecord(o.getId(), operatorId, operatorName, WorkOrderActionEnum.SCRAP,
                    from, WorkOrderStatusEnum.CLOSED.getCode(), "设备报废，工单关闭");
        }
        if (deviceId != null) {
            Device update = new Device();
            update.setId(deviceId);
            update.setStatus(3);
            deviceMapper.updateById(update);
        }
    }

    // ==================== 工单监控 ====================

    @Override
    public Page<WorkOrderMonitorDTO> monitorPage(Page<WorkOrder> page, String status, String needType) {
        LambdaQueryWrapper<WorkOrder> qw = Wrappers.lambdaQuery();
        qw.eq(status != null && !status.isBlank(), WorkOrder::getStatus, status)
                .eq(needType != null && !needType.isBlank(), WorkOrder::getNeedType, needType);
        // ★ 排序：超时天数降序 → 需求等级+驳回（红驳回>红>蓝驳回>蓝>绿驳回>绿）→ 创建时间倒序
        //   - 超时天数 = 自然日(预约日期或创建日期 → 今天) - 需求宽限期(红0/蓝3/绿5)
        //   - 驳回 = 存在 work_order_record action='reject'（验收驳回），不含运维拒单(reject_assign)
        String rejectCode = WorkOrderActionEnum.REJECT.getCode();
        qw.last("ORDER BY " +
                "(TIMESTAMPDIFF(DAY, IFNULL(appoint_date, DATE(create_time)), CURDATE()) " +
                " - CASE need_type WHEN '红' THEN 0 WHEN '绿' THEN 5 ELSE 3 END) DESC, " +
                "CASE " +
                " WHEN need_type = '红' AND EXISTS(SELECT 1 FROM work_order_record r WHERE r.order_id = work_order.id AND r.action = '" + rejectCode + "') THEN 0 " +
                " WHEN need_type = '红' THEN 1 " +
                " WHEN need_type = '蓝' AND EXISTS(SELECT 1 FROM work_order_record r WHERE r.order_id = work_order.id AND r.action = '" + rejectCode + "') THEN 2 " +
                " WHEN need_type = '蓝' THEN 3 " +
                " WHEN need_type = '绿' AND EXISTS(SELECT 1 FROM work_order_record r WHERE r.order_id = work_order.id AND r.action = '" + rejectCode + "') THEN 4 " +
                " ELSE 5 END, " +
                "create_time DESC");
        Page<WorkOrder> raw = workOrderMapper.selectPage(page, qw);
        Page<WorkOrderMonitorDTO> result = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        result.setRecords(raw.getRecords().stream().map(this::toMonitorDTO).collect(Collectors.toList()));
        return result;
    }


    private WorkOrderMonitorDTO toMonitorDTO(WorkOrder w) {
        WorkOrderMonitorDTO dto = new WorkOrderMonitorDTO();
        BeanUtils.copyProperties(w, dto);
        LocalDate start = w.getAppointDate() != null
                ? w.getAppointDate()
                : (w.getCreateTime() == null ? null : w.getCreateTime().toLocalDate());
        LocalDate today = LocalDate.now();
        long workDays = 0;
        if (start != null && !start.isAfter(today)) {
            workDays = countWorkDays(start, today.minusDays(1));
        }
        dto.setWorkDays(workDays);
        String need = w.getNeedType() == null ? "蓝" : w.getNeedType();
        int grace = "红".equals(need) ? 0 : ("绿".equals(need) ? 5 : 3);
        dto.setOverDays(Math.max(0, workDays - grace));
        return dto;
    }

    private long countWorkDays(LocalDate from, LocalDate to) {
        long days = 0;
        LocalDate d = from;
        while (!d.isAfter(to)) {
            DayOfWeek dow = d.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) days++;
            d = d.plusDays(1);
        }
        return days;
    }

    // ==================== 运维列表 ====================

    @Override
    public List<SysUser> listOperators() {
        List<Map<String, Object>> rows = sysUserMapper.listOperators();
        if (rows == null || rows.isEmpty()) {
            return new ArrayList<>();
        }
        return rows.stream().map(row -> {
            SysUser u = new SysUser();
            Object id = row.get("id");
            if (id instanceof Number) {
                u.setId(((Number) id).longValue());
            }
            Object nick = row.get("nickname");
            u.setNickname(nick == null ? null : String.valueOf(nick));
            Object wd = row.get("workDays");
            u.setWorkDays(wd == null ? null : String.valueOf(wd));
            return u;
        }).collect(Collectors.toList());
    }

    /** ★ 工作日校验 */
    private void checkWorkDay(Long assigneeId, LocalDate appointDate, String timeSlot) {
        if (assigneeId == null || appointDate == null) return;
        SysUser operator = sysUserMapper.selectById(assigneeId);
        if (operator == null) return;
        String days = operator.getWorkDays();
        if (days == null || days.isBlank()) return;
        int dow = appointDate.getDayOfWeek().getValue();
        List<Integer> list = Arrays.stream(days.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(Integer::parseInt).collect(Collectors.toList());
        if (!list.contains(dow)) {
            throw new BusinessException("该运维" + weekdayText(dow) + "不值班，请选择其工作日");
        }
    }

    /** ★ 时段冲突校验 */
    private void checkTimeSlotConflict(Long orderId, Long assigneeId, LocalDate appointDate, String timeSlot) {
        if (assigneeId == null || appointDate == null || timeSlot == null || timeSlot.isBlank()) return;
        Long count = workOrderMapper.selectCount(Wrappers.<WorkOrder>lambdaQuery()
                .eq(WorkOrder::getAssigneeId, assigneeId)
                .eq(WorkOrder::getAppointDate, appointDate)
                .eq(WorkOrder::getTimeSlot, timeSlot)
                .ne(orderId != null, WorkOrder::getId, orderId)
                .notIn(WorkOrder::getStatus,
                        WorkOrderStatusEnum.COMPLETED.getCode(),
                        WorkOrderStatusEnum.CLOSED.getCode()));
        if (count != null && count > 0) {
            throw new BusinessException("该运维 " + appointDate + " " + timeSlot + " 已有工单，请选择其他时段");
        }
    }

    // ==================== 运维排期 ====================

    @Override
    public List<WorkOrderScheduleDTO> getOperatorSchedule(Long operatorId, LocalDate start, LocalDate end) {
        if (operatorId == null || start == null || end == null) return new ArrayList<>();
        return workOrderMapper.selectList(Wrappers.<WorkOrder>lambdaQuery()
                        .eq(WorkOrder::getAssigneeId, operatorId)
                        .between(WorkOrder::getAppointDate, start, end)
                        .notIn(WorkOrder::getStatus,
                                WorkOrderStatusEnum.COMPLETED.getCode(),
                                WorkOrderStatusEnum.CLOSED.getCode())
                        .isNotNull(WorkOrder::getAppointDate)
                        .isNotNull(WorkOrder::getTimeSlot))
                .stream()
                .map(o -> new WorkOrderScheduleDTO(o.getAppointDate().toString(), o.getTimeSlot()))
                .collect(Collectors.toList());
    }

    private String weekdayText(int dow) {
        String[] names = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        return dow >= 1 && dow <= 7 ? names[dow] : "";
    }

    // ==================== 设备状态同步 ====================

    private void syncDeviceStatus(Long deviceId) {
        if (deviceId == null) return;
        Device device = deviceMapper.selectById(deviceId);
        if (device == null) return;
        if (device.getStatus() != null && device.getStatus() == 3) return;
        Long processing = workOrderMapper.selectCount(Wrappers.<WorkOrder>lambdaQuery()
                .eq(WorkOrder::getDeviceId, deviceId).in(WorkOrder::getStatus, "2", "3", "6"));
        if (processing != null && processing > 0) { updateDeviceStatus(deviceId, 2); return; }
        Long pending = workOrderMapper.selectCount(Wrappers.<WorkOrder>lambdaQuery()
                .eq(WorkOrder::getDeviceId, deviceId).eq(WorkOrder::getStatus, "1"));
        if (pending != null && pending > 0) { updateDeviceStatus(deviceId, 1); return; }
        updateDeviceStatus(deviceId, 0);
    }

    private void updateDeviceStatus(Long deviceId, int status) {
        Device update = new Device();
        update.setId(deviceId);
        update.setStatus(status);
        deviceMapper.updateById(update);
    }

    @Override
    public Map<String, Object> getMySchedule(Long operatorId, LocalDate start, LocalDate end) {
        Map<String, Object> map = new HashMap<>();
        map.put("schedule", getOperatorSchedule(operatorId, start, end));
        SysUser me = listOperators().stream()
                .filter(u -> u.getId() != null && u.getId().equals(operatorId))
                .findFirst()
                .orElse(null);
        List<Integer> workDays = new ArrayList<>();
        if (me != null && me.getWorkDays() != null) {
            for (String s : me.getWorkDays().split(",")) {
                try {
                    workDays.add(Integer.valueOf(s.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        map.put("workDays", workDays);
        return map;
    }

    // ==================== 私有工具方法 ====================

    private WorkOrder getAndCheck(Long orderId) {
        WorkOrder order = workOrderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("工单不存在");
        return order;
    }

    private boolean hasRole(String role) {
        try {
            LoginUser user = SecurityUtils.getLoginUser();
            return user != null && user.getRoles() != null && user.getRoles().contains(role);
        } catch (Exception e) {
            return false;
        }
    }

    private void changeStatus(WorkOrder order, String targetStatus, Long operatorId,
                              String operatorName, WorkOrderActionEnum action, String remark) {
        String from = order.getStatus();
        WorkOrderStateMachine.checkTransition(from, targetStatus);
        order.setStatus(targetStatus);
        if (WorkOrderStatusEnum.COMPLETED.getCode().equals(targetStatus)
                || WorkOrderStatusEnum.CLOSED.getCode().equals(targetStatus)) {
            order.setDepositRefunded(1);
        }
        int rows = workOrderMapper.updateById(order);
        if (rows == 0) throw new BusinessException("操作冲突，工单状态已被他人修改，请刷新后重试");
        saveRecord(order.getId(), operatorId, operatorName, action, from, targetStatus, remark);
        syncDeviceStatus(order.getDeviceId());
    }

    private void saveRecord(Long orderId, Long operatorId, String operatorName,
                            WorkOrderActionEnum action, String from, String to, String remark) {
        WorkOrderRecord record = new WorkOrderRecord();
        record.setOrderId(orderId);
        record.setOperatorId(operatorId);
        record.setOperatorName(operatorName);
        record.setAction(action.getCode());
        record.setFromStatus(from);
        record.setToStatus(to);
        record.setRemark(remark);
        workOrderRecordMapper.insert(record);
    }
}
