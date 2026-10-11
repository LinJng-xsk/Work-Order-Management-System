package com.campus.woms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class LoginResultDTO {

    private String token;
    private String nickname;
    private List<String> roles;
    /** 当前登录用户 id（String 防止 19 位大数被 JS 丢精度，前端按钮权限依赖） */
    private String userId;
}
