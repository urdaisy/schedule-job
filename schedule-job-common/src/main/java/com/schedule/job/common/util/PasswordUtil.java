package com.schedule.job.common.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {
    private static final Integer BCRYPT_SALT = 10;
    /**
     * 加密步骤：先生成salt再加密
     * @param rawPassword
     * @return
     */
    public static String encryptPassword(String rawPassword) {
        String salt = BCrypt.gensalt(BCRYPT_SALT);
        String encryptedPassword =  BCrypt.hashpw(rawPassword, salt);
        // 验证格式
        if (!encryptedPassword.matches("^\\$2[aby]\\$\\d{2}\\$[A-Za-z0-9\\./]{53}$")) {
            throw new RuntimeException("加密结果不符合 bcrypt 格式");
        }
        return encryptedPassword;
    }

    /**
     * 内部会自动提取salt，可直接校验密码是否匹配
     * @param rawPassword
     * @param encodePassword
     * @return
     */
    public static boolean matchPassword(String rawPassword, String encodePassword) {
        return BCrypt.checkpw(rawPassword, encodePassword);
    }
}
