ALTER TABLE `user`
    ADD COLUMN token_version INT NOT NULL DEFAULT 0 COMMENT 'JWT 撤销版本' AFTER password;
