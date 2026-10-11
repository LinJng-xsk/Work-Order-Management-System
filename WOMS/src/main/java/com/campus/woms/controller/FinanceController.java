package com.campus.woms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.woms.common.result.Result;
import com.campus.woms.entity.WorkOrder;
import com.campus.woms.security.SecurityUtils;
import com.campus.woms.service.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 资金系统：仅 运维/用户 本人可见，管理员无权 */
@RestController
@RequestMapping("/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;

    /** 运维：资金概览（累计/本月收入/本月接单数） */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        SecurityUtils.requireRole("operator");
        return Result.ok(financeService.getOverview(SecurityUtils.getUserId()));
    }

    /** 运维：每日到账明细（最近30天，分页） */
    @GetMapping("/daily")
    public Result<Page<WorkOrder>> daily(@RequestParam(defaultValue = "1") long pageNum,
                                         @RequestParam(defaultValue = "10") long pageSize) {
        SecurityUtils.requireRole("operator");
        return Result.ok(financeService.getDaily(SecurityUtils.getUserId(), Page.of(pageNum, pageSize)));
    }

    /** 用户：押金概览（在途/已退/累计缴纳） */
    @GetMapping("/deposit")
    public Result<Map<String, Object>> deposit() {
        SecurityUtils.requireRole("employee");
        return Result.ok(financeService.getDeposit(SecurityUtils.getUserId()));
    }
}
