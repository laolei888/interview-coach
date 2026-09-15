package com.interviewcoach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    /** 头像 emoji */
    private String avatar;

    /** 性别：男/女/保密 */
    private String gender;

    /** 邮箱 */
    private String email;

    private LocalDateTime createdAt;
}
