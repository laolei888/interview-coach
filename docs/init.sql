-- =====================================================
-- AI 智能面试辅导平台 - 数据库初始化脚本
-- 用 Navicat 或 MySQL 命令行执行此文件
-- =====================================================

-- 创建数据库（Navicat 中手动创建也可以）
CREATE DATABASE IF NOT EXISTS interview_coach
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE interview_coach;

-- =====================================================
-- 0. 用户表
-- =====================================================
CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码摘要（BCrypt）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- =====================================================
-- 1. 面试题库
-- =====================================================
CREATE TABLE IF NOT EXISTS question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL COMMENT '所属用户 ID，NULL 表示公共预置题',
    category VARCHAR(50) NOT NULL COMMENT '分类',
    question TEXT NOT NULL COMMENT '题目',
    answer TEXT COMMENT '参考答案',
    difficulty VARCHAR(20) DEFAULT '中等' COMMENT '难度',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试题库';

-- 预置题目
INSERT INTO question (category, question, answer, difficulty) VALUES
('Java', 'HashMap 的底层实现原理是什么？', 'HashMap 基于数组+链表+红黑树实现。通过 key 的 hashCode 计算桶位置，相同位置的元素以链表形式存储；当链表长度超过 8 且数组长度大于 64 时，链表转为红黑树以提高查询效率。默认负载因子 0.75，扩容时容量翻倍。', '中等'),
('Java', '什么是 Java 内存模型（JMM）？', 'JMM 定义了多线程环境下共享变量的访问规则，主要通过主内存和工作内存的抽象来保证可见性、有序性和原子性。volatile 保证可见性和禁止指令重排，synchronized 保证原子性和可见性。', '困难'),
('Java', 'Spring 的依赖注入有哪些方式？', '1. 构造器注入（推荐，不可变且依赖明确）2. Setter 注入（可选依赖）3. 字段注入（@Autowired，简洁但不推荐用于测试）4. 方法注入。Spring 4.3+ 单构造器可省略 @Autowired。', '简单'),
('数据库', 'MySQL 索引的底层数据结构是什么？为什么用 B+ 树？', 'B+ 树。原因：1. 所有数据在叶子节点，查询稳定；2. 叶子节点形成双向链表，适合范围查询；3. 树高度低，减少磁盘 IO；4. 非叶子节点只存索引，一次 IO 可加载更多 key。', '中等'),
('数据库', '什么是事务的 ACID 特性？', 'Atomicity（原子性）：事务要么全部成功要么全部回滚；Consistency（一致性）：事务前后数据一致；Isolation（隔离性）：并发事务互不干扰；Durability（持久性）：提交后数据永久保存。', '简单'),
('Redis', 'Redis 缓存穿透、击穿、雪崩分别是什么？如何解决？', '穿透：查询不存在的数据，请求打到 DB → 布隆过滤器或缓存空值；击穿：热点 key 过期 → 互斥锁或永不过期；雪崩：大量 key 同时过期 → 过期时间加随机值、多级缓存、限流降级。', '中等'),
('Spring', 'Spring Boot 自动配置原理是什么？', '@SpringBootApplication 包含 @EnableAutoConfiguration，通过 @Import 导入 AutoConfigurationImportSelector，扫描 META-INF/spring.factories 中的配置类，结合 @Conditional 条件注解选择性加载 Bean。', '中等'),
('计算机网络', 'HTTPS 的握手过程是怎样的？', '1. 客户端发送支持的加密套件+随机数；2. 服务端选加密套件+随机数+证书；3. 客户端验证证书，生成 pre-master secret 用公钥加密发送；4. 双方用三个随机数生成对称密钥；5. 确认握手完成，后续对称加密通信。', '困难');

-- =====================================================
-- 2. 简历分析记录表
-- =====================================================
DROP TABLE IF EXISTS resume_analysis;
CREATE TABLE resume_analysis (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '所属用户 ID',
    file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    content TEXT COMMENT 'PDF 解析出的纯文本内容',
    ai_score INT COMMENT 'AI 综合评分（0-100）',
    ai_feedback TEXT COMMENT 'AI 完整反馈（包含亮点/不足/建议/岗位推荐）',
    match_position VARCHAR(100) COMMENT 'AI 推荐的匹配岗位',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简历分析记录';

-- =====================================================
-- 2. 面试会话表
-- =====================================================
DROP TABLE IF EXISTS interview_session;
CREATE TABLE interview_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '所属用户 ID',
    position VARCHAR(100) NOT NULL DEFAULT 'Java后端开发' COMMENT '面试岗位',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态: active=进行中, completed=已结束',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试会话';

-- =====================================================
-- 3. 面试对话消息表
-- =====================================================
DROP TABLE IF EXISTS interview_message;
CREATE TABLE interview_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    session_id BIGINT NOT NULL COMMENT '所属面试会话 ID',
    role VARCHAR(20) NOT NULL COMMENT '角色: ai=面试官, user=候选人',
    content TEXT NOT NULL COMMENT '消息内容',
    score INT COMMENT 'AI 对用户回答的评分（1-10），仅 user 回答时有值',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_session_id (session_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试对话消息';

-- 插入一条测试数据（可选）
-- INSERT INTO interview_session (position, status) VALUES ('Java后端开发', 'active');
