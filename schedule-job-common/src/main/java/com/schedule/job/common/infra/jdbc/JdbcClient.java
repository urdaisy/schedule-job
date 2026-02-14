package com.schedule.job.common.infra.jdbc;

import com.schedule.job.common.config.JdbcConfigLoader;
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
        Connection conn = null; // 在方法作用域声明，供后续使用
        lock.lock();
        try {
            // 1. 尝试从空闲池获取连接（验证有效性）
            while ((conn = idleConnectionQueue.poll()) != null) {
                try {
                    if (!conn.isClosed() && conn.isValid(3)) {
                        atomicInteger.getAndIncrement();
                        log.debug("从空闲池获取连接，耗时：{}ms，当前活跃连接数：{}，空闲连接数：{}", 
                                System.currentTimeMillis() - startTime, atomicInteger.get(), idleConnectionQueue.size());
                        return conn;
                    } else {
                        log.warn("从空闲池获取的连接无效，已销毁");
                        try {
                            conn.close();
                        } catch (SQLException e) {
                            // ignore
                        }
                        atomicInteger.decrementAndGet(); // 修正计数
                    }
                } catch (SQLException e) {
                    log.warn("验证连接有效性异常", e);
                    try {
                        conn.close();
                    } catch (SQLException ex) {
                        // ignore
                    }
                    atomicInteger.decrementAndGet(); // 修正计数
                }
            }

            // 2. 如果连接池未满，创建新连接
            if (atomicInteger.get() < MAX_POOL_SIZE) {
                try {
                    conn = createNewConnection();
                    atomicInteger.getAndIncrement();
                    log.debug("创建新连接，耗时：{}ms，当前活跃连接数：{}，空闲连接数：{}", 
                            System.currentTimeMillis() - startTime, atomicInteger.get(), idleConnectionQueue.size());
                    return conn;
                } catch (SQLException e) {
                    log.error("创建新连接失败", e);
                    throw new RuntimeException("创建JDBC数据库连接失败", e);
                }
            }
        } finally {
            lock.unlock();
        }

        // 3. 连接池已满，等待连接归还（必须在锁外等待，否则其他线程无法归还连接）
        long waitTime = 0;
        long waitInterval = 200; // 减少等待间隔，提高响应速度
        while (waitTime < MAX_WAIT) {
            lock.lock();
            try {
                // 再次尝试获取连接
                conn = idleConnectionQueue.poll();
                if (conn != null) {
                    try {
                        if (!conn.isClosed() && conn.isValid(3)) {
                            atomicInteger.getAndIncrement();
                            log.debug("等待后获取连接，总耗时：{}ms，当前活跃连接数：{}，空闲连接数：{}", 
                                    System.currentTimeMillis() - startTime, atomicInteger.get(), idleConnectionQueue.size());
                            return conn;
                        } else {
                            try {
                                conn.close();
                            } catch (SQLException e) {
                                // ignore
                            }
                            atomicInteger.decrementAndGet();
                        }
                    } catch (SQLException e) {
                        try {
                            conn.close();
                        } catch (SQLException ex) {
                            // ignore
                        }
                        atomicInteger.decrementAndGet();
                    }
                }
                
                // 如果连接池未满，尝试创建新连接
                if (atomicInteger.get() < MAX_POOL_SIZE) {
                    try {
                        conn = createNewConnection();
                        atomicInteger.getAndIncrement();
                        log.debug("等待期间创建新连接，总耗时：{}ms", System.currentTimeMillis() - startTime);
                        return conn;
                    } catch (SQLException e) {
                        log.error("创建新连接失败", e);
                    }
                }
            } finally {
                lock.unlock();
            }
            
            // 释放锁后等待，让其他线程有机会归还连接
            try {
                Thread.sleep(waitInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("获取连接被中断", e);
            }
            waitTime += waitInterval;
        }
        
        // 超时
        log.error("获取JDBC连接超时，最大等待时间：{}ms，当前活跃连接数：{}，空闲连接数：{}，最大连接数：{}", 
                MAX_WAIT, atomicInteger.get(), idleConnectionQueue.size(), MAX_POOL_SIZE);
        throw new RuntimeException("获取JDBC连接超时，最大等待时间：" + MAX_WAIT + "ms");
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
            boolean isValid = true;
            try {
                if (connection.isClosed()) {
                    isValid = false;
                } else {
                    isValid = connection.isValid(3);
                }
            } catch (SQLException e) {
                log.warn("检查连接有效性时异常，视为无效连接", e);
                isValid = false;
            }
            
            if (!isValid) {
                try {
                    if (!connection.isClosed()) {
                        connection.close();
                    }
                } catch (SQLException e) {
                    log.warn("关闭无效连接时异常", e);
                }
                atomicInteger.decrementAndGet();
                log.warn("连接无效已销毁，当前活跃连接数：{}，空闲连接数：{}", 
                        atomicInteger.get(), idleConnectionQueue.size());
                
                // 如果连接池未满，尝试创建新连接补充
                if (atomicInteger.get() < MAX_POOL_SIZE) {
                    try {
                        Connection newConn = createNewConnection();
                        idleConnectionQueue.offer(newConn);
                        log.info("已创建新连接补充连接池，当前空闲连接数：{}", idleConnectionQueue.size());
                    } catch (SQLException e) {
                        log.error("补充连接失败", e);
                    }
                }
                return;
            }
            
            // 连接有效，归还到队列
            idleConnectionQueue.offer(connection);
            atomicInteger.decrementAndGet();
            log.debug("连接归还成功，当前活跃连接数：{}，空闲连接数：{}", 
                    atomicInteger.get(), idleConnectionQueue.size());
        } catch (Exception e) {
            log.error("归还连接失败", e);
            atomicInteger.decrementAndGet(); // 确保计数正确
        } finally {
            lock.unlock(); // 确保锁总是被释放
        }
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
