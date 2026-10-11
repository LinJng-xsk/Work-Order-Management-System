package com.campus.woms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.woms.common.result.Result;
import com.campus.woms.dto.WorkOrderMonitorDTO;
import com.campus.woms.security.SecurityUtils;
import com.campus.woms.service.WorkOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 工单监控：仅超级管理员/管理员可见 */
@RestController
@RequestMapping("/workorder/monitor")
@RequiredArgsConstructor
public class WorkOrderMonitorController {

    private final WorkOrderService workOrderService;

    @GetMapping("/page")
    public Result<Page<WorkOrderMonitorDTO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                  @RequestParam(defaultValue = "10") long pageSize,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(required = false) String needType) {
        SecurityUtils.requireRole("super_admin", "manager");
        return Result.ok(workOrderService.monitorPage(Page.of(pageNum, pageSize), status, needType));
    }
}
