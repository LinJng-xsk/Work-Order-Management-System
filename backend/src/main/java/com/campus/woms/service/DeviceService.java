package com.campus.woms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.woms.entity.Device;
import com.campus.woms.entity.WorkOrder;

import java.util.List;

public interface DeviceService {

    Page<Device> pageDevices(Page<Device> page, String keyword, Long categoryId, Integer status, Boolean excludeProcessing);

    void createDevice(Device device);

    void updateDevice(Device device);

    void deleteDevice(Long id);

    /** 更新设备状态（在线/离线/维修中/报废） */
    void updateStatus(Long id, Integer status);

    /** 按设备查询历史报修工单（全链路溯源） */
    List<WorkOrder> listWorkOrdersByDevice(Long deviceId);
}
