package com.cdvtc.demo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ResidentBriefVO {

    private Long id;
    private String residentNo;
    private String realName;
    private String gender;
    private String genderLabel;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    private String username;
    private String phone;
}
