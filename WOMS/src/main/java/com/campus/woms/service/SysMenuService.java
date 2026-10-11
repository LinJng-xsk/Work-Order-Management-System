package com.campus.woms.service;

import com.campus.woms.entity.SysMenu;

import java.util.List;

public interface SysMenuService {

    /** 全部菜单树（管理端） */
    List<SysMenu> menuTree();

    void createMenu(SysMenu menu);

    void updateMenu(SysMenu menu);

    void deleteMenu(Long id);

    /** 当前用户可见的菜单树（登录后前端路由用） */
    List<SysMenu> getUserMenuTree(Long userId);
}
