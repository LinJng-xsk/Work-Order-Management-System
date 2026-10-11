package com.campus.woms.service.impl;

import com.campus.woms.dto.DeviceFaultRateDTO;
import com.campus.woms.dto.DeviceFaultTopDTO;
import com.campus.woms.dto.MonthlyOrderStatDTO;
import com.campus.woms.dto.MonthlyOrderDetailDTO;
import com.campus.woms.dto.WorkerWorkloadDTO;
import com.campus.woms.mapper.WorkOrderMapper;
import com.campus.woms.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final WorkOrderMapper workOrderMapper;

    @Override
    public List<MonthlyOrderStatDTO> monthlyOrderStats(LocalDateTime startTime) {
        return workOrderMapper.selectMonthlyOrderStats(startTime);
    }

    @Override
    public List<MonthlyOrderDetailDTO> monthlyOrderDetail(String month) {
        return workOrderMapper.selectMonthlyDetail(month);
    }

    @Override
    public List<DeviceFaultRateDTO> deviceFaultRate() {
        return workOrderMapper.selectDeviceFaultRate();
    }

    @Override
    public List<WorkerWorkloadDTO> workerWorkload() {
        return workOrderMapper.selectWorkerWorkload();
    }

    @Override
    public List<DeviceFaultTopDTO> deviceFaultTop() {
        return workOrderMapper.selectDeviceFaultTop();
    }
}
