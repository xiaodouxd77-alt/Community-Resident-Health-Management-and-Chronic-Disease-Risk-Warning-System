package com.cdvtc.demo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResidentGroupMemberVO {

    private Long id;
    private Long groupId;
    private Long residentId;
    private String residentNo;
    private String residentName;
    private String gender;
    private String genderLabel;
    private String communityName;
    private String remark;

    private Long addedBy;
    private String addedByName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime addedAt;
}
