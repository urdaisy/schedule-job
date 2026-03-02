# 开发文档 PartI
## 阶段一：环境准备
开发环境配置：
1、依赖配置：父项目指定groupid、artifactId、version、name和<module指定子模块，同时子项目也要正确使用<parent>声明父项目（groupid、artifactId、version、relativePath）、添加依赖项，这样子项目才会继承父项目的依赖
[图片]
2、maven compile编译失败问题：（1） 缺失的依赖版本 - mysql-connector-java 和 lombok 在子模块中声明但未在 dependencyManagement 中定义版本（2）Lombok与Java 21兼容性问题 - Lombok 1.18.30 与 Java 21 存在不兼容，导致注解处理器初始化失败。
修复方案：在主模块中 添加 mysql-connector-java 版本定义和lombok.version版本定义，maven-compiler-plugin.version 从 3.7.0 更新到 3.13.0
3、MySQL连接器编译报错： mysql:mysql-connector-java:jar:9.5.0 was not found in http://artifactory.release.ctripcorp.com/。Maven缓存了MySQL连接器（9.5.0版本）下载失败的记录，导致mysql-connector-java依赖是自己在官网下载的，存储在文件/Users/temptrip/mysql-connector-j-9.5.0/mysql-connector-j-9.5.0.jar中，需要把本地下载的文件链接到本地maven仓库中
mvn install:install-file -Dfile=/Users/temptrip/mysql-connector-j-9.5.0/mysql-connector-j-9.5.0.jar -DgroupId=mysql -DartifactId=mysql-connector-java
4、安装DBeaver，创建表
安装com.mysql.cj.jdbc.Driver，在idea中导入jar包；
[图片]
⚠️创建表字段使用反引号 `，而不是单引号
参考链接：
1、https://www.darkathena.top/docs/dbeaver-chinese-docs/zh/dbeaver/index.html#/
2、https://www.runoob.com/mysql/mysql-create-tables.html

## 阶段二：搭建项目目录
## 阶段三：AOP
封装统一异常处理框架BusinessException
## 阶段四：JDBC + ORM + Repostory
1、接口的参数使用泛型定义，在接口的实现类时明确参数类型，比如：
```java
// 定义泛型父类
class Base<T> {}
// 子类明确指定泛型参数
class Sub extends Base<String> {}
```
但由于Java底层编译时，编译阶段，编译器会根据泛型约束做类型校验。运行时：JVM会将声明的参数类型向上转型至其边界类型（定义String编译时转成Object）,因此，使用泛型导致编译器无法感知泛型的实际类型，比如实际类型是String和Integer，运行时都会转成Object。称之为 类型擦除。
this.entityClass = (Class<T>)((ParameterizedType)getClass().getGenericSuperclass()).getActualTypeArguments()[0]
为什么ParameterizedType可以避免类型擦除的问题？
ParameterizedType是java.lang.reflect包中的接口，核心作用是保存编译时确定的泛型参数化类型，将已明确声明的泛型参数信息以原数据Metedata的方式保存在字节码中。
参考：
1、https://stackoverflow.com/questions/11067512/java-lang-class-cannot-be-cast-to-java-lang-reflect-parameterizedtype
2、https://www.baeldung.com/java-generic-type-find-class-runtime
3、https://developer.jboss.org/docs/DOC-13955
4、https://forums.oracle.com/ords/apexds/post/getclass-getgenericsuperclass-problem-8618
5、https://medium.com/egnyte-engineering/getting-runtime-type-information-from-a-generic-class-in-java-d05d6854ba5b
2、自定义注解
@Target(ElementType.TYPE) 表示使用自定义的注解
@Target({}) 表示作为嵌套注解使用，不可单独使用
- @Target：是声明在哪里使用，{ElementType.FIELD, ElementType.TYPE, ElementType.PARAMETER}
- @Retention：声明什么时候运行。CLASS、RUNTIME和SOURCE这三种，分别表示注解保存在类文件、JVM运行时刻和源代码中。只有当声明为RUNTIME的时候，才能够在运行时刻通过反射API来获取到注解的信息。
- @interface：用来声明一个注解
  (1) 参考hibernate使用注解的方式，自己重写一个
  (2)在自定义类中import自定义注解
  实现SQL语法的适配+不同的数据库生成自增主键
  1、field.isAnnotationPresent(Id.class)无法识别主键注解，项目重写了ID、Column、Entity注解，所以要正确导入
  [图片]
  3、JdbcConfigLoader
  封装Spring 原生JDBCTemplate 自己建立JDBC连接。两种配置的区别：
- 左边是xml写法，是传统Spring手动bean定义，仅存储配置参数
- 右边是yaml写法，通过SpingBoot自动配置，屏蔽JDBC的实现细节，开箱即用，无需手动创建 Connection
  [图片]
  xml解析器-DOM4J
  参考：
  1、https://stackoverflow.com/questions/838518/what-is-a-connection-in-jdbc
  2、https://www.geeksforgeeks.org/java/establishing-jdbc-connection-in-java/
  3、https://www.cnblogs.com/dw3306/p/17493278.html
  4、Jdbc Client手写简易连接池
  底层数据库连接使用 java.sql.DriverManager方法，url/user/pwd来源于读取xml配置
  Connection conn = DriverManager.getConnection(url, user, pwd); //建立连接
  conn.close(); //关闭连接
  连接池的基本配置：最大线程数Max_Pool、最小活跃线程数Min_Idle、最长等待时间Max_Wait
  维护两个变量：活跃线程数AtomicInteger和SQL连接队列Queue
  【创建连接】核心思路：
  （1）JdbcClient静态方法初始化时，循环建立最小活跃线程数Min_Idle的SQL连接，把所有SQL连接加入连接对列Queue；
  （2）在外部方法获取SQL连接时，优先从连接队列中出队（从空闲线程池中获取），当前线程池活跃线程数AtomicInteger++；
  （3）连接队列为空时，说明当前建立SQL连接已经达到了最小活跃线程数Min_Idle，建立最大线程数Max_Pool的SQL连接（创建新连接），当前线程池活跃线程数AtomicInteger++；
  （4）活跃线程数AtomicInteger达到最大线程数Max_Pool的连接后，等待1000ms后，从SQL连接队列Queue获取空闲SQL连接，获取后当前线程池活跃线程数AtomicInteger++;
  【归还连接】核心思路：
  （1）连接无效或者连接关闭，活跃线程数AtomicInteger--；
  （2）连接有效，把SQL连接插入连接队列Queue中，活跃线程数AtomicInteger--；
  5、JdbcUtil
  类似于 Spring 的 JdbcTemplate，使用JdbcClient (连接池层)管理数据库连接 (Connection)，这一层位于AbstractBaseRepository和底层DB之间，
  （1）在AbstractBaseRepository中实现好的预编译语句和所需参数，执行executeUpdate/execetuteQuery/executeCountQuery操作
  （2）封装每一次业务操作，通用所需的获取链接、关闭链接、异常处理的过程
  （3）从DB返回的元祖，对应转为Java可操作的对象实体
  具体编码层面：使用PreparedStatement对象向数据库发送 SQL 语句，封装了executeUpdate和executeQuery
  官方说明文档：https://docs.oracle.com/javase/tutorial/jdbc/basics/prepared.html

6、AbstractBaseRepository 实现 CRUD
（1）设计CRUD模版，子类可自动扩展;
（2）使用自定义注解映射SQL属性
（3）使用反射动态获取DB元祖和对象的映射

## 阶段五：Redis锁实现：解决高并发下易重复创建任务
1、redis配置内容全部放在application.yml 想使用Value注解从yml读取配置内容，发现idea报错，
@Value("${spring.redis.host}") 标红并提示 Cannot find @interface method 'value()'
解决方案：
1. @Value 注解并非 JDK 原生注解，而是 Spring 框架核心注解（位于 spring-beans / spring-context 依赖中），若项目缺失对应 Spring 依赖，IDEA 无法识别该注解及其 value() 方法；
2. 若已添加依赖，可能是导入了错误的 @Value 包（如误导入 Lombok 的 @Value 而非 Spring 的 @Value），导致注解用法不匹配。

## 阶段六：Quartz任务持久化
区分两个概念：
（1）JobInfo业务持久化CRUD ，供整个系统进行Job的管理（PDCP称之为数据面）但是定时任务Quartz无法依赖JobInfo进行调度
（2）定时任务的框架持久化，保证调度的可靠性（重启不丢失、状态可追溯、集群不重复）
能不能复用自定义的JDBCClient？--- 能
如何为Quartz配置默认线程池？
SchedulerFactoryBean是Quartz定时任务执行时的调度工厂，工厂中支持自定义配置数据源setDataResource()；
官方文档：https://docs.oracle.com/en/java/javase/17/docs/api/java.sql/javax/sql/DataSource.html

## 阶段七：项目启动和验证基本功能
1、Module 'schedule-job'
must not contain source root '/Users/temptrip/java/schedule-job/schedule-job-admin/src/main/java'.
The root already belongs to module 'schedule-job-admin'，移除根目录下标记的sources文件
2、APPLICATION FAILED TO START项目启动失败：
Description: Failed to configure a DataSource: 'url' attribute is not specified and no embedded datasource could be configured.Reason: Failed to determine a suitable driver class
解决方法，要继承DataSource，实现CustomQuartzDataSource，封装自定义的JdbcClient.getConnection()。
```java
@Component
@Slf4j
public class CustomQuartzDataSource implements DataSource {
    @Override
    public Connection getConnection() throws SQLException{
        // 确保JdbcClient.getConnection()不会返回null，且能正常获取连接
        Connection connection = JdbcClient.getConnection();
        if (connection == null) {
            throw new SQLException("JdbcClient获取数据库连接失败，返回null");
        }
        log.debug("CustomQuartzDataSource获取数据库连接成功");
        return connection;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return JdbcClient.getConnection();
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
```
在QuartzConfig初始化时，将Quartz的数据源设置为new CustomQuartzDataSource
```java
@Configuration
public class QuartzConfig {
    /**
     * 配置 SchedulerFactoryBean：核心调度器（注入上面的quartzDataSource Bean）
     */
    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() throws IOException {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        // 持久化数据库
        factory.setDataSource(new CustomQuartzDataSource());
        return factory;
    }

    /**
     * 暴露 Scheduler 实例到 Spring 容器
     */
    @Bean
    public Scheduler scheduler(SchedulerFactoryBean schedulerFactoryBean) throws Exception {
        return schedulerFactoryBean.getScheduler();
    }
}
```
3、A component required a bean named 'entityManagerFactory' that could not be found.
当前项目并没有使用Quartz定时任务封装好的JPA ORM规范，所以在项目启动的时候移除了：Quartz默认数据源自动配置和要Spring默认的HikariCP
参考stackoverflow：https://stackoverflow.com/questions/48416927/spring-boot-required-a-bean-named-entitymanagerfactory-that-could-not-be-foun，修正为：@SpringBootApplication，即可解决
4、定时任务持久化报错： Could not start Quartz Scheduler，Table 'sys.qrtz_locks' doesn't exist，当前项目使用的是JDBCJobStore（数据库存储模式） 时必须手动建表，RAMJobStore（内存模式）无需建表。建表语句在Quart官方文档中jar且配置项有详细说明：
org/quartz-scheduler/quartz/2.5.0/quartz-2.5.0.jar!/org/quartz/impl/jdbcjobstore
配置项修改：org.quartz.jobStore.driverDelegateClass = org.quartz.impl.jdbcjobstore.StdJDBCDelegate
对应github开源源码：
https://github.com/quartz-scheduler/quartz/blob/main/quartz/src/main/resources/org/quartz/impl/jdbcjobstore/tables_mysql_innodb.sql
## 阶段八：解决完上述问题后顺利启动
--- 2025.12.31 完结 ---
# PartII
## 阶段一：完善执行日志
1、增加日志记录API，用于记录每次任务执行的详细信息，支持任务可追溯性。
（1）设计JobLog实体类
（2）创建JobLogRepository（继承AbstractBaseRepository），实现JobLogService接口和JobLogServiceImpl，创建JobLogController，提供日志查询API
- saveLog(JobLog log) - 保存执行日志
- findByJobId(Long jobId) - 查询某任务的所有日志
- findByJobIdAndStatus(Long jobId, Integer status) - 查询失败日志
- findRecentLogs(int limit) - 查询最近N条日志
  （3）在BaseJob中集成日志记录
- 执行前：记录开始时间
- 执行后：记录结束时间、耗时、状态（成功/失败）
- 异常时：记录错误信息
  （4）创建JobLogController，提供日志查询API
- GET /api/job/log/{jobId} - 查询任务日志列表
- GET /api/job/log/recent - 查询最近执行日志
## 阶段二：定时任务失败重试机制
  （1）在BaseJob中实现重试逻辑，在执行错误捕获异常时判断是否需要实现重试逻辑，调用子类执行不同任务类型的重试逻辑，将重试次数保存在JobDataMap中。同时记录JobLog
  （2）在JobManagerService中增加重试相关方法，待实现
- retryFailedJob(Long jobId) - 手动重试失败任务
- getFailedJobs() - 查询所有失败任务
## 阶段三：搭建前端界面
  （1）仪表盘(`/dashboard`)：通过图表展示任务总数、运行中和暂停的任务数、失败的任务数
  （2）任务详情页(`/api/job`)：操作按钮创建、编辑、删除、暂停/恢复、立即执行。创建表单字段包括任务名、任务组、描述、Cron表达式、任务参数
  （3）日志详情页(`/job/log`)：按任务筛选日志，展示任务的执行时间、耗时、状态、结果、错误信息
  可完善的部分：
  搜索和筛选功能
  Cron表达式验证和可视化
  增加Swagger API
  定时任务详细展示：任务名、任务组、Cron表达式、状态、最后执行时间
## 阶段四：定时任务重试

## 问题修复：
1、修复连接池内的JDBC连接经常被耗尽的问题
报错记录：
```html
MisfireHandler: Error handling misfires: Failed to obtain DB connection from data source 'springNonTxDataSource.schedulerFactoryBean': java.lang.RuntimeException: java.sql.SQLException: 获取JDBC连接超时，最大等待时间：30000ms
```
由于Quartz 和业务代码共享同一个连接池，且连接池大小为 10，而 Quartz 调度默认线程池有 10 个线程，容易导致连接耗尽，故为 Quartz 创建独立的连接池
如何为Quartz配置默认线程池？
SchedulerFactoryBean是Quartz定时任务执行时的调度工厂，工厂中支持自定义配置数据源setDataResource()；
官方文档：https://docs.oracle.com/en/java/javase/17/docs/api/java.sql/javax/sql/DataSource.html
2、数据库插入Job失败
报错记录：
```html
java.lang.NullPointerException: Cannot invoke "com.schedule.job.admin.entity.JobInfoEntity.getId()" because "jobInfoEntity" is null
由于AbstractBaseRepository.save() 执行 INSERT 后，未获取数据库生成的自增ID并回填到实体。 因此在convertToDomain中jobInfoEntity.getId() 仍为 null，导致后续查询失败。
```
修改方案：
在AbstractBaseRepository.save() 执行 INSERT时，根据主键注解判断两种处理方式：（1）实体存在主键id，那么JdbcUtil.java中增加插入并返回主键id的方法executeInsertAndGetId，在PreparedStatement使用 RETURN_GENERATED_KEYS 标志，pstmt执行后能够调用getGeneratedKeys()方法返回主键Id，将主键id设置回实体。（2）实体不存在主键id，使用普通更新方法
3、任务日志保存失败
   Quartz 通过反射创建实例，无法使用 Spring 依赖注入 JobLogRepository，任务执行时 jobLogRepository 为 null，导致日志无法保存。
   修改方案：（1）创建ApplicationContextHolder.java，延迟初始化，在定时任务执行时execute方法中通过 ApplicationContext 获取Bean-JobLogRepository（2）Quartz Job是new出来的，需要通过JobDataMap传递Service，实现JobLogService注入
```java
   protected JobLogRepository getJobLogRepository() {
   if (jobLogRepository == null) {
   try {
   ApplicationContext context = ApplicationContextHolder.getApplicationContext();
   jobLogRepository = context.getBean(JobLogRepository.class);
   log.debug("通过 com.schedule.job.admin.config.ApplicationContextHolder 获取 JobLogRepository 成功");
   } catch (Exception e) {
   log.error("获取 JobLogRepository 失败，任务日志将无法保存", e);
   }
   }
   return jobLogRepository;
   }
```
4、Redis连接池泄漏报错记录：
```html
Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Request processing failed: com.schedule.job.common.exception.BusinessException: 任务更新中，请稍后重试] with root cause com.schedule.job.common.exception.BusinessException: 任务更新中，请稍后重试 at com.schedule.job.admin.service.JobManagerService.updateJob(JobManagerService.java:150)
```
异常代码如下：
```java
private RLock getLock(String lockKey)  {
    String fullNameLock = RedissonConstants.LOCK_PREFIX + lockKey;
    RedissonClient redissonClient = redisConfig.createRedissonClient();
    log.debug("获取分布式锁：{}", fullNameLock);
    return redissonClient.getLock(fullNameLock);
}
/**
 * 尝试获取锁
 */
public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
    RLock lock = getLock(lockKey);
    try {
        return lock.tryLock(waitTime, leaseTime, unit);
    } catch (InterruptedException e) {
        log.error("获取锁异常", e);
        return false;
    }
}

