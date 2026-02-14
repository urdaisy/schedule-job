package com.schedule.job.security.manager;

import com.schedule.job.common.enums.BusinessExceptionCode;
import com.schedule.job.common.exception.BusinessException;
import com.schedule.job.common.util.PasswordUtil;
import com.schedule.job.security.domain.UserInfo;
import com.schedule.job.security.domain.UserSession;
import com.schedule.job.security.entity.EntityConvert;
import com.schedule.job.security.entity.UserInfoEntity;
import com.schedule.job.security.entity.UserSessionEntity;
import com.schedule.job.security.repository.UserInfoRepository;
import com.schedule.job.security.repository.UserSessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
public class UserManager {
    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    /**
     * 根据token获取当前用户会话
     */
    public UserSession getCurrentUser(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        Optional<UserSession> userSession = userSessionRepository.findByToken(token);
        return userSession.orElse(null);
    }

    public UserInfo checkUserInfo(String username, String password) {
        // 查询用户（需要从Entity获取加密后的密码）
        Optional<UserInfo> userInfoOpt = userInfoRepository.findByUsername(username);
        if (!userInfoOpt.isPresent()) {
            log.error("用户{}不存在", username);
            throw new BusinessException(BusinessExceptionCode.USER_NOT_FOUND);
        }

        UserInfo userInfo = userInfoOpt.get();
        // 检查用户状态
        if (userInfo.getStatus() == null || userInfo.getStatus() == 0) {
            log.error("用户{}已被禁用", username);
            throw new BusinessException(BusinessExceptionCode.USER_PARAM_ERROR, "用户已被禁用");
        }

        // 进行密码验证
        boolean isValid = PasswordUtil.matchPassword(password, userInfo.getPassword());
        if (!isValid) {
            log.error("用户:{}, 密码:{}错误", username, password);
            throw new BusinessException(BusinessExceptionCode.USER_PARAM_ERROR, "用户名或密码错误");
        }

        return userInfo;
    }

    public UserSession saveUserInfo(UserInfo userInfo, String token) {
        UserSession userSession = new UserSession();
        userSession.setUserId(userInfo.getId());
        userSession.setToken(token);
        userSession.setExpireTime(LocalDateTime.now().plusHours(3));
        userSession.setCreateTime(LocalDateTime.now());

        boolean isSaved = userSessionRepository.save(EntityConvert.convertToUserSessionEntity(userSession));
        if (!isSaved) {
            throw new BusinessException(BusinessExceptionCode.USER_SESSION_UPDATE_FAILED, "用户会话更新失败");
        }
        return userSession;
    }

    public void deleteUserSession(String token) {
        Optional<UserSession> userSession = userSessionRepository.findByToken(token);
        if (userSession.isPresent()) {
            // 删除会话记录
            UserSessionEntity sessionEntity = EntityConvert.convertToUserSessionEntity(userSession.get());
            userSessionRepository.deleteById(sessionEntity.getId());
            log.info("用户登出成功, userId: {}", userSession.get().getUserId());
        }
    }

    /**
     * 创建新用户
     * @param username 用户名
     * @param password 密码
     * @param email 邮箱
     * @param roleId 角色ID（1-管理员，2-普通用户）
     * @param allowAdminRegister 是否允许注册管理员（默认false，仅允许注册普通用户）
     */
    public UserInfo createUser(String username, String password, String email, Long roleId, boolean allowAdminRegister) {
        // 检查用户名是否已存在
        Optional<UserInfo> existingUser = userInfoRepository.findByUsername(username);
        if (existingUser.isPresent()) {
            log.error("用户名{}已存在", username);
            throw new BusinessException(BusinessExceptionCode.USER_PARAM_ERROR, "用户名已存在");
        }

        // 验证角色ID
        if (roleId == null) {
            roleId = 2L; // 默认角色为普通用户
        }
        
        // 安全控制：如果不允许注册管理员，且传入的角色ID是管理员(1)，则抛出异常
        if (!allowAdminRegister && roleId == 1L) {
            log.warn("尝试注册管理员账号被拒绝，用户名: {}", username);
            throw new BusinessException(BusinessExceptionCode.USER_PARAM_ERROR, "不允许注册管理员账号");
        }
        
        // 验证角色ID是否有效（1或2）
        if (roleId != 1L && roleId != 2L) {
            log.error("无效的角色ID: {}, 用户名: {}", roleId, username);
            throw new BusinessException(BusinessExceptionCode.USER_PARAM_ERROR, "无效的角色ID，仅支持1（管理员）或2（普通用户）");
        }

        // 加密密码
        String encryptedPassword = PasswordUtil.encryptPassword(password);

        // 创建用户实体
        UserInfoEntity userEntity = new UserInfoEntity();
        userEntity.setUsername(username);
        userEntity.setPassword(encryptedPassword);
        userEntity.setEmail(email);
        userEntity.setRoleId(roleId);
        userEntity.setStatus(1); // 默认状态为启用
        userEntity.setCreateTime(LocalDateTime.now());
        userEntity.setUpdateTime(LocalDateTime.now());

        // 保存用户
        boolean isSaved = userInfoRepository.save(userEntity);
        if (!isSaved) {
            log.error("用户{}创建失败", username);
            throw new BusinessException(BusinessExceptionCode.USER_UPDATE_FAILED, "用户创建失败");
        }

        // 重新查询获取ID
        Optional<UserInfo> savedUser = userInfoRepository.findByUsername(username);
        if (!savedUser.isPresent()) {
            log.error("用户{}创建后查询失败", username);
            throw new BusinessException(BusinessExceptionCode.USER_UPDATE_FAILED, "用户创建失败");
        }

        log.info("用户{}创建成功，角色ID: {}", username, roleId);
        return savedUser.get();
    }
    
    /**
     * 创建新用户（公开注册接口，仅允许注册普通用户）
     */
    public UserInfo createUser(String username, String password, String email, Long roleId) {
        // 公开注册接口，不允许注册管理员
        return createUser(username, password, email, roleId, false);
    }
}
