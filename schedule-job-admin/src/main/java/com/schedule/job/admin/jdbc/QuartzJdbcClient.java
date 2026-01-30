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
 * Quartz专用连接池
 * 避免Quartz和业务代码相互影响
 */
@Slf4j
public class QuartzJdbcClient {
    private static AtomicInteger atomicInteger = new AtomicInteger(0);
    private static ConcurrentLinkedQueue<Connection> idleConnectionQueue = new ConcurrentLinkedQueue<>();
    private final static int MAX_POOL_SIZE = 10;
    private final static int MIN_IDLE = 2;
    private final static long MAX_WAIT = 10000;
    private final static ReentrantLock lock = new ReentrantLock();

    static {
        try {
            Class.forName(JdbcConfigLoader.getConfig("driverClass"));
            initMinIdleConnection();
            log.info("Quartz JDBC连接池初始化完成，最小空闲连接数：{}", MIN_IDLE);
        } catch (ClassNotFoundException | SQLException e) {
            log.error("加载Quartz数据库驱动失败", e);
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
                log.error("初始化Quartz空闲连接失败", e);
                throw new RuntimeException(e);
            }
        }
        log.info("Quartz连接池初始化完成，空闲连接数：{}，活跃连接数：{}", idleConnectionQueue.size(), atomicInteger.get());
    }

    /**
     * 创建新连接
     */
    private static Connection createNewConnection() throws SQLException {
        String url = JdbcConfigLoader.getConfig("url");
        String user = JdbcConfigLoader.getConfig("username");
        String pwd = JdbcConfigLoader.getConfig("password");
        Connection conn = DriverManager.getConnection(url, user, pwd);
        log.debug("创建新Quartz JDBC连接：{}", conn);
        return conn;
    }

    /**
     * 获取连接
     */
    public static Connection getConnection() {
        long startTime = System.currentTimeMillis();
        lock.lock();
        try {
            // 1. 尝试从空闲池获取连接
            Connection conn = null;
            while ((conn = idleConnectionQueue.poll()) != null) {
                try {
                    if (!conn.isClosed() && conn.isValid(3)) {
                        atomicInteger.getAndIncrement();
                        log.debug("Quartz从空闲池获取连接，耗时：{}ms，当前活跃连接数：{}，空闲连接数：{}", 
                                System.currentTimeMillis() - startTime, atomicInteger.get(), idleConnectionQueue.size());
                        return conn;
                    } else {
                        log.warn("Quartz从空闲池获取的连接无效，已销毁");
                        try {
                            conn.close();
                        } catch (SQLException e) {
                            // ignore
                        }
                    }
                } catch (SQLException e) {
                    log.warn("验证Quartz连接有效性异常", e);
                    try {
                        conn.close();
                    } catch (SQLException ex) {
                        // ignore
                    }
                }
            }

            // 2. 如果连接池未满，创建新连接
            if (atomicInteger.get() < MAX_POOL_SIZE) {
                try {
                    conn = createNewConnection();
                    atomicInteger.getAndIncrement();
                    log.debug("Quartz创建新连接，耗时：{}ms，当前活跃连接数：{}，空闲连接数：{}", 
                            System.currentTimeMillis() - startTime, atomicInteger.get(), idleConnectionQueue.size());
                    return conn;
                } catch (SQLException e) {
                    log.error("Quartz创建新连接失败", e);
                    throw new RuntimeException("创建Quartz数据库连接失败", e);
                }
            }

            // 3. 连接池已满，等待连接归还
            long waitTime = 0;
            long waitInterval = 200;
            while (waitTime < MAX_WAIT) {
                lock.unlock();
                try {
                    Thread.sleep(waitInterval);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("获取Quartz连接被中断", e);
                }
                lock.lock();
                
                // 再次尝试获取连接
                while ((conn = idleConnectionQueue.poll()) != null) {
                    try {
                        if (!conn.isClosed() && conn.isValid(3)) {
                            atomicInteger.getAndIncrement();
                            log.debug("Quartz等待后获取连接，总耗时：{}ms", System.currentTimeMillis() - startTime);
                            return conn;
                        } else {
                            try {
                                conn.close();
                            } catch (SQLException e) {
                                // ignore
                            }
                        }
                    } catch (SQLException e) {
                        try {
                            conn.close();
                        } catch (SQLException ex) {
                            // ignore
                        }
                    }
                }
                
                // 如果连接池未满，尝试创建新连接
                if (atomicInteger.get() < MAX_POOL_SIZE) {
                    try {
                        conn = createNewConnection();
                        atomicInteger.getAndIncrement();
                        log.debug("Quartz等待期间创建新连接，总耗时：{}ms", System.currentTimeMillis() - startTime);
                        return conn;
                    } catch (SQLException e) {
                        log.error("Quartz创建新连接失败", e);
                    }
                }
                
                waitTime += waitInterval;
            }
            
            // 超时
            log.error("获取Quartz JDBC连接超时，最大等待时间：{}ms，当前活跃连接数：{}，空闲连接数：{}，最大连接数：{}。可能原因：1)连接未正确归还 2)连接池配置过小 3)数据库连接异常", 
                    MAX_WAIT, atomicInteger.get(), idleConnectionQueue.size(), MAX_POOL_SIZE);
            // 尝试清理无效连接
            cleanupInvalidConnections();
            throw new SQLException("获取Quartz JDBC连接超时，最大等待时间：" + MAX_WAIT + "ms");
        } catch (SQLException e) {
            log.error("获取Quartz JDBC连接失败", e);
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 归还连接
     */
    public static void releaseConnection(Connection connection) {
        if (connection == null) {
            return;
        }
        lock.lock();
        try {
            boolean isValid = true;
            try {
                if (connection.isClosed()) {
                    isValid = false;
                } else {
                    isValid = connection.isValid(3);
                }
            } catch (SQLException e) {
                log.warn("检查Quartz连接有效性时异常，视为无效连接", e);
                isValid = false;
            }
            
            if (!isValid) {
                try {
                    if (!connection.isClosed()) {
                        connection.close();
                    }
                } catch (SQLException e) {
                    log.warn("关闭无效Quartz连接时异常", e);
                }
                atomicInteger.decrementAndGet();
                log.warn("Quartz连接无效已销毁，当前活跃连接数：{}，空闲连接数：{}", 
                        atomicInteger.get(), idleConnectionQueue.size());
                
                // 如果连接池未满，尝试创建新连接补充
                if (atomicInteger.get() < MAX_POOL_SIZE) {
                    try {
                        Connection newConn = createNewConnection();
                        idleConnectionQueue.offer(newConn);
                        log.info("已创建新Quartz连接补充连接池，当前空闲连接数：{}", idleConnectionQueue.size());
                    } catch (SQLException e) {
                        log.error("补充Quartz连接失败", e);
                    }
                }
                return;
            }
            
            // 连接有效，归还到队列
            idleConnectionQueue.offer(connection);
            atomicInteger.decrementAndGet();
            log.debug("Quartz连接归还成功，当前活跃连接数：{}，空闲连接数：{}", 
                    atomicInteger.get(), idleConnectionQueue.size());
        } catch (Exception e) {
            log.error("归还Quartz连接失败", e);
            atomicInteger.decrementAndGet();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 清理无效连接（当连接池出现问题时调用）
     */
    private static void cleanupInvalidConnections() {
        lock.lock();
        try {
            int cleaned = 0;
            Connection conn;
            while ((conn = idleConnectionQueue.poll()) != null) {
                try {
                    if (conn.isClosed() || !conn.isValid(1)) {
                        cleaned++;
                        try {
                            conn.close();
                        } catch (SQLException e) {
                            // ignore
                        }
                    } else {
                        // 连接有效，放回队列
                        idleConnectionQueue.offer(conn);
                    }
                } catch (SQLException e) {
                    cleaned++;
                    try {
                        conn.close();
                    } catch (SQLException ex) {
                        // ignore
                    }
                }
            }
            if (cleaned > 0) {
                log.warn("清理了{}个无效的Quartz连接", cleaned);
            }
        } finally {
            lock.unlock();
        }
    }

    // 私有构造，禁止实例化
    private QuartzJdbcClient() {
    }
}
