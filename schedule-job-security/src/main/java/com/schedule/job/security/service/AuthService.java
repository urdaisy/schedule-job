package com.schedule.job.security.service;

import com.schedule.job.common.enums.BusinessExceptionCode;
import com.schedule.job.common.exception.BusinessException;
import com.schedule.job.security.domain.Permission;
import com.schedule.job.security.domain.UserInfo;
import com.schedule.job.security.domain.UserSession;
import com.schedule.job.security.entity.UserInfoEntity;
import com.schedule.job.security.manager.UserManager;
import com.schedule.job.security.repository.PermissionRepository;
import com.schedule.job.security.repository.UserInfoRepository;
import com.schedule.job.security.repository.UserSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AuthService {
    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private PermissionRepository permissionRepository;
    
    @Autowired
    private UserSessionRepository userSessionRepository;

    /**
     * 使用token做认证
     * @param token
     * @return
     * @throws BusinessException
     */
    public UserInfo getUserInfoByToken(String token) throws BusinessException {
        if (token == null || token.isEmpty()) {
            throw new BusinessException(BusinessExceptionCode.USER_NOT_FOUND, "token不能为空");
        }
        // 从 UserSessionRepository 获取用户会话
        Optional<UserSession> userSessionOpt = userSessionRepository.findByToken(token);
        if (!userSessionOpt.isPresent()) {
            throw new BusinessException(BusinessExceptionCode.USER_NOT_FOUND, "token无效");
        }
        UserSession userSession = userSessionOpt.get();
        // 根据 userId 获取用户信息
        UserInfoEntity userInfoEntity = userInfoRepository.findById(userSession.getUserId());
        if (userInfoEntity == null) {
            throw new BusinessException(BusinessExceptionCode.USER_NOT_FOUND, "用户不存在");
        }
        return com.schedule.job.security.entity.EntityConvert.convertToUserInfo(userInfoEntity);
    }

    /**
     * 鉴权功能
     * @param userId
     * @param resource
     * @param action
     * @return
     * @throws BusinessException
     */
    public boolean checkPermission(Long userId, String resource, String action) throws BusinessException {
        if (userId == null || resource == null || action == null) {
            return false;
        }
        UserInfoEntity userInfoEntity = userInfoRepository.findById(userId);
        if (userInfoEntity == null) {
            return false;
        }
        List<Permission> permissionList = permissionRepository.findByRoleId(userInfoEntity.getRoleId());
        for (Permission permissionEntity : permissionList) {
            if (permissionEntity.getResource().equals(resource) && permissionEntity.getAction().equals(action)) {
                return true;
            }
        }
        return false;
    }
}
