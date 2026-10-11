package com.campus.woms.controller;

import com.campus.woms.common.annotation.OperationLog;
import com.campus.woms.common.result.Result;
import com.campus.woms.entity.SysMenu;
import com.campus.woms.security.SecurityUtils;
import com.campus.woms.service.SysMenuService;
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
@RequestMapping("/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService sysMenuService;

    /** 全部菜单树（管理端） */
    @GetMapping("/tree")
    public Result<List<SysMenu>> tree() {
        SecurityUtils.requireRole("super_admin");
        return Result.ok(sysMenuService.menuTree());
    }

    /** 当前登录用户的菜单树（前端动态路由） */
    @GetMapping("/userTree")
    public Result<List<SysMenu>> userTree() {
        return Result.ok(sysMenuService.getUserMenuTree(SecurityUtils.getUserId()));
    }

    /** 新增菜单 */
    @OperationLog(module = "菜单管理", operation = "新增菜单")
    @PostMapping
    public Result<Void> create(@RequestBody SysMenu menu) {
        SecurityUtils.requireRole("super_admin");
        sysMenuService.createMenu(menu);
        return Result.ok();
    }

    /** 修改菜单 */
    @OperationLog(module = "菜单管理", operation = "修改菜单")
    @PutMapping
    public Result<Void> update(@RequestBody SysMenu menu) {
        SecurityUtils.requireRole("super_admin");
        sysMenuService.updateMenu(menu);
        return Result.ok();
    }

    /** 删除菜单 */
    @OperationLog(module = "菜单管理", operation = "删除菜单")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        SecurityUtils.requireRole("super_admin");
        sysMenuService.deleteMenu(id);
        return Result.ok();
    }
}
