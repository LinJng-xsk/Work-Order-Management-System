package com.campus.woms.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.woms.entity.DeviceCategory;
import com.campus.woms.mapper.DeviceCategoryMapper;
import com.campus.woms.security.SecurityUtils;
import com.campus.woms.service.DeviceCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeviceCategoryServiceImpl implements DeviceCategoryService {

    private final DeviceCategoryMapper deviceCategoryMapper;

    @Override
    public List<DeviceCategory> categoryTree() {
        List<DeviceCategory> all = deviceCategoryMapper.selectList(
                Wrappers.<DeviceCategory>lambdaQuery().orderByAsc(DeviceCategory::getSort));
        Map<Long, List<DeviceCategory>> byParent = all.stream()
                .collect(Collectors.groupingBy(c -> c.getParentId() == null ? 0L : c.getParentId()));
        List<DeviceCategory> roots = byParent.getOrDefault(0L, new ArrayList<>());
        fillChildren(roots, byParent);
        return roots;
    }

    private void fillChildren(List<DeviceCategory> parents, Map<Long, List<DeviceCategory>> byParent) {
        for (DeviceCategory c : parents) {
            List<DeviceCategory> children = byParent.getOrDefault(c.getId(), new ArrayList<>());
            if (!children.isEmpty()) {
                c.setChildren(children);
                fillChildren(children, byParent);
            }
        }
    }

    @Override
    public void createCategory(DeviceCategory category) {
        category.setId(null);
        // ★ 创建人ID：决定前端"仅自己添加的可编辑"归属
        category.setCreatorId(SecurityUtils.getUserId());
        deviceCategoryMapper.insert(category);
    }

    @Override
    public void updateCategory(DeviceCategory category) {
        deviceCategoryMapper.updateById(category);
    }

    @Override
    public void deleteCategory(Long id) {
        deviceCategoryMapper.deleteById(id);
    }
}
