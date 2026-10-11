package com.campus.woms.service.impl;

import com.campus.woms.common.exception.BusinessException;
import com.campus.woms.dto.LoginDTO;
import com.campus.woms.dto.LoginResultDTO;
import com.campus.woms.entity.SysLoginLog;
import com.campus.woms.entity.SysUser;
import com.campus.woms.mapper.SysLoginLogMapper;
import com.campus.woms.security.JwtUtils;
import com.campus.woms.service.AuthService;
import com.campus.woms.service.SysUserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;

//验证码可视化-开发版
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;




@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {


    /** 验证码调试开关：true 时响应返回验证码明文（仅本地开发） */
    @Value("${woms.captcha.debug:false}")
    private boolean captchaDebug;



    private final SysUserService sysUserService;
    private final SysLoginLogMapper sysLoginLogMapper;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate stringRedisTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final String CAPTCHA_PREFIX = "woms:captcha:";
    private static final String LOGIN_FAIL_PREFIX = "woms:login:fail:";
    private static final int MAX_FAIL_COUNT = 5;

//    @Override
//    public Map<String, String> generateCaptcha() {
//        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 20);
//        String uuid = UUID.randomUUID().toString().replace("-", "");
//        stringRedisTemplate.opsForValue().set(CAPTCHA_PREFIX + uuid, captcha.getCode(), Duration.ofMinutes(5));
//        Map<String, String> map = new HashMap<>();
//        map.put("uuid", uuid);
//        map.put("img", captcha.getImageBase64());
//        return map;
//    }


//上面方法的优化版，将验证码直接可视化
    @Override
    public Map<String, String> generateCaptcha() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 20);
        String code = captcha.getCode();   // 验证码明文，存进变量供复用
        String uuid = UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForValue().set(CAPTCHA_PREFIX + uuid, code, Duration.ofMinutes(5));
        Map<String, String> map = new HashMap<>();
        map.put("uuid", uuid);
        map.put("img", captcha.getImageBase64());
//修改上面那一行，让`img` 返回时就带上前缀
//        map.put("img", "data:image/png;base64," + captcha.getImageBase64Data());


        // 开发调试：验证码明文直接返回，方便测试（上线关闭）
        if (captchaDebug) {
            map.put("captchaCode", code);
        }
        return map;
    }


    @Override
    public LoginResultDTO login(LoginDTO dto, HttpServletRequest request) {
        // 1. 验证码校验
        String captcha = stringRedisTemplate.opsForValue().get(CAPTCHA_PREFIX + dto.getUuid());
        if (captcha == null) {
            throw new BusinessException("验证码已过期，请刷新");
        }
        stringRedisTemplate.delete(CAPTCHA_PREFIX + dto.getUuid());
        if (!captcha.equalsIgnoreCase(dto.getCode())) {
            throw new BusinessException("验证码错误");
        }

        // 2. 登录失败限流：连续失败 5 次，10 分钟内锁定
        String failKey = LOGIN_FAIL_PREFIX + dto.getUsername();
        String failCount = stringRedisTemplate.opsForValue().get(failKey);
        if (failCount != null && Integer.parseInt(failCount) >= MAX_FAIL_COUNT) {
            throw new BusinessException("失败次数过多，请10分钟后再试");
        }

        // 3. 用户 + 密码校验
        SysUser user = sysUserService.getByUsername(dto.getUsername());
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            Long count = stringRedisTemplate.opsForValue().increment(failKey);
            if (count != null && count == 1) {
                stringRedisTemplate.expire(failKey, Duration.ofMinutes(10));
            }
            saveLoginLog(dto.getUsername(), false, "用户名或密码错误", request);
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 1) {
            saveLoginLog(dto.getUsername(), false, "账号已停用", request);
            throw new BusinessException("账号已被停用");
        }
        stringRedisTemplate.delete(failKey);

        // 4. 签发 Token + 登录日志
        String token = jwtUtils.generateToken(user.getId(), user.getUsername());
        List<String> roles = sysUserService.getRoleCodesByUserId(user.getId());
        saveLoginLog(user.getUsername(), true, "登录成功", request);
        return new LoginResultDTO(token, user.getNickname(), roles, String.valueOf(user.getId()));
    }

    @Override
    public void logout(String token) {
        Claims claims = jwtUtils.parseToken(token);
        jwtUtils.blacklist(claims.getId(), claims.getExpiration());
    }

    @Override
    public String refresh(String token) {
        try {
            Claims claims = jwtUtils.parseToken(token);
            if (jwtUtils.isBlacklisted(claims.getId())) {
                throw new BusinessException(401, "Token已失效，请重新登录");
            }
            return jwtUtils.generateToken(Long.valueOf(claims.getSubject()),
                    claims.get("username", String.class));
        } catch (JwtException e) {
            throw new BusinessException(401, "Token无效或已过期");
        }
    }

    private void saveLoginLog(String username, boolean success, String msg, HttpServletRequest request) {
        SysLoginLog log = new SysLoginLog();
        log.setUsername(username);
        log.setIp(request.getRemoteAddr());
        log.setBrowser(parseBrowser(request.getHeader("User-Agent")));
        log.setStatus(success ? 0 : 1);
        log.setMsg(msg);
        log.setCreateTime(LocalDateTime.now());   // ← 新增这一行：手动填充时间
        sysLoginLogMapper.insert(log);
    }

    /** 极简 UA 解析，够用即可 */
    private String parseBrowser(String userAgent) {
        if (userAgent == null) return "未知";
        if (userAgent.contains("Android")) return "Android";
        if (userAgent.contains("iPhone")) return "iPhone";
        if (userAgent.contains("Edg")) return "Edge";
        if (userAgent.contains("Chrome")) return "Chrome";
        if (userAgent.contains("Firefox")) return "Firefox";
        return "其他";
    }
}
