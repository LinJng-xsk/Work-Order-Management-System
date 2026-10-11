package com.campus.woms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.woms.common.annotation.OperationLog;
import com.campus.woms.common.result.Result;
import com.campus.woms.entity.SysRole;
import com.campus.woms.security.SecurityUtils;
import com.campus.woms.service.SysRoleService;
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

@RestController
@RequestMapping("/system/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService sysRoleService;

    @GetMapping("/page")
    public Result<Page<SysRole>> page(@RequestParam(defaultValue = "1") long pageNum,
                                      @RequestParam(defaultValue = "10") long pageSize,
                                      @RequestParam(required = false) String keyword) {
        SecurityUtils.requireRole("super_admin");
        return Result.ok(sysRoleService.pageRoles(Page.of(pageNum, pageSize), keyword));
    }

    /** 新增角色 */
    @OperationLog(module = "角色管理", operation = "新增角色")
    @PostMapping
    public Result<Void> create(@RequestBody SysRole role) {
        SecurityUtils.requireRole("super_admin");
        sysRoleService.createRole(role);
        return Result.ok();
    }

    /** 修改角色 */
    @OperationLog(module = "角色管理", operation = "修改角色")
    @PutMapping
    public Result<Void> update(@RequestBody SysRole role) {
        SecurityUtils.requireRole("super_admin");
        sysRoleService.updateRole(role);
        return Result.ok();
    }

    /** 删除角色 */
    @OperationLog(module = "角色管理", operation = "删除角色")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        SecurityUtils.requireRole("super_admin");
        sysRoleService.deleteRole(id);
        return Result.ok();
    }

    /** 分配菜单权限（body 用 menuIds 字段） */
    @OperationLog(module = "角色管理", operation = "分配菜单权限")
    @PutMapping("/{id}/menus")
    public Result<Void> assignMenus(@PathVariable Long id, @RequestBody SysRole role) {
        SecurityUtils.requireRole("super_admin");
        sysRoleService.assignMenus(id, role.getMenuIds());
        return Result.ok();
    }
}
