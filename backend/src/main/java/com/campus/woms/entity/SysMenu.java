package com.campus.woms.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 菜单/权限实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    /** 父菜单ID，0为顶级 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    /** 菜单名称 */
    private String menuName;

    /** 类型：0目录 1菜单 2按钮 */
    private Integer menuType;

    /** 路由地址 */
    private String path;

    /** 组件路径 */
    private String component;

    /** 权限标识，如 system:user:add */
    private String perms;

    /** 图标 */
    private String icon;

    /** 显示顺序 */
    private Integer sort;

    /** 状态：0正常 1停用 */
    private Integer status;

    /** 非表字段：子菜单（菜单树） */
    @TableField(exist = false)
    private List<SysMenu> children;
}
