package com.campus.woms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.woms.entity.SysUser;

import java.util.List;

public interface SysUserService {

    Page<SysUser> pageUsers(Page<SysUser> page, String keyword, Integer status);

    /** 新增用户（密码 BCrypt 加密 + 唯一校验 + 分配角色） */
    void createUser(SysUser user, String rawPassword);

    /** 修改用户（不改密码） */
    void updateUser(SysUser user);

    void deleteUser(Long id);

    void resetPassword(Long id, String newPassword);


    /** 注册（仅支持 用户/运维人员 两种角色） */
    void register(SysUser user, String rawPassword, String roleCode);

    /** 个人详情 */
    SysUser getMyInfo(Long userId);

    /** 编辑个人信息（昵称/手机号/工作日，同步历史工单快照） */
    void updateMyInfo(SysUser user, Long userId);

    /** 注销个人账户（逻辑删除） */
    void deleteMyAccount(Long userId);



    /** 分配角色：先删后插 */
    void assignRoles(Long userId, List<Long> roleIds);

    SysUser getByUsername(String username);

    List<String> getRoleCodesByUserId(Long userId);

    List<String> getPermsByUserId(Long userId);
}
