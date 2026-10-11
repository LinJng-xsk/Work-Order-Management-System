package com.campus.woms.service;

import com.campus.woms.entity.DeviceCategory;

import java.util.List;

public interface DeviceCategoryService {

    List<DeviceCategory> categoryTree();

    void createCategory(DeviceCategory category);

    void updateCategory(DeviceCategory category);

    void deleteCategory(Long id);
}
