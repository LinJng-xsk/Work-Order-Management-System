package com.campus.woms.service;

import com.campus.woms.dto.DeviceFaultRateDTO;
import com.campus.woms.dto.DeviceFaultTopDTO;
import com.campus.woms.dto.MonthlyOrderStatDTO;
import com.campus.woms.dto.MonthlyOrderDetailDTO;
import com.campus.woms.dto.WorkerWorkloadDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface StatisticsService {

    List<MonthlyOrderStatDTO> monthlyOrderStats(LocalDateTime startTime);

    List<MonthlyOrderDetailDTO> monthlyOrderDetail(String month);

    List<DeviceFaultRateDTO> deviceFaultRate();

    List<WorkerWorkloadDTO> workerWorkload();

    List<DeviceFaultTopDTO> deviceFaultTop();
}
