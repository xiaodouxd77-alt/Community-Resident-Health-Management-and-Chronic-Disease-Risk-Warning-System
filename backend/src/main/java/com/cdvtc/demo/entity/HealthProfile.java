package com.cdvtc.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("health_profile")
public class HealthProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String residentNo;

    private String realName;

    private String gender;

    private LocalDate birthDate;

    /** 以下为居民可自行维护的基础资料（自述） */
    private BigDecimal height;

    private String pastHistory;

    private String familyHistory;

    private String allergyHistory;

    private String healthRemark;

    /** 资料是否经医生核实：0-否 1-是；用于区分居民自述与医生核实 */
    private Integer verified;

    private Long verifiedDoctorId;

    private LocalDateTime verifiedAt;

    private Long communityId;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
