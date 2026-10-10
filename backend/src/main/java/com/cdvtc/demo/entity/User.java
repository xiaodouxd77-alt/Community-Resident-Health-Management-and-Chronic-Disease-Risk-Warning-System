package com.cdvtc.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String realName; // 展示用姓名，为空时回退 username
    private String email;
    private String phone;
    private Integer status; // 0-禁用 1-启用
    private String role; // RESIDENT-居民 DOCTOR-社区医生 ADMIN-管理员

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 展示名：优先真实姓名，缺省回退登录名 */
    public String displayName() {
        return (realName != null && !realName.isBlank()) ? realName : username;
    }
}