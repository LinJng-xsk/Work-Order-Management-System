// security/LoginUser.java
package com.campus.woms.security;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 登录用户主体：认证通过后放入 SecurityContext */
@Data
public class LoginUser implements UserDetails {

    private Long userId;
    private String username;
    private String password;
    private Integer status;                 // 0正常 1停用
    private Set<String> roles;              // 角色编码集合
    private Set<String> permissions;        // 权限标识集合

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 权限标识 -> GrantedAuthority，@PreAuthorize("hasAuthority('xxx')") 依赖这里
        return permissions == null ? List.of()
                : permissions.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return status != null && status == 0; }
}
