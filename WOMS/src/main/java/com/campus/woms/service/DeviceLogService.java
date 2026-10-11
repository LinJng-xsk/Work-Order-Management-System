package com.campus.woms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.woms.entity.DeviceLog;

public interface DeviceLogService {

    /** 记录设备操作日志 */
//    void record(String deviceCode, String deviceName, String action, Long operatorId, String operatorName);
    void record(String deviceCode, String deviceName, String userName, String action, Long operatorId, String operatorName);

    /** 分页查询（支持按编号/名称搜索） */
    Page<DeviceLog> pageDeviceLogs(Page<DeviceLog> page, String keyword);
}
