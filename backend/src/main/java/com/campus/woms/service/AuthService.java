package com.campus.woms.service;

import com.campus.woms.dto.LoginDTO;
import com.campus.woms.dto.LoginResultDTO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface AuthService {

    /** 生成验证码：返回 {uuid, base64图片} */
    Map<String, String> generateCaptcha();

    /** 登录：验证码 → 限流 → BCrypt → 签发 Token + 登录日志 */
    LoginResultDTO login(LoginDTO dto, HttpServletRequest request);

    /** 注销：Token 进黑名单 */
    void logout(String token);

    /** 刷新 Token */
    String refresh(String token);
}
