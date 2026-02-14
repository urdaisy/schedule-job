package com.schedule.job.security.controller;

import com.schedule.job.common.enums.ResultCode;
import com.schedule.job.common.exception.BusinessException;
import com.schedule.job.common.exception.Result;
import com.schedule.job.security.domain.Permission;
import com.schedule.job.security.domain.RoleInfo;
import com.schedule.job.security.domain.UserDetailInfo;
import com.schedule.job.security.domain.UserInfo;
import com.schedule.job.security.entity.EntityConvert;
import com.schedule.job.security.entity.UserInfoEntity;
import com.schedule.job.security.manager.UserManager;
import com.schedule.job.security.repository.PermissionRepository;
import com.schedule.job.security.repository.RoleInfoRepository;
import com.schedule.job.security.repository.UserInfoRepository;
import com.schedule.job.security.service.AuthService;
import jakarta.annotation.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import com.schedule.job.security.annotation.RequirePermission;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户管理Controller
 * 仅管理员可访问
 */
@RestController
@RequestMapping("/api/user")
public class UserController {
    
    @Resource
    private UserInfoRepository userInfoRepository;
    
    @Resource
    private RoleInfoRepository roleInfoRepository;
    
    @Resource
    private PermissionRepository permissionRepository;
    
    @Resource
    private UserManager userManager;
    
    @Resource
    private AuthService authService;
    
