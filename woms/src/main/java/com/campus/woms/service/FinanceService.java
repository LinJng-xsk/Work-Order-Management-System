package com.campus.woms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.woms.entity.WorkOrder;
import java.util.Map;

public interface FinanceService {

    /** 运维资金概览：累计收入 / 本月收入 / 本月接单数 */
    Map<String, Object> getOverview(Long operatorId);

    /** 运维每日到账明细（最近30天，分页） */
    Page<WorkOrder> getDaily(Long operatorId, Page<WorkOrder> page);

    /** 用户押金概览：在途 / 已退 / 累计缴纳 */
    Map<String, Object> getDeposit(Long reporterId);
}
