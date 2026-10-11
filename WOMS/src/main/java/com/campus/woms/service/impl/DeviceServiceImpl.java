package com.campus.woms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.woms.common.exception.BusinessException;
import com.campus.woms.entity.Device;
import com.campus.woms.entity.DeviceCategory;
import com.campus.woms.entity.SysUser;
import com.campus.woms.entity.WorkOrder;
import com.campus.woms.enums.DeviceStatusEnum;
import com.campus.woms.mapper.DeviceCategoryMapper;
import com.campus.woms.mapper.DeviceMapper;
import com.campus.woms.mapper.SysUserMapper;
import com.campus.woms.mapper.WorkOrderMapper;
import com.campus.woms.service.DeviceLogService;
import com.campus.woms.service.DeviceService;
import com.campus.woms.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceMapper deviceMapper;
    private final WorkOrderMapper workOrderMapper;
    private final DeviceCategoryMapper deviceCategoryMapper;
    private final DeviceLogService deviceLogService;
    // ★ 新增：查当前登录用户昵称用
    private final SysUserMapper sysUserMapper;

    @Override
    public Page<Device> pageDevices(Page<Device> page, String keyword, Long categoryId, Integer status, Boolean excludeProcessing) {
        LambdaQueryWrapper<Device> qw = Wrappers.lambdaQuery();
        qw.and(keyword != null && !keyword.isBlank(), w -> w
                        .like(Device::getDeviceName, keyword)
                        .or()
                        .like(Device::getDeviceCode, keyword))
                .eq(categoryId != null, Device::getCategoryId, categoryId)
                .eq(status != null, Device::getStatus, status)
                .orderByDesc(Device::getCreateTime);
        // ★ 新建工单选设备：排除有未关闭工单的设备（工单全部已关闭 或 从未报修过 才可选）
        if (Boolean.TRUE.equals(excludeProcessing)) {
            qw.apply("NOT EXISTS (SELECT 1 FROM work_order w WHERE w.device_id = device.id AND w.deleted = 0 AND w.status <> '5')");
        }
        Page<Device> result = deviceMapper.selectPage(page, qw);
        fillCategoryName(result.getRecords());
        return result;
    }

    private void fillCategoryName(List<Device> records) {
        if (records == null || records.isEmpty()) return;
        Set<Long> categoryIds = records.stream()
                .map(Device::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (categoryIds.isEmpty()) return;
        Map<Long, String> nameMap = deviceCategoryMapper.selectBatchIds(categoryIds).stream()
                .collect(Collectors.toMap(DeviceCategory::getId, DeviceCategory::getCategoryName));
        records.forEach(d -> d.setCategoryName(nameMap.get(d.getCategoryId())));
    }

    @Override
    public void createDevice(Device device) {
        Long count = deviceMapper.selectCount(
                Wrappers.<Device>lambdaQuery().eq(Device::getDeviceCode, device.getDeviceCode()));
        if (count > 0) {
            throw new BusinessException("设备编号已存在");
        }
        // ★ 安装位置必填
        if (device.getLocation() == null || device.getLocation().isBlank()) {
            throw new BusinessException("安装位置不能为空");
        }
        device.setId(null);
        // ★ 使用人强制绑定建单人昵称（覆盖前端传值；编辑时可再改）
        device.setUserName(currentNickname());
        // ★ 创建人ID：决定前端"仅自己添加的可编辑"归属（修复 creatorId 为 null 的 bug）
        device.setCreatorId(currentOperatorId());
        if (device.getStatus() == null) {
            device.setStatus(DeviceStatusEnum.ONLINE.getCode());
        }
        try {
            deviceMapper.insert(device);
        } catch (DuplicateKeyException e) {
            throw new BusinessException("设备编号已存在，请更换编号");
        }
        deviceLogService.record(device.getDeviceCode(), device.getDeviceName(), device.getUserName(), "create", currentOperatorId(), currentUsername());
    }

    @Override
    public void updateDevice(Device device) {
        // ★ 安装位置必填（编辑同样校验）
        if (device.getLocation() == null || device.getLocation().isBlank()) {
            throw new BusinessException("安装位置不能为空");
        }

        Device exist = deviceMapper.selectById(device.getId());
        if (exist == null) {
            throw new BusinessException("设备不存在");
        }
        device.setDeviceCode(null); // 编号不可改
        deviceMapper.updateById(device);
        deviceLogService.record(
                exist.getDeviceCode(),
                device.getDeviceName() != null ? device.getDeviceName() : exist.getDeviceName(),
                device.getUserName() != null ? device.getUserName() : exist.getUserName(),
                "update",
                currentOperatorId(), currentUsername());
    }

    @Override
    public void deleteDevice(Long id) {
        Device device = deviceMapper.selectById(id);
        deviceMapper.deleteById(id);
        if (device != null) {
            deviceLogService.record(device.getDeviceCode(), device.getDeviceName(), device.getUserName(), "delete", currentOperatorId(), currentUsername());
        }
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        for (DeviceStatusEnum e : DeviceStatusEnum.values()) {
            if (e.getCode().equals(status)) {
                Device update = new Device();
                update.setId(id);
                update.setStatus(status);
                deviceMapper.updateById(update);
                return;
            }
        }
        throw new BusinessException("非法的设备状态值：" + status);
    }

    @Override
    public List<WorkOrder> listWorkOrdersByDevice(Long deviceId) {
        return workOrderMapper.selectList(Wrappers.<WorkOrder>lambdaQuery()
                .eq(WorkOrder::getDeviceId, deviceId)
                .orderByDesc(WorkOrder::getCreateTime));
    }

    // ===== 当前登录用户（principal 是 LoginUser 对象）=====
    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof LoginUser loginUser) {
            return loginUser.getUsername();
        }
        return principal instanceof String s ? s : null;
    }

    private Long currentOperatorId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof LoginUser loginUser) {
            return loginUser.getUserId();
        }
        return null;
    }

    /** ★ 当前登录用户昵称（新增设备时绑定为使用人，LoginUser 无昵称字段，查库取） */
    private String currentNickname() {
        Long userId = currentOperatorId();
        if (userId == null) return null;
        SysUser u = sysUserMapper.selectById(userId);
        return u == null ? null : u.getNickname();
    }
}
