package com.campus.woms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.woms.dto.DeviceFaultRateDTO;
import com.campus.woms.dto.DeviceFaultTopDTO;
import com.campus.woms.dto.MonthlyOrderStatDTO;
import com.campus.woms.dto.MonthlyOrderDetailDTO;
import com.campus.woms.dto.WorkerWorkloadDTO;
import com.campus.woms.entity.WorkOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单 Mapper：BaseMapper CRUD + 统计报表聚合 SQL
 * 注意：手写 SQL 必须手动加 deleted=0（逻辑删除不自动生效）
 */
@Mapper
public interface WorkOrderMapper extends BaseMapper<WorkOrder> {

    /** 月度工单总量（ECharts 折线/柱状图数据源） */
    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m') AS month, COUNT(*) AS total " +
            "FROM work_order " +
            "WHERE deleted = 0 AND create_time >= #{startTime} " +
            "GROUP BY month ORDER BY month")
    List<MonthlyOrderStatDTO> selectMonthlyOrderStats(@Param("startTime") LocalDateTime startTime);



    /**
     * 按月份统计每天派单明细（day + 运维人员）
     * month 格式：2026-09
     */
    @Select("SELECT DATE_FORMAT(create_time, '%d') AS day, assignee_name AS assigneeName, COUNT(*) AS cnt, " +
            "DATE_FORMAT(appoint_date, '%Y-%m-%d') AS appointDate, MAX(time_slot) AS timeSlot " +
            "FROM work_order " +
            "WHERE deleted = 0 AND DATE_FORMAT(create_time, '%Y-%m') = #{month} " +
            "GROUP BY day, assigneeName, appointDate " +
            "ORDER BY day")
    List<MonthlyOrderDetailDTO> selectMonthlyDetail(@Param("month") String month);



    /** 设备故障率：该设备累计工单数 + 平均每台设备故障次数 */
    @Select("SELECT d.id AS deviceId, d.device_name AS deviceName, " +
            "COUNT(w.id) AS totalCount, " +
            "ROUND(COUNT(w.id) / (SELECT COUNT(*) FROM device WHERE deleted = 0), 4) AS faultRate " +
            "FROM device d " +
            "LEFT JOIN work_order w ON w.device_id = d.id AND w.deleted = 0 " +
            "WHERE d.deleted = 0 " +
            "GROUP BY d.id, d.device_name")
    List<DeviceFaultRateDTO> selectDeviceFaultRate();

    /** 运维人员工作量排行（按处理工单数倒序：处理中/待复核/已完成/已关闭都算） */
    @Select("SELECT assignee_name AS workerName, COUNT(*) AS finishedCount, " +
            "ROUND(AVG(TIMESTAMPDIFF(HOUR, accept_time, finish_time)), 2) AS avgHandleHours " +
            "FROM work_order " +
            "WHERE deleted = 0 AND status IN ('2', '3', '4', '5') AND assignee_id IS NOT NULL " +
            "GROUP BY assignee_id, assignee_name " +
            "ORDER BY finishedCount DESC")
    List<WorkerWorkloadDTO> selectWorkerWorkload();

    /** 故障设备 TOP10（★ 修复：join device 表取设备名） */
    @Select("SELECT d.device_name AS deviceName, COUNT(w.id) AS faultCount " +
            "FROM work_order w " +
            "LEFT JOIN device d ON d.id = w.device_id AND d.deleted = 0 " +
            "WHERE w.deleted = 0 AND w.device_id IS NOT NULL " +
            "GROUP BY w.device_id, d.device_name " +
            "ORDER BY faultCount DESC LIMIT 10")
    List<DeviceFaultTopDTO> selectDeviceFaultTop();

}
