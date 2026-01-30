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
            if (params != null && params.length > 0) {
                for (int i = 0; i < params.length; i++) {
                    pstmt.setObject(i+1, params[i]);
                }
            }
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            log.error("数据库更新失败", e);
            throw new RuntimeException("执行更新SQL失败", e);
        } finally {
            if (pstmt != null) {
                pstmt.close();
            }
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
        ResultSet resultSet = null;
        try {
            conn = JdbcClient.getConnection();
            pstmt = conn.prepareStatement(sql);
            // 支持无参数查询
            if (params != null && params.length > 0) {
                for (int i = 0; i < params.length; i++) {
                    pstmt.setObject(i+1, params[i]);
                }
            }
            resultSet = pstmt.executeQuery();
            List<T> list = new ArrayList<>();
            while (resultSet.next()) {
                list.add(mapper.apply(resultSet));
            }
            log.info("执行SQL[{}]成功，返回行数：{}，耗时：{}ms，参数：{}",
                    sql, list.size(), System.currentTimeMillis() - start, params);
            return list;
        } catch (SQLException e) {
            log.error("数据库查询失败", e);
            throw new RuntimeException("执行查询SQL失败", e);
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (pstmt != null) {
                pstmt.close();
            }
            JdbcClient.releaseConnection(conn);
        }
    }

    /**
     * 查询总数
     */
    public static Long executeCountQuery(String sql, Object... params) throws SQLException {
        long start = System.currentTimeMillis();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet resultSet = null;
        try {
            conn = JdbcClient.getConnection();
            pstmt = conn.prepareStatement(sql);
            if (params != null && params.length > 0) {
                for (int i = 0; i < params.length; i++) {
                    pstmt.setObject(i+1, params[i]);
                }
            }
            resultSet = pstmt.executeQuery();
            if (resultSet.next()) {
                Long count = resultSet.getLong(1);
                log.info("执行COUNT查询[{}]成功，结果：{}，耗时：{}ms，参数：{}",
                        sql, count, System.currentTimeMillis() - start, params);
                return count;
            } else {
                log.warn("执行COUNT查询[{}]未返回结果，返回0，参数：{}", sql, params);
                return 0L;
            }
        } catch (SQLException e) {
            log.error("数据库查询总数失败", e);
            throw new RuntimeException("执行查询SQL总数失败", e);
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (pstmt != null) {
                pstmt.close();
            }
            JdbcClient.releaseConnection(conn);
        }
    }
}
