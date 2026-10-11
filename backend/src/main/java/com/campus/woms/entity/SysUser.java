package com.campus.woms.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 用户实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    /** 登录账号 */
    private String username;

    /** 密码（BCrypt），只允许写入、不参与序列化返回 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** 昵称/姓名 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 状态：0正常 1停用 */
    private Integer status;

    /** 运维工作日：1=周一…7=周日，逗号分隔，如 "1,3,5" */
    private String workDays;


    /** 非表字段：分配角色时使用（角色ID集合） */
    @TableField(exist = false)
    @JsonSerialize(contentUsing = ToStringSerializer.class)
    private List<Long> roleIds;

    /** 非表字段：角色编码（列表展示用，来自 sys_user_role + sys_role 联查） */
    @TableField(exist = false)
    private String roleCode;


}
