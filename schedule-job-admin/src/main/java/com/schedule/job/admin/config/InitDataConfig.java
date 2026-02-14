package com.schedule.job.admin.config;

import com.schedule.job.security.domain.RoleInfo;
import com.schedule.job.security.domain.UserInfo;
import com.schedule.job.security.manager.UserManager;
import com.schedule.job.security.repository.RoleInfoRepository;
import com.schedule.job.security.repository.UserInfoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 初始化数据配置
 * 系统启动时自动创建默认管理员账号
 */
@Slf4j
@Component
public class InitDataConfig implements CommandLineRunner {
    
    @Autowired
    private UserManager userManager;
    
    @Autowired
    private UserInfoRepository userInfoRepository;
    
    @Autowired
    private RoleInfoRepository roleInfoRepository;
    
    @Override
    public void run(String... args) throws Exception {
        log.info("开始初始化系统数据...");
        
        // 1. 检查并创建默认管理员账号
        initDefaultAdmin();
        
        // 2. 检查并初始化角色数据
        initRoles();
        
        log.info("系统数据初始化完成");
    }
    
    /**
     * 初始化默认管理员账号
     */
    private void initDefaultAdmin() {
        try {
            // 检查是否已存在管理员账号
            Optional<UserInfo> adminUser = userInfoRepository.findByUsername("admin");
            if (adminUser.isPresent()) {
                log.info("默认管理员账号已存在，跳过创建");
                return;
            }
            
            // 检查是否存在管理员角色（roleId=1）
            Optional<RoleInfo> adminRole = roleInfoRepository.findRoleById(1L);
            if (!adminRole.isPresent()) {
                log.warn("管理员角色不存在，无法创建默认管理员账号");
                return;
            }
            
            // 创建默认管理员账号
            // 使用 allowAdminRegister=true 允许创建管理员
            userManager.createUser(
                "admin",
                "admin123",
                "admin@example.com",
                1L,  // 管理员角色ID
                true  // 允许创建管理员
            );
            
            log.info("默认管理员账号创建成功: username=admin, password=admin123");
        } catch (Exception e) {
            log.error("创建默认管理员账号失败", e);
        }
    }
    
    /**
     * 初始化角色数据（如果不存在）
     */
    private void initRoles() {
        try {
            // 检查管理员角色
            Optional<RoleInfo> adminRole = roleInfoRepository.findRoleById(1L);
            if (!adminRole.isPresent()) {
                log.warn("管理员角色（roleId=1）不存在，请手动创建");
            } else {
                log.info("管理员角色已存在: {}", adminRole.get().getRoleName());
            }
            
            // 检查普通用户角色
            Optional<RoleInfo> userRole = roleInfoRepository.findRoleById(2L);
            if (!userRole.isPresent()) {
                log.warn("普通用户角色（roleId=2）不存在，请手动创建");
            } else {
                log.info("普通用户角色已存在: {}", userRole.get().getRoleName());
            }
        } catch (Exception e) {
            log.error("检查角色数据失败", e);
        }
    }
}
