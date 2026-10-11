package com.campus.woms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.woms.common.annotation.OperationLog;
import com.campus.woms.common.result.Result;
import com.campus.woms.entity.Device;
import com.campus.woms.entity.WorkOrder;
import com.campus.woms.security.SecurityUtils;
import com.campus.woms.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/device")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    /** 设备分页（excludeProcessing=true 时排除有未关闭工单的设备，用于新建工单选设备） */
    @GetMapping("/page")
    public Result<Page<Device>> page(@RequestParam(defaultValue = "1") long pageNum,
                                     @RequestParam(defaultValue = "10") long pageSize,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) Long categoryId,
                                     @RequestParam(required = false) Integer status,
                                     @RequestParam(required = false) Boolean excludeProcessing) {
        return Result.ok(deviceService.pageDevices(Page.of(pageNum, pageSize), keyword, categoryId, status, excludeProcessing));
    }

    /** 新增设备 */
    @OperationLog(module = "设备管理", operation = "新增设备")
    @PostMapping
    public Result<Void> create(@RequestBody Device device) {
        device.setCreatorId(SecurityUtils.getUserId());   // ★ 新增时写入创建人（编辑走 update，不受影响）
        deviceService.createDevice(device);
        return Result.ok();
    }

    /** 修改设备 */
    @OperationLog(module = "设备管理", operation = "修改设备")
    @PutMapping
    public Result<Void> update(@RequestBody Device device) {
        deviceService.updateDevice(device);
        return Result.ok();
    }

    /** 删除设备 */
    @OperationLog(module = "设备管理", operation = "删除设备")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return Result.ok();
    }

    /** 更新设备状态（在线/离线/维修中/报废） */
    @OperationLog(module = "设备管理", operation = "更新设备状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        deviceService.updateStatus(id, status);
        return Result.ok();
    }

    /** 按设备查历史报修工单（全链路溯源） */
    @GetMapping("/{id}/workorders")
    public Result<List<WorkOrder>> workOrders(@PathVariable Long id) {
        return Result.ok(deviceService.listWorkOrdersByDevice(id));
    }
}
