package com.campus.woms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.woms.entity.SysOperationLog;

public interface SysOperationLogService {

    /**
     * AOP 切面调用：异步保存
     */
    void saveLog(SysOperationLog log);

    Page<SysOperationLog> pageLogs(Page<SysOperationLog> page, String module, String operatorName, String operationType, Integer status);
}