package com.campus.woms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.woms.common.result.Result;
import com.campus.woms.entity.SysOperationLog;
import com.campus.woms.security.SecurityUtils;
import com.campus.woms.service.SysOperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/log")
@RequiredArgsConstructor
public class SysOperationLogController {

    private final SysOperationLogService sysOperationLogService;

    @GetMapping("/page")
    public Result<Page<SysOperationLog>> page(@RequestParam(defaultValue = "1") long pageNum,
                                              @RequestParam(defaultValue = "10") long pageSize,
                                              @RequestParam(required = false) String module,
                                              @RequestParam(required = false) String operatorName,
                                              @RequestParam(required = false) String operationType,   // ★ 新增
                                              @RequestParam(required = false) Integer status) {
        SecurityUtils.requireRole("super_admin");
        return Result.ok(sysOperationLogService.pageLogs(Page.of(pageNum, pageSize), module, operatorName, operationType, status));
    }
}
