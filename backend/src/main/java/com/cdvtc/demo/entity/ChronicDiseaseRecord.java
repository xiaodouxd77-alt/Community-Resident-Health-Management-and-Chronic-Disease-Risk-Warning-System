package com.cdvtc.demo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 慢病患者建档记录。由医生依据已有诊断资料建档，不等于风险评估结论。
 */
@Data
@TableName("chronic_disease_record")
public class ChronicDiseaseRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long residentId;

    /** HYPERTENSION / DIABETES / OTHER */
    private String diseaseType;

    /** PENDING_REVIEW / CONFIRMED / REJECTED */
    private String diagnosisStatus;

    private LocalDate diagnosisDate;

    private String diagnosisBasis;

    /** ACTIVE / SUSPENDED / CLOSED */
    private String managementStatus;

    private Long managingDoctorId;

    private Long reviewedBy;

    private LocalDateTime reviewedAt;

    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
