package com.campus.woms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.woms.common.annotation.OperationLog;
import com.campus.woms.common.exception.BusinessException;
import com.campus.woms.common.result.Result;
import com.campus.woms.dto.ReviewOrderDTO;
import com.campus.woms.dto.SubmitReviewDTO;
import com.campus.woms.entity.SysUser;
import com.campus.woms.entity.WorkOrder;
import com.campus.woms.entity.WorkOrderRecord;
import com.campus.woms.security.LoginUser;
import com.campus.woms.security.SecurityUtils;
import com.campus.woms.service.WorkOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import com.campus.woms.dto.WorkOrderScheduleDTO;

import java.util.Map;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/workorder")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    /** 提交工单（可指定运维+预约，也可不指定进公共池） */
    @OperationLog(module = "工单管理", operation = "提交工单")
    @PostMapping
    public Result<WorkOrder> create(@RequestBody WorkOrder order) {
        return Result.ok(workOrderService.createOrder(order,
                SecurityUtils.getUserId(), SecurityUtils.getUsername()));
    }

    /**
     * 工单分页：
     *  myOrders=true     → 只查指派给我的（我的接单）
     *  myReporter=true   → 只查我报修的（我的工单）
     *  employeePool=true → 员工工单管理：待接单池 ∪ 自己报修的全部（逻辑在 ServiceImpl）
     *  excludeMyAssigned=true → 运维接单大厅：排除"指派给我"的单
     *  sortByStatus=true → 按状态分组排序（工单管理页）
     */
    @OperationLog(module = "工单管理", operation = "分页查询工单")
    @GetMapping("/page")
    public Result<Page<WorkOrder>> page(@RequestParam(defaultValue = "1") long pageNum,
                                        @RequestParam(defaultValue = "10") long pageSize,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) String keywordNo,
                                        @RequestParam(required = false) String keywordTitle,
                                        @RequestParam(required = false) String keywordDesc,
                                        @RequestParam(required = false) Boolean myOrders,
                                        @RequestParam(required = false) Boolean myReporter,
                                        @RequestParam(required = false) Boolean sortByStatus,
                                        // ★ 运维接单大厅：true 时排除"指派给我"的单（我在我的接单里处理）
                                        @RequestParam(required = false) Boolean excludeMyAssigned,
                                        // ★ 员工视图：待接单池 ∪ 自己的单
                                        @RequestParam(required = false) Boolean employeePool) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        Long assigneeId = null;
        Long reporterId = null;
        if (Boolean.TRUE.equals(myOrders) && loginUser != null) {
            assigneeId = loginUser.getUserId();
        }
        if (Boolean.TRUE.equals(myReporter) && loginUser != null) {
            reporterId = loginUser.getUserId();
        }
        Long excludeAssigneeId = null;
        if (Boolean.TRUE.equals(excludeMyAssigned) && loginUser != null) {
            excludeAssigneeId = loginUser.getUserId();
        }
        return Result.ok(workOrderService.pageOrders(Page.of(pageNum, pageSize),
                reporterId, assigneeId, status, keyword,
                keywordNo, keywordTitle, keywordDesc,
                Boolean.TRUE.equals(sortByStatus), excludeAssigneeId,
                Boolean.TRUE.equals(employeePool)));
    }

    /** ★ 运维列表（全员可访问，用于建单/请求选择，含工作日） */
    @GetMapping("/operators")
    public Result<List<SysUser>> operators() {
        return Result.ok(workOrderService.listOperators());
    }

    @GetMapping("/{id}")
    public Result<WorkOrder> detail(@PathVariable Long id) {
        return Result.ok(workOrderService.getOrderDetail(id));
    }

    @GetMapping("/{id}/records")
    public Result<List<WorkOrderRecord>> records(@PathVariable Long id) {
        return Result.ok(workOrderService.listRecords(id));
    }

    /** 删除工单（管理角色 + 用户自己的单） */
    @OperationLog(module = "工单管理", operation = "删除工单")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        LoginUser user = SecurityUtils.getLoginUser();
        boolean admin = user.getRoles().contains("super_admin") || user.getRoles().contains("manager");
        WorkOrder order = workOrderService.getOrderDetail(id);
        if (!admin && !user.getUserId().equals(order.getReporterId())) {
            throw new BusinessException("只能删除自己提交的工单");
        }
        workOrderService.deleteOrder(id);
        return Result.ok();
    }

    /** ★ 编辑自己的待接单工单（标题/描述/需求情况） */
    @OperationLog(module = "工单管理", operation = "编辑工单")
    @PutMapping("/{id}/edit")
    public Result<Void> edit(@PathVariable Long id,
                             @RequestParam(required = false) String title,
                             @RequestParam(required = false) String description,
                             @RequestParam(required = false) String needType) {
        workOrderService.editOrder(id, title, description, needType, SecurityUtils.getUserId());
        return Result.ok();
    }

    /** 接单确认（必须带预约日期+时段） */
    @OperationLog(module = "工单管理", operation = "接单")
    @PutMapping("/{id}/accept")
    public Result<Void> accept(@PathVariable Long id,
                               @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate appointDate,
                               @RequestParam String timeSlot) {
        SecurityUtils.requireRole("super_admin", "manager", "operator");
        workOrderService.accept(id, appointDate, timeSlot,
                SecurityUtils.getUserId(), SecurityUtils.getUsername());
        return Result.ok();
    }

    /** 指派/请求处理人（四角色可调；指定后仍待接单，等运维确认） */
    @OperationLog(module = "工单管理", operation = "指派处理人")
    @PutMapping("/{id}/assign")
    public Result<Void> assign(@PathVariable Long id,
                               @RequestParam Long assigneeId,
                               @RequestParam String assigneeName,
                               @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate appointDate,
                               @RequestParam String timeSlot) {
        LoginUser user = SecurityUtils.getLoginUser();
        SecurityUtils.requireRole("super_admin", "manager", "operator", "employee");
        boolean admin = user.getRoles().contains("super_admin") || user.getRoles().contains("manager");
        if (!admin) {
            WorkOrder order = workOrderService.getOrderDetail(id);
            if (order.getReporterId() == null || !order.getReporterId().equals(user.getUserId())) {
                throw new BusinessException("只能请求自己工单的处理人");
            }
        }
        workOrderService.assign(id, assigneeId, assigneeName, appointDate, timeSlot,
                SecurityUtils.getUserId(), SecurityUtils.getUsername());
        return Result.ok();
    }

    /** ★ 运维排期：派遣/请求/接单弹窗用，返回占用时段 */
    @GetMapping("/schedule")
    public Result<List<WorkOrderScheduleDTO>> schedule(@RequestParam Long operatorId,
                                                       @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
                                                       @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        return Result.ok(workOrderService.getOperatorSchedule(operatorId, start, end));
    }

    /** ★ 当前登录运维的排期 + 工作日（接单弹窗用） */
    @GetMapping("/schedule/mine")
    public Result<Map<String, Object>> mySchedule(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        return Result.ok(workOrderService.getMySchedule(SecurityUtils.getUserId(), start, end));
    }

    /** 运维拒单：回待接单池，该运维从请求列表剔除 */
    @OperationLog(module = "工单管理", operation = "拒绝接单")
    @PutMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id,
                               @RequestParam(required = false) String reason) {
        SecurityUtils.requireRole("operator", "super_admin", "manager");
        workOrderService.reject(id, reason, SecurityUtils.getUserId(), SecurityUtils.getUsername());
        return Result.ok();
    }

    /** 用户取消接单 */
    @OperationLog(module = "工单管理", operation = "取消接单")
    @PutMapping("/{id}/cancelAssign")
    public Result<Void> cancelAssign(@PathVariable Long id) {
        workOrderService.cancelAssign(id, SecurityUtils.getUserId(), SecurityUtils.getUsername());
        return Result.ok();
    }

    /** 拒过该工的运维ID列表（用户请求时排除） */
    @GetMapping("/{id}/rejectedOperators")
    public Result<List<Long>> rejectedOperators(@PathVariable Long id) {
        return Result.ok(workOrderService.listRejectedOperatorIds(id));
    }

    /** 提交复核 */
    @OperationLog(module = "工单管理", operation = "提交复核")
    @PutMapping("/{id}/submitReview")
    public Result<Void> submitReview(@PathVariable Long id, @Valid @RequestBody SubmitReviewDTO dto) {
        SecurityUtils.requireRole("super_admin", "manager", "operator");
        workOrderService.submitReview(id, dto.getRepairContent(), dto.getRepairCost(),
                SecurityUtils.getUserId(), SecurityUtils.getUsername());
        return Result.ok();
    }

    /** 验收评价（报修人 + 管理角色） */
    @OperationLog(module = "工单管理", operation = "验收评价")
    @PutMapping("/{id}/review")
    public Result<Void> review(@PathVariable Long id, @Valid @RequestBody ReviewOrderDTO dto) {
        SecurityUtils.requireRole("employee", "super_admin", "manager");
        workOrderService.review(id, dto.getScore(), dto.getComment(), dto.getPass(),
                SecurityUtils.getUserId(), SecurityUtils.getUsername());
        return Result.ok();
    }

    /** 重新打开（仅兼容历史"已驳回"数据，管理角色） */
    @OperationLog(module = "工单管理", operation = "重新打开工单")
    @PutMapping("/{id}/reopen")
    public Result<Void> reopen(@PathVariable Long id) {
        SecurityUtils.requireRole("super_admin", "manager");
        workOrderService.reopen(id, SecurityUtils.getUserId(), SecurityUtils.getUsername());
        return Result.ok();
    }

    /** 关闭 */
    @OperationLog(module = "工单管理", operation = "关闭工单")
    @PutMapping("/{id}/close")
    public Result<Void> close(@PathVariable Long id) {
        SecurityUtils.requireRole("super_admin", "manager");
        workOrderService.close(id, SecurityUtils.getUserId(), SecurityUtils.getUsername());
        return Result.ok();
    }

    /** 报废：管理角色全权；运维仅自己接的单（Service 校验） */
    @OperationLog(module = "工单管理", operation = "报废")
    @PutMapping("/{id}/scrap")
    public Result<Void> scrap(@PathVariable Long id) {
        workOrderService.scrap(id, SecurityUtils.getUserId(), SecurityUtils.getUsername());
        return Result.ok();
    }
}
