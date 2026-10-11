package com.campus.woms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.woms.common.exception.BusinessException;
import com.campus.woms.entity.SysUser;
import com.campus.woms.entity.SysRole;

import com.campus.woms.entity.SysUserRole;
import com.campus.woms.mapper.SysRoleMapper;
import com.campus.woms.mapper.SysUserMapper;
import com.campus.woms.mapper.SysUserRoleMapper;
import com.campus.woms.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.campus.woms.entity.WorkOrder;
import com.campus.woms.mapper.WorkOrderMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;   // ← 加在 sysUserRoleMapper 后面
    private final WorkOrderMapper workOrderMapper;

    /** 密码加密器（安全层后续会统一配置为 Bean，这里直接 new 亦可，无状态） */
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** ★ 校验 roleIds 是否包含超级管理员角色 */
    private boolean containsSuperAdmin(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return false;
        for (Long roleId : roleIds) {
            SysRole role = sysRoleMapper.selectById(roleId);
            if (role != null && "super_admin".equals(role.getRoleCode())) {
                return true;
            }
        }
        return false;
    }

    /** ★ 判断某用户是否为超级管理员 */
    private boolean isSuperAdminUser(Long userId) {
        List<String> codes = sysUserMapper.selectRoleCodesByUserId(userId);
        return codes != null && codes.contains("super_admin");
    }

    @Override
    public Page<SysUser> pageUsers(Page<SysUser> page, String keyword, Integer status) {
        LambdaQueryWrapper<SysUser> qw = Wrappers.lambdaQuery();
        qw.like(keyword != null && !keyword.isBlank(), SysUser::getUsername, keyword)
                .eq(status != null, SysUser::getStatus, status)
                .orderByDesc(SysUser::getCreateTime);
        Page<SysUser> result = sysUserMapper.selectPage(page, qw);
        fillRoleCode(result.getRecords());   // ★ 填角色编码
        return result;
    }

    /** 批量填角色编码（复用已有的 selectRoleCodesByUserId） */
    private void fillRoleCode(List<SysUser> records) {
        if (records == null || records.isEmpty()) return;
        for (SysUser u : records) {
            List<String> codes = sysUserMapper.selectRoleCodesByUserId(u.getId());
            if (codes != null && !codes.isEmpty()) {
                u.setRoleCode(codes.get(0));
            }
        }
    }

    //注册功能
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(SysUser user, String rawPassword, String roleCode) {
        // 1. 用户名唯一校验
        Long count = sysUserMapper.selectCount(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, user.getUsername()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }
        // 2. 只允许注册为 用户 / 运维人员
        if (!"employee".equals(roleCode) && !"operator".equals(roleCode)) {
            throw new BusinessException("注册仅支持选择：用户、运维人员");
        }
        // ★ 运维人员必须选择工作日
        if ("operator".equals(roleCode) && (user.getWorkDays() == null || user.getWorkDays().isBlank())) {
            throw new BusinessException("运维人员请选择工作日");
        }

        // 3. 角色编码 → 角色 id
        SysRole role = sysRoleMapper.selectOne(
                Wrappers.<SysRole>lambdaQuery().eq(SysRole::getRoleCode, roleCode));
        if (role == null) {
            throw new BusinessException("角色不存在：" + roleCode);
        }
        // 4. 加密 + 默认状态
        user.setId(null);
        user.setPassword(passwordEncoder.encode(rawPassword));
        if (user.getStatus() == null) {
            user.setStatus(0);
        }
        // 5. 插入用户（雪花 ID 回填）
        sysUserMapper.insert(user);
        // 6. 写入角色关联表
        SysUserRole ur = new SysUserRole();
        ur.setUserId(user.getId());
        ur.setRoleId(role.getId());
        sysUserRoleMapper.insert(ur);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createUser(SysUser user, String rawPassword) {
        // 1. 用户名唯一校验
        Long count = sysUserMapper.selectCount(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, user.getUsername()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }
        // ★ 安全：新增用户不允许分配超级管理员角色
        if (containsSuperAdmin(user.getRoleIds())) {
            throw new BusinessException("超级管理员角色不可通过界面分配");
        }
        // 2. BCrypt 加密 + 默认状态
        user.setId(null);
        user.setPassword(passwordEncoder.encode(rawPassword));
        if (user.getStatus() == null) {
            user.setStatus(0);
        }
        // 3. 插入用户（雪花ID回填到 user.getId()）
        sysUserMapper.insert(user);
        // 4. 分配角色（roleIds 来自非表字段）
        if (user.getRoleIds() != null) {
            for (Long roleId : user.getRoleIds()) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(user.getId());
                ur.setRoleId(roleId);
                sysUserRoleMapper.insert(ur);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(SysUser user) {
        SysUser exist = sysUserMapper.selectById(user.getId());
        if (exist == null) {
            throw new BusinessException("用户不存在");
        }
        // 用户名唯一（排除自己）
        Long count = sysUserMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, user.getUsername())
                .ne(SysUser::getId, user.getId()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }
        user.setPassword(null); // 修改用户不更新密码
        sysUserMapper.updateById(user);
        // ★ 修复：同步更新角色关联表（角色存在 sys_user_role，不在 sys_user 表）
        if (user.getRoleIds() != null) {
            // ★ 安全：不允许把任何人改成超级管理员
            if (containsSuperAdmin(user.getRoleIds())) {
                throw new BusinessException("超级管理员角色不可通过界面分配");
            }
            // ★ 安全：超管账号角色锁定——忽略传入角色，保持原角色不变
            if (!isSuperAdminUser(user.getId())) {
                assignRoles(user.getId(), user.getRoleIds());
            }
        }
    }

    @Override
    public void deleteUser(Long id) {
        // ★ 安全：超级管理员账号不可删除（防止系统失去超管）
        if (isSuperAdminUser(id)) {
            throw new BusinessException("超级管理员账号不可删除");
        }
        sysUserMapper.deleteById(id); // @TableLogic 自动转逻辑删除
    }

    @Override
    public void resetPassword(Long id, String newPassword) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        SysUser update = new SysUser();
        update.setId(id);
        update.setPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(Long userId, List<Long> roleIds) {
        // ★ 安全：统一入口兜底，任何途径都不允许授予超级管理员
        if (containsSuperAdmin(roleIds)) {
            throw new BusinessException("超级管理员角色不可通过界面分配");
        }
        // 先删后插（中间表无逻辑删除字段，这里是物理 DELETE，正确）
        sysUserRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, userId));
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                sysUserRoleMapper.insert(ur);
            }
        }
    }

    @Override
    public SysUser getMyInfo(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        List<String> codes = sysUserMapper.selectRoleCodesByUserId(userId);
        if (codes != null && !codes.isEmpty()) {
            user.setRoleCode(codes.get(0));
        }
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMyInfo(SysUser user, Long userId) {
        SysUser exist = sysUserMapper.selectById(userId);
        if (exist == null) {
            throw new BusinessException("用户不存在");
        }
        SysUser update = new SysUser();
        update.setId(userId);
        update.setNickname(user.getNickname());
        update.setPhone(user.getPhone());
        update.setWorkDays(user.getWorkDays());
        sysUserMapper.updateById(update);

        // ★ 昵称变更 → 同步历史工单快照（报修人 + 处理人）
        String nickname = user.getNickname();
        if (nickname != null && !nickname.isBlank()) {
            WorkOrder w1 = new WorkOrder();
            w1.setReporterName(nickname);
            workOrderMapper.update(w1, Wrappers.<WorkOrder>lambdaUpdate()
                    .eq(WorkOrder::getReporterId, userId));
            WorkOrder w2 = new WorkOrder();
            w2.setAssigneeName(nickname);
            workOrderMapper.update(w2, Wrappers.<WorkOrder>lambdaUpdate()
                    .eq(WorkOrder::getAssigneeId, userId));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMyAccount(Long userId) {
        // ★ 安全：超管账号不可自行注销
        if (isSuperAdminUser(userId)) {
            throw new BusinessException("超级管理员账号不可注销");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        sysUserMapper.deleteById(userId); // @TableLogic 逻辑删除
    }

    @Override
    public SysUser getByUsername(String username) {
        return sysUserMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username));
    }

    @Override
    public List<String> getRoleCodesByUserId(Long userId) {
        return sysUserMapper.selectRoleCodesByUserId(userId);
    }

    @Override
    public List<String> getPermsByUserId(Long userId) {
        return sysUserMapper.selectPermsByUserId(userId);
    }
}
