CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    avatar VARCHAR(255) NULL,
    gender VARCHAR(20) DEFAULT '保密',
    email VARCHAR(100) NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    category VARCHAR(50) NOT NULL,
    question TEXT NOT NULL,
    answer TEXT NULL,
    difficulty VARCHAR(20) DEFAULT '中等',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_question_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS resume_analysis (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content MEDIUMTEXT NULL,
    ai_score INT NULL,
    ai_feedback MEDIUMTEXT NULL,
    match_position VARCHAR(200) NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_resume_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS interview_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    position VARCHAR(100) NOT NULL DEFAULT 'Java后端开发',
    status VARCHAR(20) NOT NULL DEFAULT 'active',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_user_created (user_id, created_at),
    INDEX idx_session_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS interview_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content MEDIUMTEXT NOT NULL,
    score INT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_message_session_created (session_id, created_at),
    CONSTRAINT fk_message_session FOREIGN KEY (session_id) REFERENCES interview_session(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO question (user_id, category, question, answer, difficulty)
SELECT NULL, 'Java', 'HashMap 的底层实现原理是什么？', 'HashMap 基于数组、链表和红黑树实现。默认负载因子为 0.75，链表达到阈值并满足容量条件时转换为红黑树。', '中等'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE user_id IS NULL AND question = 'HashMap 的底层实现原理是什么？');

INSERT INTO question (user_id, category, question, answer, difficulty)
SELECT NULL, '数据库', 'MySQL 为什么使用 B+ 树作为索引结构？', 'B+ 树高度低、扇出大，可减少磁盘 IO；数据集中在叶子节点且叶子节点有序连接，适合范围查询。', '中等'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE user_id IS NULL AND question = 'MySQL 为什么使用 B+ 树作为索引结构？');

INSERT INTO question (user_id, category, question, answer, difficulty)
SELECT NULL, 'Redis', '缓存穿透、击穿和雪崩分别如何治理？', '穿透可使用布隆过滤器或缓存空值；击穿可使用互斥锁或逻辑过期；雪崩可使用随机过期时间、多级缓存和限流降级。', '中等'
WHERE NOT EXISTS (SELECT 1 FROM question WHERE user_id IS NULL AND question = '缓存穿透、击穿和雪崩分别如何治理？');
