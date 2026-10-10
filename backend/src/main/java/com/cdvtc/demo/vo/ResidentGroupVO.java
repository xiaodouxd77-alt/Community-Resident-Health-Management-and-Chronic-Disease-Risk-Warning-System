package com.cdvtc.demo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResidentGroupVO {

    private Long id;
    private String groupName;
    private String groupCode;
    private String description;

    private Long communityId;
    private String communityName;

    private Long managerDoctorId;
    private String managerDoctorName;

    private Long createdBy;
    private String creatorName;

    private String status;
    private String statusLabel;

    private Long memberCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
