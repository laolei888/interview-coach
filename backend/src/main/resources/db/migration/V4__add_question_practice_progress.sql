CREATE TABLE IF NOT EXISTS question_progress (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'review',
    practice_count INT NOT NULL DEFAULT 1,
    last_practiced_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_question (user_id, question_id),
    KEY idx_user_status_time (user_id, status, last_practiced_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户题目练习进度';
