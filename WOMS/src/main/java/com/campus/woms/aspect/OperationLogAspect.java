package com.campus.woms.aspect;

import cn.hutool.json.JSONUtil;
import com.campus.woms.common.annotation.OperationLog;
import com.campus.woms.dto.ReviewOrderDTO;
import com.campus.woms.entity.Device;
import com.campus.woms.entity.DeviceCategory;
import com.campus.woms.entity.SysMenu;
import com.campus.woms.entity.SysOperationLog;
import com.campus.woms.entity.SysRole;
import com.campus.woms.entity.SysUser;
import com.campus.woms.entity.WorkOrder;
import com.campus.woms.mapper.DeviceCategoryMapper;
import com.campus.woms.mapper.DeviceMapper;
import com.campus.woms.mapper.SysMenuMapper;
import com.campus.woms.mapper.SysOperationLogMapper;
import com.campus.woms.mapper.SysRoleMapper;
import com.campus.woms.mapper.SysUserMapper;
import com.campus.woms.mapper.WorkOrderMapper;
import com.campus.woms.security.LoginUser;
import com.campus.woms.security.SecurityUtils;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;


import com.campus.woms.dto.LoginDTO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final SysOperationLogMapper operationLogMapper;
    // ★ 删除场景按 ID 补业务名用
    private final DeviceMapper deviceMapper;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final DeviceCategoryMapper deviceCategoryMapper;
    private final SysMenuMapper sysMenuMapper;
    private final WorkOrderMapper workOrderMapper;

    @Pointcut("@annotation(com.campus.woms.common.annotation.OperationLog)")
    public void logPointcut() {
    }

    @Around("logPointcut()")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        // 只记录写操作（POST/PUT/DELETE）：GET 查询直接放行，不落库
        HttpServletRequest request = currentRequest();
        if (request != null && "GET".equalsIgnoreCase(request.getMethod())) {
            return pjp.proceed();
        }

        long start = System.currentTimeMillis();
        // ★ 在 proceed() 之前构建记录（此时实体还没删，能查到名称）
        SysOperationLog record = buildBaseRecord(pjp);
        try {
            Object result = pjp.proceed();
            record.setStatus(0);                      // 0=成功
            return result;
        } catch (Throwable e) {
            record.setStatus(1);                      // 1=失败
            record.setErrorMsg(truncate(e.getMessage(), 500));
            throw e;                                  // 继续抛给全局异常处理器
        } finally {
            record.setCostTime(System.currentTimeMillis() - start);
            saveRecord(record);
        }
    }

    private SysOperationLog buildBaseRecord(ProceedingJoinPoint pjp) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        OperationLog annotation = signature.getMethod().getAnnotation(OperationLog.class);

        SysOperationLog record = new SysOperationLog();
        record.setModule(annotation.module());
        record.setDescription(annotation.operation());
        record.setMethod(signature.getDeclaringTypeName() + "#" + signature.getName());
        record.setParams(buildParams(pjp.getArgs()));
        record.setDetail(buildDetail(pjp.getArgs(), pjp));   // ★ 操作明细（做了什么）
        record.setCreateTime(LocalDateTime.now());

        // 当前登录用户
        try {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            if (loginUser != null) {
                record.setOperatorId(loginUser.getUserId());
                record.setOperatorName(loginUser.getUsername());
            }
        } catch (Exception ignored) {
        }


        // HTTP 方法 + IP + 操作类型
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            record.setRequestMethod(request.getMethod());
            // ★ 认证模块（登录/登出）：操作类型语义化，不再显示"新增"
            if ("认证".equals(annotation.module())) {
                String op = annotation.operation() == null ? "" : annotation.operation();
                record.setOperationType(op.contains("退出") ? "登出" : "登录");
            } else {
                record.setOperationType(operationTypeOf(request.getMethod()));
            }
            record.setIp(getIp(request));
        }
        // ★ 操作人补录：登录接口尚未登录（SecurityUtils 拿不到），从请求参数 LoginDTO 取用户名
        if (record.getOperatorName() == null && "认证".equals(annotation.module())) {
            for (Object arg : pjp.getArgs()) {
                if (arg instanceof LoginDTO dto) {
                    record.setOperatorName(dto.getUsername());
                }
            }
        }
        return record;
    }


        /**
         * ★ 操作明细：
         * 1. 请求体有实体对象 → 直接取业务名称（新增/修改）；
         * 2. 只有 ID（删除/流转）→ 按 Controller 类型查库补名称（此时实体还没删，可查到）
         */
    private String buildDetail(Object[] args, ProceedingJoinPoint pjp) {
        if (args == null || args.length == 0) {
            return null;
        }
        try {
            // 第一优先：请求体里的实体对象
            for (Object arg : args) {
                if (arg == null) {
                    continue;
                }
                if (arg instanceof ServletRequest || arg instanceof ServletResponse
                        || arg instanceof MultipartFile || arg instanceof BindingResult) {
                    continue;
                }
                if (arg instanceof Device d) {
                    return "设备编号：" + nullToEmpty(d.getDeviceCode()) + "\n设备名称：" + nullToEmpty(d.getDeviceName());
                }
                if (arg instanceof SysUser u) {
                    return "用户：" + nullToEmpty(u.getUsername());
                }
                if (arg instanceof SysRole r) {
                    return "角色：" + nullToEmpty(r.getRoleName());
                }
                if (arg instanceof DeviceCategory c) {
                    return "分类：" + nullToEmpty(c.getCategoryName());
                }
                if (arg instanceof SysMenu m) {
                    return "菜单：" + nullToEmpty(m.getMenuName());
                }
                if (arg instanceof WorkOrder w) {
                    return "工单：" + nullToEmpty(w.getTitle());
                }
            }

            // 第二优先：只有 Long id → 按 Controller 类型查库补名称（删除/状态流转）
            Long id = null;
            for (Object arg : args) {
                if (arg instanceof Long l) {
                    id = l;
                    break;
                }
            }
            if (id != null) {
                String controller = pjp.getSignature().getDeclaringTypeName();
                if (controller.endsWith("DeviceController")) {
                    Device d = deviceMapper.selectById(id);
                    return d == null
                            ? "设备编号：" + id + "（该设备已删除）"
                            : "设备编号：" + nullToEmpty(d.getDeviceCode()) + "\n设备名称：" + nullToEmpty(d.getDeviceName());
                }
                if (controller.endsWith("SysUserController")) {
                    SysUser u = sysUserMapper.selectById(id);
                    return u == null ? "用户：ID " + id : "用户：" + nullToEmpty(u.getUsername());
                }
                if (controller.endsWith("SysRoleController")) {
                    SysRole r = sysRoleMapper.selectById(id);
                    return r == null ? "角色：ID " + id : "角色：" + nullToEmpty(r.getRoleName());
                }
                if (controller.endsWith("DeviceCategoryController")) {
                    DeviceCategory c = deviceCategoryMapper.selectById(id);
                    return c == null ? "分类：ID " + id : "分类：" + nullToEmpty(c.getCategoryName());
                }
                if (controller.endsWith("SysMenuController")) {
                    SysMenu m = sysMenuMapper.selectById(id);
                    return m == null ? "菜单：ID " + id : "菜单：" + nullToEmpty(m.getMenuName());
                }
                if (controller.endsWith("WorkOrderController")) {
                    WorkOrder w = workOrderMapper.selectById(id);
                    String methodName = pjp.getSignature().getName();
                    // 指派场景
                    if ("assign".equals(methodName)) {
                        for (Object arg : args) {
                            if (arg instanceof String s && !s.isBlank()) {
                                return "指派处理人：" + s + "\n" + workOrderBase(w, id);
                            }
                        }
                        return workOrderBase(w, id);
                    }
                    // 验收场景：按 pass 显示通过/不通过
                    if ("review".equals(methodName)) {
                        for (Object arg : args) {
                            if (arg instanceof ReviewOrderDTO dto) {
                                boolean pass = Boolean.TRUE.equals(dto.getPass());
                                return workOrderBase(w, id) + "\n验收" + (pass ? "通过" : "不通过");
                            }
                        }
                        return workOrderBase(w, id);
                    }
                    // ★ 删除/接单/处理/重新打开/关闭 等：工单（设备名）一行 + 原因（标题）一行
                    return workOrderBase(w, id);
                }
                return "ID " + id;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /** ★ 工单通用明细：设备名一行 + 标题（原因）一行，设备名为空时退化为标题 */
    private String workOrderBase(WorkOrder w, Long id) {
        if (w == null) {
            return "设备：ID " + id;
        }
        String device = nullToEmpty(w.getDeviceName());
        String title = nullToEmpty(w.getTitle());
        return device.isEmpty() ? "原因：" + title : "设备：" + device + "\n原因：" + title;
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private String operationTypeOf(String method) {
        if (method == null) {
            return "其他";
        }
        return switch (method.toUpperCase()) {
            case "POST" -> "新增";
            case "PUT" -> "修改";
            case "DELETE" -> "删除";
            case "GET" -> "查询";
            default -> "其他";
        };
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private String buildParams(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        try {
            List<Object> list = new ArrayList<>();
            for (Object arg : args) {
                if (arg == null) {
                    continue;
                }
                if (arg instanceof ServletRequest || arg instanceof ServletResponse
                        || arg instanceof MultipartFile || arg instanceof BindingResult) {
                    list.add("[忽略:" + arg.getClass().getSimpleName() + "]");
                } else {
                    list.add(arg);
                }
            }
            return JSONUtil.toJsonStr(list);
        } catch (Exception e) {
            return "[参数序列化失败]";
        }
    }

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else if (ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return truncate(ip, 64);
    }

    private String truncate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }

    private void saveRecord(SysOperationLog record) {
        try {
            operationLogMapper.insert(record);
        } catch (Exception e) {
            log.error("操作日志写入失败: {}", e.getMessage());
        }
    }
}
