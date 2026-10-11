package com.campus.woms.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.woms.entity.DeviceLog;
import com.campus.woms.mapper.DeviceLogMapper;
import com.campus.woms.service.DeviceLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeviceLogServiceImpl implements DeviceLogService {

    private final DeviceLogMapper deviceLogMapper;

    @Override
    public void record(String deviceCode, String deviceName,String userName, String action, Long operatorId, String operatorName) {
        DeviceLog log = new DeviceLog();
        log.setDeviceCode(deviceCode);
        log.setDeviceName(deviceName);
        log.setUserName(userName);
        log.setAction(action);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);

        deviceLogMapper.insert(log);
    }

    @Override
    public Page<DeviceLog> pageDeviceLogs(Page<DeviceLog> page, String keyword) {
        return deviceLogMapper.selectPage(page, Wrappers.<DeviceLog>lambdaQuery()
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(DeviceLog::getDeviceCode, keyword)
                        .or()
                        .like(DeviceLog::getDeviceName, keyword))
                .orderByDesc(DeviceLog::getCreateTime));
    }
}
