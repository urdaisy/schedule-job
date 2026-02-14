package com.schedule.job.admin.jdbc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

@Component
@Slf4j
public class CustomQuartzDataSource implements DataSource {
    @Override
    public Connection getConnection() throws SQLException{
        Connection connection = QuartzJdbcClient.getConnection();
        if (connection == null) {
            throw new SQLException("QuartzJdbcClient获取数据库连接失败，返回null");
        }
        log.debug("CustomQuartzDataSource获取数据库连接成功，包装为PooledConnection");
        // 包装为PooledConnection，确保close()时归还到连接池而不是真正关闭
        return new PooledConnection(connection, true);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection connection = QuartzJdbcClient.getConnection();
        if (connection == null) {
            throw new SQLException("QuartzJdbcClient获取数据库连接失败，返回null");
        }
        // 包装为PooledConnection，确保close()时归还到连接池而不是真正关闭
        return new PooledConnection(connection, true);
    }
    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return null;
    }
    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {

    }
    @Override
    public void setLoginTimeout(int seconds) throws SQLException {

    }
    @Override
    public int getLoginTimeout() throws SQLException {
        return 0;
    }
    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }


    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }
}
