package com.campus.woms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.woms.common.exception.BusinessException;
import com.campus.woms.entity.SysRole;
import com.campus.woms.entity.SysRoleMenu;
import com.campus.woms.mapper.SysRoleMapper;
import com.campus.woms.mapper.SysRoleMenuMapper;
import com.campus.woms.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {

    private final SysRoleMapper sysRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;

    @Override
    public Page<SysRole> pageRoles(Page<SysRole> page, String keyword) {
        LambdaQueryWrapper<SysRole> qw = Wrappers.lambdaQuery();
        qw.like(keyword != null && !keyword.isBlank(), SysRole::getRoleName, keyword)
                .orderByAsc(SysRole::getSort);
        return sysRoleMapper.selectPage(page, qw);
    }

    @Override
    public void createRole(SysRole role) {
        Long count = sysRoleMapper.selectCount(
                Wrappers.<SysRole>lambdaQuery().eq(SysRole::getRoleCode, role.getRoleCode()));
        if (count > 0) {
            throw new BusinessException("角色编码已存在");
        }
        role.setId(null);
        sysRoleMapper.insert(role);
    }

    @Override
    public void updateRole(SysRole role) {
        role.setRoleCode(null); // 编码创建后不可改
        sysRoleMapper.updateById(role);
    }

    @Override
    public void deleteRole(Long id) {
        sysRoleMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        sysRoleMenuMapper.delete(
                Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, roleId));
        if (menuIds != null) {
            for (Long menuId : menuIds) {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(roleId);
                rm.setMenuId(menuId);
                sysRoleMenuMapper.insert(rm);
            }
        }
    }
}
