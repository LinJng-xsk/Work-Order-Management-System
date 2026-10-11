package com.campus.woms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.woms.common.result.Result;
import com.campus.woms.entity.DeviceLog;
import com.campus.woms.service.DeviceLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/devicelog")
@RequiredArgsConstructor
public class DeviceLogController {

    private final DeviceLogService deviceLogService;

    @GetMapping("/page")
    public Result<Page<DeviceLog>> page(@RequestParam(defaultValue = "1") long pageNum,
                                        @RequestParam(defaultValue = "10") long pageSize,
                                        @RequestParam(required = false) String keyword) {
        return Result.ok(deviceLogService.pageDeviceLogs(Page.of(pageNum, pageSize), keyword));
    }
}
