# ScheduleJob
基于 Spring Boot 的分布式任务调度平台，采用 Quartz 作为核心调度引擎，Redis 实现分布式协调.<br>

任务管理
- 任务CRUD：支持创建、编辑、删除、查询任务（字段：任务名称、执行器、Cron表达式、描述、状态（启用/禁用））
- 执行器管理：注册/维护执行器（应用名称、地址列表、状态），支持自动发现
- 任务触发：手动触发执行 + Cron定时触发（支持常见Cron表达式解析、校验）

执行控制
- 失败重试：可配置重试次数（0-3次）、重试间隔（1-5分钟）
- 任务状态：运行中、已完成、失败、暂停（清晰展示）
- 并发控制：基于Redis分布式锁，避免同一任务重复执行 

日志追溯
- 执行日志记录：包含任务ID、执行时间、耗时、执行结果（成功/失败）、异常堆栈（失败时）
- 日志查询：按任务名称、时间范围、执行状态筛选查询
- 日志导出：支持导出近7天执行日志（CSV格式）

基础告警
- 失败告警：任务执行失败后，通过邮件通知指定接收人（配置收件人邮箱）
- 告警记录：记录告警时间、任务ID、告警方式、接收人

系统基础功能
- 用户登录：简单账号密码登录（默认管理员账号，支持修改密码）
- 权限控制：基础角色（管理员/普通用户），普通用户仅可查看/触发任务，管理员全权限

## Release 2025.12.31 功能说明
**搭建基础架构，实现任务CRUD和定时执行核心流程**

基本功能如下：<br>
1. 封装统一异常处理框架：GobalException和BusinessException<br>
2. JDBC + ORM + 模板方法层
* 底层 JDBC 层：借鉴 HikariCP的连接池； 
* ORM 映射层：Hibernate使用注解的方式@Entity/@Table/@Column的设计，实现注解→SQL的动态生成，动态拼接 CREATE TABLE 语句； 
* 模板方法层AbstractBaseRepository：借鉴 Spring JdbcTemplate，将 CRUD 拆分为通用模板和差异化回调；
3. Redis锁实现：解决高并发下易重复创建任务<br>
4. Quartz任务持久化：复用自定义的JdbcClient连接池<br>
* JobInfo业务持久化CRUD ，供整个系统进行Job的管理（数据面），但是定时任务Quartz无法依赖JobInfo进行调度
* 定时任务的框架持久化，保证调度的可靠性（重启不丢失、状态可追溯、集群不重复）

**编码过程总结**
1. 类型擦除及ParameterizedType
接口的参数使用泛型定义，在接口的实现类时明确参数类型，比如：
```
// 定义泛型父类
class Base<T> {}
// 子类明确指定泛型参数
lass Sub extends Base<String> {}
```
但由于Java底层编译时，编译阶段，编译器会根据泛型约束做类型校验。<br>
运行时：JVM会将声明的参数类型向上转型至其边界类型（定义String编译时转成Object）<br>
因此，使用泛型导致编译器无法感知泛型的实际类型，比如实际类型是String和Integer，运行时都会转成Object，称之为 类型擦除。
```
this.entityClass = (Class<T>)((ParameterizedType)getClass().getGenericSuperclass()).getActualTypeArguments()[0]
```
为什么ParameterizedType可以避免类型擦除的问题？
ParameterizedType是java.lang.reflect包中的接口，核心作用是保存编译时确定的泛型参数化类型，将已明确声明的泛型参数信息以原数据Metedata的方式保存在字节码中。<br>
参考：<br>
https://stackoverflow.com/questions/11067512/java-lang-class-cannot-be-cast-to-java-lang-reflect-parameterizedtype<br>
https://www.baeldung.com/java-generic-type-find-class-runtime<br>
https://developer.jboss.org/docs/DOC-13955<br>
https://forums.oracle.com/ords/apexds/post/getclass-getgenericsuperclass-problem-8618<br>
https://medium.com/egnyte-engineering/getting-runtime-type-information-from-a-generic-class-in-java-d05d6854ba5b<br>

2. 自定义注解：参考Hibernate使用注解的方式，自己重写一个。
- @Target：是声明在哪里使用，{ElementType.FIELD, ElementType.TYPE, ElementType.PARAMETER}
- @Retention：声明什么时候运行。CLASS、RUNTIME和SOURCE这三种。只有当声明为RUNTIME的时候，才能够在运行时刻通过反射API来获取到注解的信息。
- @interface：用来声明一个注解
3. JdbcConfigLoader：封装Spring 原生JDBCTemplate 手动建立JDBC连接。 
两种配置的区别：<br>
- 左边是xml写法，是传统Spring手动bean定义，仅存储配置参数。xml解析器-DOM4J<br>
- 右边是yaml写法，通过SpingBoot自动配置，屏蔽JDBC的实现细节，开箱即用，无需手动创建 Connection<br>
4. Jdbc Client手写简易连接池
底层数据库连接使用 java.sql.DriverManager方法，url/user/pwd来源于读取xml配置
```
Connection conn = DriverManager.getConnection(url, user, pwd); //建立连接
conn.close(); //关闭连接
```
连接池的基本配置：最大线程数Max_Pool、最小活跃线程数Min_Idle、最长等待时间Max_Wait<br>
维护两个变量：活跃线程数AtomicInteger和SQL连接队列Queue <br>
【创建连接】核心思路： <br>
- （1）JdbcClient静态方法初始化时，循环建立最小活跃线程数Min_Idle的SQL连接，把所有SQL连接加入连接对列Queue； <br>
- （2）在外部方法获取SQL连接时，优先从连接队列中出队（从空闲线程池中获取），当前线程池活跃线程数AtomicInteger++； <br>
- （3）连接队列为空时，说明当前建立SQL连接已经达到了最小活跃线程数Min_Idle，建立最大线程数Max_Pool的SQL连接（创建新连接），当前线程池活跃线程数AtomicInteger++； <br>
- （4）活跃线程数AtomicInteger达到最大线程数Max_Pool的连接后，等待1000ms后，从SQL连接队列Queue获取空闲SQL连接，获取后当前线程池活跃线程数AtomicInteger++; <br>

【归还连接】核心思路： <br>
- （1）连接无效或者连接关闭，活跃线程数AtomicInteger--； <br>
- （2）连接有效，把SQL连接插入连接队列Queue中，活跃线程数AtomicInteger--； <br>

5. 模版方法AbstractBaseRepository 实现 CRUD，核心思路：通过自定义注解，通过反射自动获取列名和实体的映射，拼接SQL。