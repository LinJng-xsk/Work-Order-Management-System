package com.campus.woms.controller;

import com.campus.woms.common.annotation.OperationLog;
import com.campus.woms.common.result.Result;
import com.campus.woms.entity.DeviceCategory;
import com.campus.woms.security.SecurityUtils;
import com.campus.woms.service.DeviceCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/device/category")
@RequiredArgsConstructor
public class DeviceCategoryController {

    private final DeviceCategoryService deviceCategoryService;

    @GetMapping("/tree")
    public Result<List<DeviceCategory>> tree() {
        return Result.ok(deviceCategoryService.categoryTree());
    }

    /** 新增分类 */
    @OperationLog(module = "设备分类", operation = "新增分类")
    @PostMapping
    public Result<Void> create(@RequestBody DeviceCategory category) {
        category.setCreatorId(SecurityUtils.getUserId());   // ★ 新增时写入创建人（编辑走 update，不受影响）
        deviceCategoryService.createCategory(category);
        return Result.ok();
    }

    /** 修改分类 */
    @OperationLog(module = "设备分类", operation = "修改分类")
    @PutMapping
    public Result<Void> update(@RequestBody DeviceCategory category) {
        deviceCategoryService.updateCategory(category);
        return Result.ok();
    }

    /** 删除分类 */
    @OperationLog(module = "设备分类", operation = "删除分类")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        deviceCategoryService.deleteCategory(id);
        return Result.ok();
    }
}
