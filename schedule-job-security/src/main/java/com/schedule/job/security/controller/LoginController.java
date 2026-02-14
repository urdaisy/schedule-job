package com.schedule.job.security.controller;

import com.schedule.job.common.enums.ResultCode;
import com.schedule.job.common.exception.BusinessException;
import com.schedule.job.common.exception.Result;
import com.schedule.job.security.domain.RoleInfo;
import com.schedule.job.security.domain.UserInfo;
import com.schedule.job.security.domain.UserSession;
import com.schedule.job.security.annotation.RequirePermission;
import com.schedule.job.security.repository.RoleInfoRepository;
import com.schedule.job.security.service.AuthService;
import com.schedule.job.security.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/login")
public class LoginController {
    @Resource
    private AuthService authService;

    @Resource
    private UserService userService;
    
    @Resource
    private RoleInfoRepository roleInfoRepository;

    /**
     * 用户登录
     */
    @PostMapping
    public Result<UserSession> login(@RequestParam String username, @RequestParam String password) throws BusinessException {
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            return Result.error(ResultCode.PARAM_ERROR, "用户登录名和密码不能为空");
        }
        UserSession userSession = userService.login(username.trim(), password.trim());
        return Result.success("用户登录成功", userSession);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public Result<String> logout(@RequestParam(required = false) String token) throws BusinessException {
        // token 可以从请求头获取，也可以从参数获取
        if (StringUtils.isEmpty(token)) {
            // 如果参数中没有，尝试从请求头获取（由拦截器设置）
            return Result.error(ResultCode.PARAM_ERROR, "用户令牌不能为空");
        }
        userService.logout(token);
        return Result.success("用户登出成功");
    }
    
    /**
     * 获取当前用户信息
     */
    @PostMapping("/current")
    public Result<UserInfo> getCurrentUser(@RequestParam String token) throws BusinessException {
        if (StringUtils.isEmpty(token)) {
            return Result.error(ResultCode.PARAM_ERROR, "用户令牌不能为空");
        }
        UserInfo userInfo = authService.getUserInfoByToken(token);
        return Result.success(userInfo);
    }

    /**
     * 获取可注册的角色列表（公开接口，用于注册页面）
     * 返回的角色列表排除管理员，只允许注册普通用户
     */
    @GetMapping("/register/roles")
    public Result<List<RoleInfo>> getRegisterableRoles() {
        try {
            // 获取可注册的角色列表（排除管理员）
            List<RoleInfo> roles = roleInfoRepository.findRegisterableRoles();
            return Result.success(roles);
        } catch (Exception e) {
            return Result.error(ResultCode.ERROR, "获取角色列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取所有角色列表（需要权限验证，用于用户管理页面）
     * 管理员可以查看所有角色，包括管理员角色
     */
    @GetMapping("/roles")
    @RequirePermission("user:query")
    public Result<List<RoleInfo>> getAllRoles() {
        try {
            // 获取所有角色（包括管理员）
            List<RoleInfo> roles = roleInfoRepository.findAllRoles();
            return Result.success(roles);
        } catch (Exception e) {
            return Result.error(ResultCode.ERROR, "获取角色列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 用户注册（允许用户选择角色）
     */
    @PostMapping("/register")
    public Result<UserInfo> register(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String email,
            @RequestParam(required = false) Long roleId) throws BusinessException {
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password) || StringUtils.isEmpty(email)) {
            return Result.error(ResultCode.PARAM_ERROR, "用户名、密码和邮箱不能为空");
        }
        
        // 验证用户名长度
        if (username.trim().length() < 3) {
            return Result.error(ResultCode.PARAM_ERROR, "用户名长度不能少于3位");
        }
        
        // 验证密码长度
        if (password.trim().length() < 6) {
            return Result.error(ResultCode.PARAM_ERROR, "密码长度不能少于6位");
        }
        
        // 验证邮箱格式（简单验证）
        String emailTrimmed = email.trim();
        if (!emailTrimmed.contains("@") || !emailTrimmed.contains(".")) {
            return Result.error(ResultCode.PARAM_ERROR, "邮箱格式不正确");
        }
        
        // 验证角色ID
        if (roleId == null) {
            return Result.error(ResultCode.PARAM_ERROR, "请选择角色");
        }
        
        // 验证角色ID是否有效（检查角色是否存在）
        List<RoleInfo> registerableRoles = roleInfoRepository.findRegisterableRoles();
        boolean roleExists = registerableRoles.stream().anyMatch(role -> role.getId().equals(roleId));
        if (!roleExists) {
            return Result.error(ResultCode.PARAM_ERROR, "选择的角色无效或不允许注册");
        }
        
        // 安全控制：不允许注册管理员（roleId=1）
        if (roleId == 1L) {
            return Result.error(ResultCode.PARAM_ERROR, "不允许通过公开接口注册管理员账号");
        }
        
        UserInfo userInfo = userService.register(username.trim(), password.trim(), emailTrimmed, roleId);
        return Result.success("用户注册成功", userInfo);
    }
}
