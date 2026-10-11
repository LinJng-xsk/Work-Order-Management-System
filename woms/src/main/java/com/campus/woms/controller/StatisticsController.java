package com.campus.woms.controller;

import com.campus.woms.common.result.Result;
import com.campus.woms.dto.DeviceFaultRateDTO;
import com.campus.woms.dto.DeviceFaultTopDTO;
import com.campus.woms.dto.MonthlyOrderStatDTO;
import com.campus.woms.dto.MonthlyOrderDetailDTO;
import com.campus.woms.dto.WorkerWorkloadDTO;
import com.campus.woms.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/** 统计接口：数据源给前端 ECharts */
@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    /** 月度工单总量（默认近 12 个月） */
    @GetMapping("/order/monthly")
    public Result<List<MonthlyOrderStatDTO>> monthlyOrderStats(
            @RequestParam(required = false) LocalDateTime startTime) {
        LocalDateTime start = startTime == null ? LocalDateTime.now().minusMonths(11) : startTime;
        return Result.ok(statisticsService.monthlyOrderStats(start));
    }

    /**
     * 月度派单明细：某月中每天哪些运维人员处理了工单
     */
    @GetMapping("/order/monthlyDetail")
    public Result<List<MonthlyOrderDetailDTO>> monthlyOrderDetail(@RequestParam("month") String month) {
        return Result.ok(statisticsService.monthlyOrderDetail(month));
    }

    /** 设备故障率 */
    @GetMapping("/device/faultRate")
    public Result<List<DeviceFaultRateDTO>> deviceFaultRate() {
        return Result.ok(statisticsService.deviceFaultRate());
    }

    /** 运维人员工作量排行 */
    @GetMapping("/worker/workload")
    public Result<List<WorkerWorkloadDTO>> workerWorkload() {
        return Result.ok(statisticsService.workerWorkload());
    }

    /** 故障设备 TOP */
    @GetMapping("/device/top")
    public Result<List<DeviceFaultTopDTO>> deviceFaultTop() {
        return Result.ok(statisticsService.deviceFaultTop());
    }
}
