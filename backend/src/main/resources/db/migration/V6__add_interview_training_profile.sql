ALTER TABLE interview_session
    ADD COLUMN interview_mode VARCHAR(30) NOT NULL DEFAULT 'technical',
    ADD COLUMN difficulty_level VARCHAR(30) NOT NULL DEFAULT 'intermediate',
    ADD COLUMN target_rounds INT NOT NULL DEFAULT 5,
    ADD INDEX idx_session_user_mode (`user_id`, `interview_mode`);
