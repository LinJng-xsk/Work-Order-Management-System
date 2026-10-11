package com.campus.woms.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.woms.common.annotation.OperationLog;
import com.campus.woms.common.result.Result;
import com.campus.woms.dto.ResetPasswordDTO;
import com.campus.woms.entity.SysUser;
import com.campus.woms.security.SecurityUtils;
import com.campus.woms.service.SysUserService;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;

    /** 用户分页（仅管理员） */
    @GetMapping("/page")
    public Result<Page<SysUser>> page(@RequestParam(defaultValue = "1") long pageNum,
                                      @RequestParam(defaultValue = "10") long pageSize,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) Integer status) {
        SecurityUtils.requireRole("super_admin");
        return Result.ok(sysUserService.pageUsers(Page.of(pageNum, pageSize), keyword, status));
    }

    /** 新增用户（body 里带 password 明文 + roleIds） */
    @OperationLog(module = "用户管理", operation = "新增用户")
    @PostMapping
    public Result<Void> create(@RequestBody SysUser user) {
        SecurityUtils.requireRole("super_admin");
        sysUserService.createUser(user, user.getPassword());
        return Result.ok();
    }

    /** 修改用户 */
    @OperationLog(module = "用户管理", operation = "修改用户")
    @PutMapping
    public Result<Void> update(@RequestBody SysUser user) {
        SecurityUtils.requireRole("super_admin");
        sysUserService.updateUser(user);
        return Result.ok();
    }

    /** 删除用户（逻辑删除） */
    @OperationLog(module = "用户管理", operation = "删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        SecurityUtils.requireRole("super_admin");
        sysUserService.deleteUser(id);
        return Result.ok();
    }

    /** 重置密码 */
    @OperationLog(module = "用户管理", operation = "重置密码")
    @PutMapping("/{id}/resetPwd")
    public Result<Void> resetPwd(@PathVariable Long id, @RequestBody ResetPasswordDTO dto) {
        SecurityUtils.requireRole("super_admin");
        sysUserService.resetPassword(id, dto.getPassword());
        return Result.ok();
    }

    /** 分配角色（body 用 roleIds 字段） */
    @OperationLog(module = "用户管理", operation = "分配角色")
    @PutMapping("/{id}/roles")
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody SysUser user) {
        SecurityUtils.requireRole("super_admin");
        sysUserService.assignRoles(id, user.getRoleIds());
        return Result.ok();
    }

    /** 当前登录用户信息（前端右上角 + 路由用） */
    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", SecurityUtils.getUserId());
        map.put("username", SecurityUtils.getUsername());
        map.put("roles", sysUserService.getRoleCodesByUserId(SecurityUtils.getUserId()));
        map.put("perms", sysUserService.getPermsByUserId(SecurityUtils.getUserId()));
        return Result.ok(map);
    }




    /** ★ 个人中心：我的详情 */
    @GetMapping("/me")
    public Result<SysUser> me() {
        return Result.ok(sysUserService.getMyInfo(SecurityUtils.getUserId()));
    }

    /** ★ 个人中心：编辑我的信息（昵称/手机号/工作日，同步历史工单） */
    @OperationLog(module = "用户管理", operation = "编辑个人信息")
    @PutMapping("/me")
    public Result<Void> updateMe(@RequestBody SysUser user) {
        sysUserService.updateMyInfo(user, SecurityUtils.getUserId());
        return Result.ok();
    }

    /** ★ 个人中心：注销账户（仅 用户/运维） */
    @OperationLog(module = "用户管理", operation = "注销账户")
    @DeleteMapping("/me")
    public Result<Void> deleteMe() {
        SecurityUtils.requireRole("employee", "operator");
        sysUserService.deleteMyAccount(SecurityUtils.getUserId());
        return Result.ok();
    }

}