    /**
     * 获取所有用户列表（包含角色和权限信息）
     * 使用连表查询优化性能，一次性获取所有数据
     */
    @GetMapping("/list")
    @RequirePermission("user:query")
    public Result<List<UserDetailInfo>> getUserList() {
        try {
            List<UserDetailInfo> userDetails = userInfoRepository.findAllUserDetails();
            return Result.success(userDetails);
        } catch (Exception e) {
            return Result.error(ResultCode.ERROR, "获取用户列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取用户详情（包含角色和权限）
     * 使用连表查询优化性能，一次性获取所有数据
     */
    @GetMapping("/{userId}")
    @RequirePermission("user:query")
    public Result<UserDetailInfo> getUserDetail(@PathVariable Long userId) {
        try {
            Optional<UserDetailInfo> userDetailOpt = userInfoRepository.findUserDetailById(userId);
            if (!userDetailOpt.isPresent()) {
                return Result.error(ResultCode.NOT_FOUND, "用户不存在");
            }
            
            return Result.success(userDetailOpt.get());
        } catch (Exception e) {
            return Result.error(ResultCode.ERROR, "获取用户详情失败：" + e.getMessage());
        }
    }
    
    /**
     * 创建用户（管理员创建）
     */
    @PostMapping
    @RequirePermission("user:create")
    public Result<UserDetailInfo> createUser(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String email,
            @RequestParam Long roleId) {
        try {
            // 参数验证
            if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password) || StringUtils.isEmpty(email)) {
                return Result.error(ResultCode.PARAM_ERROR, "用户名、密码和邮箱不能为空");
            }
            
            if (roleId == null) {
                return Result.error(ResultCode.PARAM_ERROR, "角色ID不能为空");
            }
            
            // 验证角色是否存在
            Optional<RoleInfo> roleOpt = roleInfoRepository.findRoleById(roleId);
            if (!roleOpt.isPresent()) {
                return Result.error(ResultCode.PARAM_ERROR, "角色不存在");
            }
            
            // 创建用户（管理员创建，允许创建管理员角色）
            UserInfo userInfo = userManager.createUser(username.trim(), password.trim(), email.trim(), roleId, true);
            
            UserDetailInfo detail = convertToUserDetailInfo(userInfo);
            return Result.success("用户创建成功", detail);
        } catch (BusinessException e) {
            return Result.error(ResultCode.ERROR, e.getMessage());
        } catch (Exception e) {
            return Result.error(ResultCode.ERROR, "创建用户失败：" + e.getMessage());
        }
    }
    
    /**
     * 更新用户信息
     */
    @PutMapping("/{userId}")
    @RequirePermission("user:update")
    public Result<UserDetailInfo> updateUser(
            @PathVariable Long userId,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Integer status) {
        try {
            Optional<UserInfo> userOpt = userInfoRepository.findUserById(userId);
            if (!userOpt.isPresent()) {
                return Result.error(ResultCode.NOT_FOUND, "用户不存在");
            }
            
            UserInfoEntity userEntity = userInfoRepository.findById(userId);
            boolean needUpdate = false;
            
            if (email != null && !email.trim().isEmpty()) {
                userEntity.setEmail(email.trim());
                needUpdate = true;
            }
            
            if (roleId != null) {
                // 验证角色是否存在
                Optional<RoleInfo> roleOpt = roleInfoRepository.findRoleById(roleId);
                if (!roleOpt.isPresent()) {
                    return Result.error(ResultCode.PARAM_ERROR, "角色不存在");
                }
                userEntity.setRoleId(roleId);
                needUpdate = true;
            }
            
            if (status != null) {
                userEntity.setStatus(status);
                needUpdate = true;
            }
            
            if (needUpdate) {
                userEntity.setUpdateTime(LocalDateTime.now());
                userInfoRepository.updateUser(userEntity);
            }
            
            UserDetailInfo detail = convertToUserDetailInfo(EntityConvert.convertToUserInfo(userEntity));
            return Result.success("用户更新成功", detail);
        } catch (Exception e) {
            return Result.error(ResultCode.ERROR, "更新用户失败：" + e.getMessage());
        }
    }
    
    /**
     * 删除用户
     */
    @DeleteMapping("/{userId}")
    @RequirePermission("user:delete")
    public Result<String> deleteUser(@PathVariable Long userId) {
        try {
            Optional<UserInfo> userOpt = userInfoRepository.findUserById(userId);
            if (!userOpt.isPresent()) {
                return Result.error(ResultCode.NOT_FOUND, "用户不存在");
            }
            
            // 不允许删除自己
            // TODO: 从request中获取当前用户ID，判断是否为当前用户
            
            boolean deleted = userInfoRepository.deleteById(userId);
            if (deleted) {
                return Result.success("用户删除成功");
            } else {
                return Result.error(ResultCode.ERROR, "用户删除失败");
            }
        } catch (Exception e) {
            return Result.error(ResultCode.ERROR, "删除用户失败：" + e.getMessage());
        }
    }
    
    /**
     * 启用/禁用用户
     */
    @PutMapping("/{userId}/status")
    @RequirePermission("user:update")
    public Result<String> updateUserStatus(@PathVariable Long userId, @RequestParam Integer status) {
        try {
            if (status == null || (status != 0 && status != 1)) {
                return Result.error(ResultCode.PARAM_ERROR, "状态值无效，只能为0（禁用）或1（启用）");
            }
            
            Optional<UserInfo> userOpt = userInfoRepository.findUserById(userId);
            if (!userOpt.isPresent()) {
                return Result.error(ResultCode.NOT_FOUND, "用户不存在");
            }
            
            UserInfoEntity userEntity = userInfoRepository.findById(userId);
            userEntity.setStatus(status);
            userEntity.setUpdateTime(LocalDateTime.now());
            userInfoRepository.updateUser(userEntity);
            
            String message = status == 1 ? "用户已启用" : "用户已禁用";
            return Result.success(message);
        } catch (Exception e) {
            return Result.error(ResultCode.ERROR, "更新用户状态失败：" + e.getMessage());
        }
    }
    
    /**
     * 将UserInfo转换为UserDetailInfo（包含角色和权限信息）
     */
    private UserDetailInfo convertToUserDetailInfo(UserInfo user) {
        UserDetailInfo detail = new UserDetailInfo();
        detail.setId(user.getId());
        detail.setUsername(user.getUsername());
        detail.setEmail(user.getEmail());
        detail.setRoleId(user.getRoleId());
        detail.setStatus(user.getStatus());
        detail.setCreateTime(user.getCreateTime());
        detail.setUpdateTime(user.getUpdateTime());
        
        // 获取角色信息
        if (user.getRoleId() != null) {
            Optional<RoleInfo> roleOpt = roleInfoRepository.findRoleById(user.getRoleId());
            if (roleOpt.isPresent()) {
                RoleInfo role = roleOpt.get();
                detail.setRoleName(role.getRoleName());
                detail.setRoleCode(role.getRoleCode());
                detail.setRoleDescription(role.getDescription());
                
                // 获取权限列表
                List<Permission> permissions = permissionRepository.findByRoleId(user.getRoleId());
                detail.setPermissions(permissions);
            }
        }
        
        return detail;
    }
}
