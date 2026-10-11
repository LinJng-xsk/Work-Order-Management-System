// security/SecurityUtils.java
package com.campus.woms.security;

import com.campus.woms.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** 当前登录用户工具：Controller 里获取操作人信息 */
public class SecurityUtils {

    public static LoginUser getLoginUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    public static Long getUserId() {
        LoginUser user = getLoginUser();
        return user == null ? null : user.getUserId();
    }

    public static String getUsername() {
        LoginUser user = getLoginUser();
        return user == null ? null : user.getUsername();
    }

    /** 角色校验：不满足抛 403（接口权限的等价实现） */
    public static void requireRole(String... roles) {
        LoginUser user = getLoginUser();
        if (user == null) {
            throw new BusinessException(401, "未登录");
        }
        for (String role : roles) {
            if (user.getRoles().contains(role)) {
                return;
            }
        }
        throw new BusinessException(403, "无权限执行此操作");
    }
}
