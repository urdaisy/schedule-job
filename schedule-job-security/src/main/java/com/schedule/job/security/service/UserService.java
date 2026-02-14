package com.schedule.job.security.service;

import com.schedule.job.security.domain.UserInfo;
import com.schedule.job.security.domain.UserSession;
import com.schedule.job.security.manager.UserManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class UserService {
    @Autowired
    private UserManager userManager;
    /**
     * 进行登录验证
     */
    public UserSession login(String username, String password) {
        // 校验用户基本信息
        UserInfo userInfo = userManager.checkUserInfo(username, password);
        // 生成token
        String token = UUID.randomUUID().toString().replace("-", "");
        return userManager.saveUserInfo(userInfo, token);
    }

    /**
     * 登出，清除会话
     */
    public void logout(String token) {
        userManager.deleteUserSession(token);
    }

    /**
     * 用户注册
     */
    public UserInfo register(String username, String password, String email, Long roleId) {
        return userManager.createUser(username, password, email, roleId);
    }
}
