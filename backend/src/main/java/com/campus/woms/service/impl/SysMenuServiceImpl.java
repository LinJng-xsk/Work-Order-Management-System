package com.campus.woms.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.campus.woms.common.exception.BusinessException;
import com.campus.woms.entity.SysMenu;
import com.campus.woms.entity.SysRoleMenu;
import com.campus.woms.entity.SysUserRole;
import com.campus.woms.mapper.SysMenuMapper;
import com.campus.woms.mapper.SysRoleMenuMapper;
import com.campus.woms.mapper.SysUserMapper;
import com.campus.woms.mapper.SysUserRoleMapper;
import com.campus.woms.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl implements SysMenuService {

    private final SysMenuMapper sysMenuMapper;
    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;

    @Override
    public List<SysMenu> menuTree() {
        List<SysMenu> all = sysMenuMapper.selectList(
                Wrappers.<SysMenu>lambdaQuery().orderByAsc(SysMenu::getSort));
        return buildTree(all);
    }

    /** 通用树组装：把平铺列表按 parentId 组装成树（菜单、分类共用思路） */
    private List<SysMenu> buildTree(List<SysMenu> all) {
        Map<Long, List<SysMenu>> byParent = all.stream()
                .collect(Collectors.groupingBy(m -> m.getParentId() == null ? 0L : m.getParentId()));
        List<SysMenu> roots = byParent.getOrDefault(0L, new ArrayList<>());
        fillChildren(roots, byParent);
        return roots;
    }

    private void fillChildren(List<SysMenu> parents, Map<Long, List<SysMenu>> byParent) {
        for (SysMenu menu : parents) {
            List<SysMenu> children = byParent.getOrDefault(menu.getId(), new ArrayList<>());
            if (!children.isEmpty()) {
                menu.setChildren(children);
                fillChildren(children, byParent);
            }
        }
    }

    @Override
    public void createMenu(SysMenu menu) {
        menu.setId(null);
        sysMenuMapper.insert(menu);
    }

    @Override
    public void updateMenu(SysMenu menu) {
        sysMenuMapper.updateById(menu);
    }

    @Override
    public void deleteMenu(Long id) {
        sysMenuMapper.deleteById(id);
    }

    @Override
    public List<SysMenu> getUserMenuTree(Long userId) {
        List<String> roleCodes = sysUserMapper.selectRoleCodesByUserId(userId);
        // 超级管理员：返回全部菜单
        List<Long> menuIds;
        if (roleCodes.contains("super_admin")) {
            menuIds = sysMenuMapper.selectList(null).stream()
                    .map(SysMenu::getId).collect(Collectors.toList());
        } else {
            // 普通角色：user_role -> role_menu -> menuId 集合
            List<Long> roleIds = sysUserRoleMapper.selectList(
                            Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId))
                    .stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
            if (roleIds.isEmpty()) {
                return new ArrayList<>();
            }
            menuIds = sysRoleMenuMapper.selectList(
                            Wrappers.<SysRoleMenu>lambdaQuery().in(SysRoleMenu::getRoleId, roleIds))
                    .stream().map(SysRoleMenu::getMenuId).distinct().collect(Collectors.toList());
        }
        if (menuIds.isEmpty()) {
            return new ArrayList<>();
        }
        // 只查菜单/目录类型（不含按钮），组树
        List<SysMenu> menus = sysMenuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                .in(SysMenu::getId, menuIds)
                .ne(SysMenu::getMenuType, 2)
                .orderByAsc(SysMenu::getSort));
        return buildTree(menus);
    }
}
