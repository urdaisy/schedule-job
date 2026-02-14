package com.schedule.job.security.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schedule.job.common.enums.ResultCode;
import com.schedule.job.common.exception.Result;
import com.schedule.job.security.domain.UserSession;
import com.schedule.job.security.manager.UserManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Token验证拦截器
 * Spring MVC 的 HandlerInterceptor 拦截 HTTP 请求
 * 用于验证用户请求中的token是否有效
 */
@Slf4j
public class TokenHandlerInterceptor implements HandlerInterceptor {
    private final UserManager userManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TokenHandlerInterceptor(UserManager userManager) {
        this.userManager = userManager;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取token（支持从Header或参数中获取）
        String token = request.getHeader("token");
        if (token == null || token.isEmpty()) {
            token = request.getParameter("token");
        }
        
        // 如果没有token，返回未授权错误
        if (token == null || token.isEmpty()) {
            log.warn("请求中未指定token, 请求路径: {}", request.getRequestURI());
            writeErrorResponse(response, ResultCode.UNAUTHORIZED, "请先登录");
            return false;
        }

        // 验证token有效性
        UserSession userSession = userManager.getCurrentUser(token);
        if (userSession == null) {
            log.warn("token无效或已过期, token: {}", token);
            writeErrorResponse(response, ResultCode.UNAUTHORIZED, "token无效或已过期，请重新登录");
            return false;
        }

        // 检查token是否过期
        if (userSession.getExpireTime() != null && userSession.getExpireTime().isBefore(LocalDateTime.now())) {
            log.warn("token已过期, userId: {}, token: {}", userSession.getUserId(), token);
            writeErrorResponse(response, ResultCode.UNAUTHORIZED, "token已过期，请重新登录");
            return false;
        }

        // 将用户信息存储到request中，方便后续使用
        request.setAttribute("userId", userSession.getUserId());
        request.setAttribute("userSession", userSession);
        
        log.debug("token验证通过, userId: {}, path: {}", userSession.getUserId(), request.getRequestURI());
        return true;
    }

    /**
     * 写入错误响应
     */
    private void writeErrorResponse(HttpServletResponse response, ResultCode resultCode, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Result<Object> result = Result.error(resultCode, message);
        objectMapper.writeValue(response.getWriter(), result);
    }
}
