package com.cdvtc.demo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ChronicDiseaseVO {

    private Long id;
    private Long residentId;
    private String residentNo;
    private String residentName;

    private String diseaseType;
    private String diseaseTypeLabel;

    private String diagnosisStatus;
    private String diagnosisStatusLabel;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate diagnosisDate;

    private String diagnosisBasis;

    private String managementStatus;
    private String managementStatusLabel;

    private Long managingDoctorId;
    private String managingDoctorName;

    private Long reviewedBy;
    private String reviewedByName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewedAt;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