/**
 * 释放锁
 */
public void unlock(String lockKey) {
    try {
        String fullNameLock = RedissonConstants.LOCK_PREFIX + lockKey;
        RLock lock = getLock(fullNameLock);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    } catch (Exception e) {
        log.error("释放锁异常", e);
    }
}
```
存在问题：（1）每次调用tryLock都创建新的 RedissonClient，导致资源浪费和连接泄漏 （2）释放锁时，重复添加锁前缀，根据重复锁前缀找不到对应的锁对象
总结：redis锁工具类核心只有两个方法：获取锁tryLock和释放锁unlock，根据最长等待时间waitTime和锁持有时间leaseTime加锁，释放锁要有当前持有锁的线程释放
5、日志查询失败
```html
Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Request processing failed: java.lang.RuntimeException: 结果集映射实体失败, 实体类：com.schedule.job.admin.entity.JobLogEntity异常: Can not set java.lang.Long field com.schedule.job.admin.entity.JobLogEntity.duration to java.math.BigInteger] with root cause
java.lang.RuntimeException: 结果集映射实体失败, 实体类：com.schedule.job.admin.entity.JobLogEntity异常: Can not set java.lang.Long field com.schedule.job.admin.entity.JobLogEntity.duration to java.math.BigInteger at com.schedule.job.admin.repository.AbstractBaseRepository.mapResultSetToEntity(AbstractBaseRepository.java:118) ~[classes/:na]
```
数据库的 bigint(20) 类型在 JDBC 驱动返回时，部分场景下会被解析为 BigInteger 而非 Long，而 Java 实体类的 duration 字段定义为 Long 类型，直接赋值就会出现类型不匹配的错误。修复在保存任务执行日志saveJobLog时，约束执行错误信息长度，避免过长溢出。
修改为：实现TypeConvertorUtil类型转换工具类，mapResultSetToEntity时根据目标实体的类型转换类型
--- 2026.1.31 完结 ---
# PartIII 权限控制和告警功能实现
## 阶段一：权限控制
基于角色的访问控制(RBAC)：谁(User)拥有什么样的角色(Role)拥有哪些权限(Permission)
基于属性的访问控制(ABAC)：该模型常见于分布式云系统中，属性可扩展为资源/机器/Region，即以物的属性做权限区分，而不是单指基于人的属性（角色）区分控制权限，ABAC是RBAC模型的扩展。
目标：保证安全性
数据库表设计
五张表：用户表User，角色表Role，权限表Permission，用户角色关系表User-Role，角色权限关系表RolePermission表，符合数据库设计的单一原则。
数据库表设计思路：
（1）sys_job_user、sys_job_role和sys_job_permission表中都使用了id作为主键，但是唯一约束是不用的，确定唯一约束的核心逻辑是：识别业务上 天然不允许重复的标识信息。例如sys_job_user中username用户名（登录标识）必须唯一，sys_job_role中角色编号必须唯一、sys_job_permission中权限编号必须唯一
（2）sys_job_user_role和sys_job_role_permission表使用InnoDB存储引擎的B树做联合索引
（3）sys_job_alert_config和sys_job_alert_record表中，索引可以有多个。索引设计的核心是匹配查询频率和场景，在告警记录中可以通过任务id检索、也可以用任务执行日志id检索。
（4）sys_job_permission和sys_job_role_permission使用inner join内连接，实现连表查询。
内连接/左连接/右连接语法：select table1.* from table1 inner/left/right join table2 on table1.id = table2.id where id = ?
## 阶段二：认证功能
1、加密算法：BCrypt基于 Blowfish 加密算法改造的自适应哈希算法，生成加密后密码：先生成salt再hash。在选择jar的时候，考虑到springboot-security说存在CVE安全问题，本项目使用的是org.mindrot.bcrypt.
相关链接：https://www.herodevs.com/vulnerability-directory/cve-2025-22234
2、token认证：TokenHandlerInterceptor实现springboot HandlerInterceptor接口，重写preHandler()。在 Spring MVC 中，Handler 指处理 HTTP 请求的处理器，通常是 Controller 方法。因此Handler = Controller 方法（处理请求的业务逻辑），HandlerInterceptor = 拦截 HTTP 请求的拦截器（框架层面）。
命名原则：实现什么接口，就叫什么名字。在本项目中Token验证拦截器命名为；TokenHandlerInterceptor
重写preHandler()：在进行基础校验后，查询sys_job_user_session表判断当前token是否过期，确保用户登录会话的有效性。
Token认证的整体流程：
用户登录 → 服务端生成 token + 创建 session 记录（存到数据库/Redis）→ 返回 token 给客户端 →  客户端每次请求携带 token → 服务端通过 token 查询 session 表（认证：确认用户身份+token 有效性）→  从 session/关联的权限表读取权限（鉴权）→ 允许/拒绝请求
【详细说明】
（1）token令牌，令牌的作用认证，客户端携带令牌访问服务端时，携带的令牌是双方都认识的，那么可放行；
（2）session会话。由于Http/Https是无状态协议，那么如何在客户端和服务端保持会话状态--使用session维系当前权限的使用周期；
（3）为什么分布式项目中，使用token做认证和鉴权统一：
token的payload使用base64编码，实际是以明文形式传输，包含产品类型、redis key等基本信息，拿到token后可直接访问redis、省去访问数据库的环节，并且在这个过程中直接完成了认证+鉴权。
（4）在微服务分布式部署场景下，使用redis替代数据库校验有状态的session，比如设计token过期时间30min，在30min后redis未命中，则直接说明当前sessio失效。
【核心思路】
在本项目认证+鉴权的层次设计中：Http Request -> TokenHandlerInceptor（实现springboot HandlerInterceptor接口，重写preHandler方法）实现token校验、验证token是否过期、将用户信息存入request -> Controller控制器加上自定义注解@RequirePermission("job:create")，通过解析注解验证用户权限，无权限则退出访问 ->执行业务逻辑
## 阶段三：授权功能
参考apache shiro 手写AOP实现授权功能，自定义注解 @RequirePermission("job:create")，通过拦截器/过滤器实现权限校验。
（1）自定义注解PointCut/Aspect/Around/Before/After/RequirePermission，实现解析PointCut切点接口PointcutMatcher和PointcutMatcherImpl，
参考官方实现：org.aspectj.weaver.tools下PointcutParser。
核心思路：正则表达式匹配原则，解析PointCut切点注解标柱的：返回值 包名 类名（返回值），将其保存ExecutionPointcutNode切点表达式
（2）将拦截器拦截的方法转换为切面的方法调用。采用工厂模式，使用代理工厂，根据目标类是否有接口决定JDK动态代理/CGLIB动态代理。其中，InvocationHandler是JDK 动态代理处理器，使用 method.invoke(target, args)（反射调用目标对象）；而MethodInterceptor是CGLIB 拦截器，使用 methodProxy.invokeSuper(proxy, args)（调用代理对象的父类方法）
参考官方实现：org.springframework.aop.framework下ProxyFactory
JDK和CGLIB动态代理的实现参考官方实现：org.springframework.aop.framework CglibAopProxy
（3）切面扫描和解析：扫面项目文件下的所有被Aspect注解标注的类，解析切点、切点表达式和通知，全部保存到切面实例中，最后将切面实例保存到Spring上下文，交由Spring代管。
（4）通过Spring BeanPostProcessor在Bean初始化后找到切面中被切点表达式标记的bean，该bean为目标方法，通过反射调用时会优先正常的方法先执行目标bean。
（5）创建代理对象，执行切面的鉴权方法AuthService.checkPermission()
（6）鉴权完毕后执行定时任务 CRUD
【对应上述步骤的业务流程】
1、用户前端请求 → TokenHandlerInterceptor 验证 Token，将 userId 存入 request
2、Controller 方法 → 被 PermissionAspect 拦截
3、权限验证 → 从 request 获取 userId，调用 AuthService.checkPermission() 验证
4、验证通过 → 执行目标方法
5、验证失败 → 抛出 BusinessException，返回无权限错误
【官方依据】
Spring官方文档对AOP的介绍：
https://docs.spring.io/spring-framework/docs/4.3.15.RELEASE/spring-framework-reference/html/aop.html
【定义】
1、切面：切割多个类的模块化方法，常见的切面是Transaction事务
2、连接点：执行程序时候的一个点，例如：执行的方法或者拦截的异常，在AOP中连接点代表方法的执行。连接点的概念是面向切面编程（AOP）的关键所在，它区别于那些仅提供拦截功能的传统技术。切入点使得通知可以独立于面向对象层次结构进行定位。例如，一个提供声明式事务管理的环绕通知可以应用于跨越多个对象的方法集（例如服务层中的所有业务操作）
3、通知：切面在某个连接点采取的操作，Spring中将Advice建模为拦截器，并在连接点周围维护一个拦截链。通知的类型：Before/After/Around。Around可以在方法调用前后执行自定义行为。它还负责决定是继续执行到连接点，还是通过返回自身返回值或抛出异常来跳过被建议的方法执行。
4、切点：匹配连接点的谓词，通知和切点表达式关联，通知运行在任何和切点匹配的连接点上（根据切点表达式pointCut<Adivce>执行一个方法join point）。和切点表达式匹配的连接点是AOP的核心。
5、引入：在类型上声明额外的方法或者字段
6、目标对象：被一个或多个切面通知的对象。由于Spring是运行时代理，这个目标对象就是代理对象
7、AOP代理：由AOP框架创建的对象，用于实现切面契约（例如，通知方法执行等）。在Spring框架中，AOP代理可以是JDK动态代理或CGLIB代理。Spring AOP 默认使用标准的 JDK动态代理作为 AOP 代理。
8、织入：通过其他的应用类型或者对象链接切面去建立一个通知对象，织入在编译时、加载时、运行时完成。在Spring AOP中，纯Java狂降都是在运行时织入的。
【官方文档中给出了AspectJ/AOP使用选型的建议】
位于11.4章节，Spring AOP比AspectJ更简单，如果只需要在Spring bean上执行通知的操作，那么使用AOP就可以；如果通知的对象不止由Spring管理，那么需要使用AspectJ。使用AspectJ的话，需要配套使用AspectJ language syntax或者@AspectJ注解风格，如果切面在架构设计中处于重要位置，官方推荐最好使用 AspectJ Development Tools插件（https://www.eclipse.org/projects/archives.php）
【使用注解】
1、声明切面Aspect：仅使用Aspect注解后，Spring在类路径中无法自动检测该类（在 Spring AOP 中，切面本身不能成为其他切面的通知目标。类上的Aspect注解将其标记为切面后，会将其排除在自动代理之外），需要添加一个单独的Component注解（或者，根据 Spring 组件扫描器的规则，添加一个符合要求的自定义构造型注解）--在当前项目中，手写AspectScanner扫描类，收集了被Aspect注解标识的类。
2、声明切点PointCut：匹配切点表达式的bean会被执行。官方给了个例子：任何能匹配切入点表达式的transfer方法都将执行anyOldTransfer
@Pointcut("execution(* transfer(..))")// the pointcut expression
private void anyOldTransfer() {}// the pointcut signature
切点标识符：execute/within/this/target/args/within/annotation
切点表达式：execution(modifiers-pattern? ret-type-pattern declaring-type-pattern?name-pattern(param-pattern)throws-pattern?)。支持运算符“&&”、“||”和“!”组合切入点表达式。
DNF 析取范式
3、声明通知Advice，Around递归实现/DFS

【问题修复】
1、编译报错引入注解依赖：
ava: Annotation processing is not supported for module cycles. Please ensure that all modules from cycle [schedule-job-admin,schedule-job-common] are excluded from annotation processing
解决方案：使用Module->Analyze dependencies->Analyze，找到依赖后并解决后，点击Sync Maven Project，重新加载Maven，再编译运行。
参考链接：https://stackoverflow.com/questions/27223917/how-to-configure-annotations-processing-in-intellij-idea-14-for-current-project
2、HttpMediaTypeNotAcceptableException报错：封装的统一响应体没有get属性，导致spring无法识别正确的响应体体格式，参考链接：https://stackoverflow.com/questions/28466207/could-not-find-acceptable-representation-using-spring-boot-starter-web
3、注册用户时创建角色列表失败：401 (Unauthorized)，接口被 TokenHandlerInterceptor 拦截，需要 token 验证，但注册页面访问时用户尚未登录，没有 token。修复：在 WebMvcConfig 中将 /api/login/register/roles 加入拦截器排除列表，使其无需 token 验证。
参考文档：
1、https://www.cnblogs.com/-tang/p/13220418.html
2、https://konnase.github.io/2017/11/25/java_spring/sinosteel-RequiresPermissions%E6%B3%A8%E8%A7%A3%E5%AE%9E%E7%8E%B0%E6%B5%81%E7%A8%8B/
3、shiro官方文档：https://github.com/apache/shiro/blob/main/core/src/main/java/org/apache/shiro/authz/annotation/RequiresPermissions.java

### 阶段四：告警功能
实现任务执行失败时及时通知，新增AlertService.sendAlert(JobLog log)接口，功能点：
（1）在任务失败时触发告警
- 在BaseJob中，任务失败且重试次数用尽后发送告警
（2）告警去重机制
- 同一任务短时间内多次失败，只发送一次告警
- 使用Redis记录告警状态
【编码总结】
1、获取Spring bean两种方式：通过在bean上添加Component注解，以beanName的方式 ；指定某个类，即xxxFilter.calss