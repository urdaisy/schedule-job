package com.schedule.job.security.aop;

import com.schedule.job.common.enums.ResultCode;
import com.schedule.job.common.exception.BusinessException;
import com.schedule.job.security.annotation.Around;
import com.schedule.job.security.annotation.Aspect;
import com.schedule.job.security.annotation.Pointcut;
import com.schedule.job.security.annotation.RequirePermission;
import com.schedule.job.security.domain.UserSession;
import com.schedule.job.security.manager.UserManager;
import com.schedule.job.security.reflect.ProceedingJoinPoint;
import com.schedule.job.security.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.WebApplicationContextUtils;

import java.lang.reflect.Method;
import java.util.Objects;

@Aspect
@Component
public class PermissionAspect {
    @Autowired
    private AuthService authService;

    // 定义切点：匹配带有 @RequirePermission 注解的方法
    @Pointcut(execute = "@annotation(com.schedule.job.security.annotation.RequirePermission)")
    public void permissionPointcut() {}

    // Around：在方法执行前后都执行
    @Around("permissionPointcut()")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 获取方法上的 @RequirePermission 注解
        Method method = joinPoint.getMethod();
        RequirePermission annotation = method.getAnnotation(RequirePermission.class);
        if (annotation == null) {
            return joinPoint.proceed();
        }

        // 2. 获取当前用户
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "未登录，请先登录");
        }

        // 3. 验证权限：支持多个权限，需要全部通过，如果没有指定权限，直接执行目标方法
        String[] permissions = annotation.value();
        if (permissions == null || permissions.length == 0) {
            return joinPoint.proceed();
        }
        
        // 遍历所有权限，检查用户是否拥有所有权限
        for (String permission : permissions) {
            if (permission == null || permission.trim().isEmpty()) {
                continue;
            }
            // 权限格式：resource:action，例如 "log:delete"、"job:create"
            String[] parts = permission.trim().split(":");
            if (parts.length != 2) {
                throw new BusinessException(ResultCode.UNAUTHORIZED, "权限格式错误，应为 resource:action 格式：" + permission);
            }
            String resource = parts[0].trim();
            String action = parts[1].trim();
            if (!authService.checkPermission(userId, resource, action)) {
                throw new BusinessException(ResultCode.UNAUTHORIZED, "无权限：" + permission);
            }
        }

        // 4. 权限验证通过，执行目标方法
        return joinPoint.proceed();
    }

    private Long getCurrentUserId() {
        try {
            // 从 RequestContextHolder 获取当前请求
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            
            if (requestAttributes instanceof ServletRequestAttributes) {
                    HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
                
                // 从 request 中获取 userId（由 TokenHandlerInterceptor 设置）
                Object userIdObj = request.getAttribute("userId");
                if (userIdObj instanceof Long) {
                    return (Long) userIdObj;
                }
                
                // 如果 request 中没有，尝试从 token 获取
                String token = request.getHeader("token");
                if (token == null || token.isEmpty()) {
                    token = request.getParameter("token");
                }
                
                if (token != null && !token.isEmpty()) {
                    UserSession userSession = Objects.requireNonNull(WebApplicationContextUtils.getWebApplicationContext(request.getServletContext()))
                            .getBean(UserManager.class)
                            .getCurrentUser(token);
                    if (userSession != null) {
                        return userSession.getUserId();
                    }
                }
            }
        } catch (Exception e) {
            // 忽略异常，返回 null
        }
        
        return null;
    }
}