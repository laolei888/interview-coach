
ALTER TABLE interview_session
    ADD COLUMN feedback_style VARCHAR(24) NOT NULL DEFAULT 'balanced';
