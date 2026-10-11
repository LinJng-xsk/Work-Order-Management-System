// security/UserDetailsServiceImpl.java
package com.campus.woms.security;

import com.campus.woms.entity.SysUser;
import com.campus.woms.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;

/** 登录时按用户名加载用户 + 角色 + 权限 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserService sysUserService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserService.getByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setPassword(user.getPassword());
        loginUser.setStatus(user.getStatus());
        loginUser.setRoles(new HashSet<>(sysUserService.getRoleCodesByUserId(user.getId())));
        loginUser.setPermissions(new HashSet<>(sysUserService.getPermsByUserId(user.getId())));
        return loginUser;
    }
}
