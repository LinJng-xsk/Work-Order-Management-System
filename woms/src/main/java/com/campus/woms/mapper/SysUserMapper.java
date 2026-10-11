package com.campus.woms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.woms.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

import java.util.Map;


/** 用户 Mapper：除 BaseMapper 自带 CRUD 外，提供登录/权限联表查询 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /** 查询用户的角色编码集合（RBAC 登录时用） */
    @Select("SELECT r.role_code FROM sys_role r " +
            "JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.deleted = 0 AND r.status = 0")
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    /** 查询用户的权限标识集合（接口权限 @PreAuthorize 用） */
    @Select("SELECT DISTINCT m.perms FROM sys_menu m " +
            "JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "JOIN sys_user_role ur ON rm.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND m.deleted = 0 AND m.status = 0 " +
            "AND m.perms IS NOT NULL AND m.perms <> ''")
    List<String> selectPermsByUserId(@Param("userId") Long userId);


    /**
     * ★ 运维人员列表（含工作日），供建单/请求/指派弹窗使用
     * 跨 sys_user_role / sys_role 关联，取 role_code='operator' 且未逻辑删除的用户
     */
    @Select("SELECT u.id, u.nickname, u.work_days AS workDays " +
            "FROM sys_user u " +
            "INNER JOIN sys_user_role ur ON ur.user_id = u.id " +
            "INNER JOIN sys_role r ON r.id = ur.role_id " +
            "WHERE r.role_code = 'operator' AND u.deleted = 0")
    List<Map<String, Object>> listOperators();


}
