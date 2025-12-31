package com.schedule.job.admin.jdbc;

import com.schedule.job.admin.config.JdbcConfigLoader;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 手写简易连接池（核心：连接复用+线程安全）
 */
@Slf4j
public class JdbcClient {
    // 活跃连接数（原子类保证线程安全）
    private static AtomicInteger atomicInteger = new AtomicInteger(0);
    // 连接队列（存储空闲连接）
    private static ConcurrentLinkedQueue<Connection> idleConnectionQueue = new ConcurrentLinkedQueue<>();
    // 连接池配置
    private final static int MAX_POOL_SIZE = Integer.parseInt(JdbcConfigLoader.getConfig("maxPoolSize"));
    private final static int MIN_IDLE = Integer.parseInt(JdbcConfigLoader.getConfig("minIdle"));
    private final static long MAX_WAIT = Long.parseLong(JdbcConfigLoader.getConfig("maxWait"));
    // 锁：保证初始化/获取连接的线程安全
    private final static ReentrantLock lock = new ReentrantLock();

    static {
        try {
            Class.forName(JdbcConfigLoader.getConfig("driverClass"));
            initMinIdleConnection();
            log.info("JDBC连接池初始化完成，最小空闲连接数：{}", MIN_IDLE);
        } catch (ClassNotFoundException | SQLException e) {
            log.error("加载数据库驱动失败", e);
            throw new RuntimeException(e);
        }
    }


    /**
     * 初始化最小空闲连接
     */
    private static void initMinIdleConnection() throws SQLException {
        for (int i = 0; i < MIN_IDLE; i++) {
            try {
                Connection conn = createNewConnection();
                idleConnectionQueue.offer(conn);
            } catch (SQLException e) {
                log.error("初始化空闲连接失败", e);
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 创建新连接（底层调用JDBC原生API）
     */
    private static Connection createNewConnection() throws SQLException {
        String url = JdbcConfigLoader.getConfig("url");
        String user = JdbcConfigLoader.getConfig("username");
        String pwd = JdbcConfigLoader.getConfig("password");
        Connection conn = DriverManager.getConnection(url, user, pwd);
        log.debug("创建新JDBC连接：{}", conn);
        return conn;
    }

    /**
     * 获取连接（核心方法）
     */
    public static Connection getConnection() {
        long startTime = System.currentTimeMillis();
        lock.lock();
        try {
            Connection conn = idleConnectionQueue.poll();
            if (conn != null) {
                atomicInteger.getAndIncrement();
                log.debug("从空闲池获取连接，耗时：{}ms", System.currentTimeMillis() - startTime);
                return conn;
            }

            if (atomicInteger.get() < MAX_POOL_SIZE) {
                conn = createNewConnection();
                atomicInteger.getAndIncrement();
                log.debug("创建新连接，耗时：{}ms", System.currentTimeMillis() - startTime);
                return conn;
            }

            long waitTime = 0;
            while (waitTime < MAX_WAIT) {
                conn = idleConnectionQueue.poll();
                if (conn != null) {
                    atomicInteger.getAndIncrement();
                    log.debug("等待后获取连接，总耗时：{}ms", System.currentTimeMillis() - startTime);
                    return conn;
                }
                Thread.sleep(1000);
                waitTime += 1000;
            }
            throw new SQLException("获取JDBC连接超时，最大等待时间：" + MAX_WAIT + "ms");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取连接被中断", e);
        } catch (SQLException e) {
            log.error("获取JDBC连接失败", e);
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 归还连接（核心：检测连接有效性，无效则销毁）
     */
    public static void releaseConnection(Connection connection) {
        if (connection == null) {
            return;
        }
        lock.lock();
        try {
            if (connection.isClosed() || !connection.isValid(3)) {
                atomicInteger.decrementAndGet();
                connection.close();
                log.debug("连接无效，销毁连接：{}", connection);
                return;
            }
            idleConnectionQueue.offer(connection);
            atomicInteger.decrementAndGet();
            log.debug("连接归还成功，当前空闲连接数：{}", idleConnectionQueue.size());
        } catch (SQLException e) {
            log.error("归还连接失败", e);
            throw new RuntimeException(e);
        }
        lock.unlock();
    }

    /**
     * 关闭连接池（销毁所有连接）
     */
    public void releaseAllConnection() {
        lock.lock();
        try {
            while (!idleConnectionQueue.isEmpty()) {
                Connection conn = idleConnectionQueue.poll();
                conn.close();
                atomicInteger.decrementAndGet();
                log.debug("销毁空闲连接：{}", conn);
            }
            log.info("连接池已关闭，所有连接销毁完成");
        } catch (SQLException e) {
            log.error("关闭连接池失败", e);
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    // 私有构造，禁止实例化
    JdbcClient() {
    }
}
