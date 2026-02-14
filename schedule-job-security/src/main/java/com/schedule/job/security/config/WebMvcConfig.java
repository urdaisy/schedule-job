package com.schedule.job.security.config;

import com.schedule.job.security.interceptor.TokenHandlerInterceptor;
import com.schedule.job.security.manager.UserManager;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置类
 * 用于注册拦截器
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    @Resource
    private UserManager userManager;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TokenHandlerInterceptor(userManager))
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/login",                    // 登录接口
                        "/api/login/register",           // 注册接口
                        "/api/login/register/roles",    // 获取可注册角色列表接口（公开接口）
                        "/api/login/logout",             // 登出接口（如果需要）
                        "/api/logout",                   // 登出接口（兼容）
                        "/api/error",
                        "/api/health"
                );
    }
}
