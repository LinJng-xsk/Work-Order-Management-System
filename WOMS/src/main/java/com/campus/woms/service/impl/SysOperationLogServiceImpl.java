package com.campus.woms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.woms.entity.SysOperationLog;
import com.campus.woms.mapper.SysOperationLogMapper;
import com.campus.woms.service.SysOperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SysOperationLogServiceImpl implements SysOperationLogService {

    private final SysOperationLogMapper sysOperationLogMapper;

    @Override
    public void saveLog(SysOperationLog log) {
        sysOperationLogMapper.insert(log);
    }

    @Override
    public Page<SysOperationLog> pageLogs(Page<SysOperationLog> page, String module, String operatorName, String operationType, Integer status) {
        LambdaQueryWrapper<SysOperationLog> qw = Wrappers.lambdaQuery();
        qw.eq(module != null && !module.isBlank(), SysOperationLog::getModule, module)
                .like(operatorName != null && !operatorName.isBlank(), SysOperationLog::getOperatorName, operatorName)
                .eq(operationType != null && !operationType.isBlank(), SysOperationLog::getOperationType, operationType)   // ★ 新增
                .eq(status != null, SysOperationLog::getStatus, status)
                .orderByDesc(SysOperationLog::getCreateTime);
        return sysOperationLogMapper.selectPage(page, qw);
    }
}
