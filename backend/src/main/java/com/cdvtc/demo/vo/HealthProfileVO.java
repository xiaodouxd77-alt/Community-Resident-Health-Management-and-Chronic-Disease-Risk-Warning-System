package com.cdvtc.demo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class HealthProfileVO {

    private Long id;
    private Long userId;
    private String residentNo;
    private String realName;
    private String gender;
    private String genderLabel;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    private BigDecimal height;

    /** 居民自述信息 */
    private String pastHistory;
    private String familyHistory;
    private String allergyHistory;
    private String healthRemark;

    /** 资料是否经医生核实 */
    private Boolean verified;
    private Long verifiedDoctorId;
    private String verifiedDoctorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime verifiedAt;

    private Long communityId;
    private String communityName;

    private String username;
    private String phone;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
