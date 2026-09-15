DROP PROCEDURE IF EXISTS add_index_if_missing;
DELIMITER //
CREATE PROCEDURE add_index_if_missing(IN target_table VARCHAR(64), IN target_index VARCHAR(64), IN index_definition VARCHAR(255))
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = target_table AND index_name = target_index
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', target_table, '` ADD INDEX `', target_index, '` ', index_definition);
        PREPARE statement FROM @sql;
        EXECUTE statement;
        DEALLOCATE PREPARE statement;
    END IF;
END//
DELIMITER ;

CALL add_index_if_missing('interview_session', 'idx_session_user_created', '(`user_id`, `created_at`)');
CALL add_index_if_missing('interview_session', 'idx_session_user_status', '(`user_id`, `status`)');
CALL add_index_if_missing('resume_analysis', 'idx_resume_user_created', '(`user_id`, `created_at`)');
CALL add_index_if_missing('question', 'idx_question_user_created', '(`user_id`, `created_at`)');
CALL add_index_if_missing('interview_message', 'idx_message_session_created', '(`session_id`, `created_at`)');

DROP PROCEDURE add_index_if_missing;
