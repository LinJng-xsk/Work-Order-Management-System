package com.campus.woms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterDTO {

    /** 用户名 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 昵称 */
    @NotBlank(message = "昵称不能为空")
    private String nickname;

    /** 密码 */
    @NotBlank(message = "密码不能为空")
    private String password;

    /** 手机号 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 角色编码：employee=用户，operator=运维人员 */
    @NotBlank(message = "请选择角色")
    @Pattern(regexp = "^(employee|operator)$", message = "注册仅支持用户、运维人员两种角色")
    private String role;

    /** 工作日（仅运维人员需要）：1~7 逗号分隔，如 1,2,3,4,5 */
    private String workDays;
}
