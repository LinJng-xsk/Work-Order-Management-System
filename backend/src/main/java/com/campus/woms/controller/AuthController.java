package com.campus.woms.controller;

import com.campus.woms.common.result.Result;
import com.campus.woms.dto.LoginDTO;
import com.campus.woms.dto.LoginResultDTO;
import com.campus.woms.entity.SysUser;
import com.campus.woms.service.AuthService;

import com.campus.woms.dto.RegisterDTO;
import com.campus.woms.service.SysUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.campus.woms.common.annotation.OperationLog;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private final SysUserService sysUserService;

    /** 获取验证码：{uuid, img} */
    @GetMapping("/captcha")
    public Result<Map<String, String>> captcha() {
        return Result.ok(authService.generateCaptcha());
    }

    /** 登录 */
    @OperationLog(module = "认证", operation = "用户登录")
    @PostMapping("/login")
    public Result<LoginResultDTO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        return Result.ok(authService.login(dto, request));
    }

    //注册
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setNickname(dto.getNickname());
        user.setPhone(dto.getPhone());
        user.setWorkDays(dto.getWorkDays());
        sysUserService.register(user, dto.getPassword(), dto.getRole());
        return Result.ok();
    }

    /** 注销（Token 进黑名单即时失效） */
    @OperationLog(module = "认证", operation = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authorization) {
        authService.logout(authorization.substring(7));
        return Result.ok();
    }

    /** 刷新 Token */
    @OperationLog(module = "认证", operation = "刷新Token")
    @PostMapping("/refresh")
    public Result<String> refresh(@RequestHeader("Authorization") String authorization) {
        return Result.ok(authService.refresh(authorization.substring(7)));
    }
}
