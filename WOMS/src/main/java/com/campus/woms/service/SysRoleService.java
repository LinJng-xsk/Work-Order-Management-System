package com.campus.woms.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.woms.entity.SysRole;

import java.util.List;

public interface SysRoleService {

    Page<SysRole> pageRoles(Page<SysRole> page, String keyword);

    void createRole(SysRole role);

    void updateRole(SysRole role);

    void deleteRole(Long id);

    /** 分配菜单权限：先删后插 */
    void assignMenus(Long roleId, List<Long> menuIds);
}
