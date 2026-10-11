package com.campus.woms.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 角色实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    /** 角色名称 */
    private String roleName;

    /** 角色编码：super_admin / operator / employee */
    private String roleCode;

    /** 显示顺序 */
    private Integer sort;

    /** 状态：0正常 1停用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 非表字段：分配菜单权限时使用（菜单ID集合） */
    @TableField(exist = false)
    @JsonSerialize(contentUsing = ToStringSerializer.class)
    private List<Long> menuIds;
}
