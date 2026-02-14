package com.schedule.job.security.repository;

import com.schedule.job.common.infra.jdbc.JdbcUtil;
import com.schedule.job.common.infra.AbstractBaseRepository;
import com.schedule.job.common.enums.BusinessExceptionCode;
import com.schedule.job.common.exception.BusinessException;
import com.schedule.job.security.domain.UserSession;
import com.schedule.job.security.entity.EntityConvert;
import com.schedule.job.security.entity.UserSessionEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
public class UserSessionRepository extends AbstractBaseRepository<UserSessionEntity, Long> {
    /**
     * 查找用户session信息
     */
    public Optional<UserSession> findByToken(String token) {
        try {
            StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tableName).append(" WHERE token=? ");
            List<UserSessionEntity> userSessionEntities =
                    JdbcUtil.executeQuery(sql.toString(), this::mapResultSetToEntity, token);
            if (userSessionEntities.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(EntityConvert.convertToUserSession(userSessionEntities.get(0)));
        } catch (Exception e) {
            throw new BusinessException(BusinessExceptionCode.USER_SESSION_NOT_FOUND);
        }
    }
}
