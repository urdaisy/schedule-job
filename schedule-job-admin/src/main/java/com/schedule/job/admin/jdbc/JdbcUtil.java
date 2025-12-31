package com.schedule.job.admin.jdbc;

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * JDBC工具类，封装通用操作
 */
@Slf4j
public class JdbcUtil {
    public JdbcUtil() {}
    /**
     * 执行增删改
     */
    public static int executeUpdate(String sql, Object... params) throws SQLException {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = JdbcClient.getConnection();
            pstmt = conn.prepareStatement(sql);
            if (params == null || params.length == 0) {
                return 0;
            }
            for (int i = 0; i < params.length; i++) {
                // parameterIndex is 1,2,3...
                pstmt.setObject(i+1, params[i]);
            }
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            log.info("数据库更新失败");
            throw new RuntimeException("执行更新SQL失败", e);
        } finally {
            pstmt.close();
            JdbcClient.releaseConnection(conn);
        }
    }

    /**
     * 执行查询
     */
    public static <T> List<T> executeQuery(String sql, Function<ResultSet, T> mapper, Object... params)
            throws SQLException {
        long start = System.currentTimeMillis();
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = JdbcClient.getConnection();
            pstmt = conn.prepareStatement(sql);
            if (params == null || params.length == 0) {
                return null;
            }
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i+1, params[i]);
            }
            ResultSet resultSet = pstmt.executeQuery();
            List<T> list = new ArrayList<>();
            while (resultSet.next()) {
                list.add(mapper.apply(resultSet));
            }
            log.info("执行SQL[{}]成功，返回行数：{}，耗时：{}ms，参数：{}",
                    sql, list.size(), System.currentTimeMillis() - start, params);
            return list;
        } catch (SQLException e) {
            log.info("数据库查询失败");
            throw new RuntimeException("执行查询SQL失败", e);
        } finally {
            pstmt.close();
            JdbcClient.releaseConnection(conn);
        }
    }

}
