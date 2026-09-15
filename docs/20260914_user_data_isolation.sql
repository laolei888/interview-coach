-- =====================================================
-- 用户数据权限隔离迁移脚本
-- 适用于已存在 interview_coach 数据库的环境，可重复执行
-- =====================================================

USE interview_coach;

-- BCrypt 摘要长度为 60，预留空间供后续算法升级。
ALTER TABLE `user` MODIFY COLUMN password VARCHAR(100) NOT NULL COMMENT '密码摘要（BCrypt）';

-- 为历史数据选择一个默认归属用户，避免添加 NOT NULL 字段失败。
SET @default_user_id := (SELECT id FROM `user` ORDER BY id ASC LIMIT 1);

DELIMITER //
DROP PROCEDURE IF EXISTS require_default_user_for_isolation;
CREATE PROCEDURE require_default_user_for_isolation()
BEGIN
    IF @default_user_id IS NULL AND (
        EXISTS (SELECT 1 FROM resume_analysis LIMIT 1)
        OR EXISTS (SELECT 1 FROM interview_session LIMIT 1)
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '用户表为空，无法为历史简历/面试数据设置默认归属用户';
    END IF;
END//
DELIMITER ;

CALL require_default_user_for_isolation();
DROP PROCEDURE require_default_user_for_isolation;

DROP PROCEDURE IF EXISTS add_column_if_missing;
DELIMITER //
CREATE PROCEDURE add_column_if_missing(IN target_table VARCHAR(64), IN target_column VARCHAR(64), IN column_definition VARCHAR(255))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = target_table AND column_name = target_column) THEN
        SET @sql := CONCAT('ALTER TABLE `', target_table, '` ADD COLUMN `', target_column, '` ', column_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CALL add_column_if_missing('resume_analysis', 'user_id', 'BIGINT NULL COMMENT ''所属用户 ID'' AFTER `id`');
CALL add_column_if_missing('interview_session', 'user_id', 'BIGINT NULL COMMENT ''所属用户 ID'' AFTER `id`');
CALL add_column_if_missing('question', 'user_id', 'BIGINT NULL COMMENT ''所属用户 ID，NULL 表示公共预置题'' AFTER `id`');
DROP PROCEDURE add_column_if_missing;

UPDATE resume_analysis
SET user_id = @default_user_id
WHERE user_id IS NULL;

ALTER TABLE resume_analysis
    MODIFY COLUMN user_id BIGINT NOT NULL COMMENT '所属用户 ID',
    ADD INDEX idx_user_id (user_id);

UPDATE interview_session
SET user_id = @default_user_id
WHERE user_id IS NULL;

ALTER TABLE interview_session
    MODIFY COLUMN user_id BIGINT NOT NULL COMMENT '所属用户 ID',
    ADD INDEX idx_user_id (user_id);

DROP PROCEDURE IF EXISTS add_index_if_missing;
DELIMITER //
CREATE PROCEDURE add_index_if_missing(IN target_table VARCHAR(64), IN target_index VARCHAR(64), IN index_definition VARCHAR(255))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = target_table AND index_name = target_index) THEN
        SET @sql := CONCAT('ALTER TABLE `', target_table, '` ADD INDEX `', target_index, '` ', index_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//
DELIMITER ;

CALL add_index_if_missing('resume_analysis', 'idx_user_id', '(`user_id`)');
CALL add_index_if_missing('interview_session', 'idx_user_id', '(`user_id`)');
CALL add_index_if_missing('question', 'idx_user_id', '(`user_id`)');
DROP PROCEDURE add_index_if_missing;
