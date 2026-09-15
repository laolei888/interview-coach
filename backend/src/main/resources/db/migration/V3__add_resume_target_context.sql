ALTER TABLE resume_analysis
    ADD COLUMN target_position VARCHAR(100) NULL COMMENT '目标岗位' AFTER file_name,
    ADD COLUMN job_description TEXT NULL COMMENT '目标岗位 JD' AFTER target_position;
