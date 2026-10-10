package com.cdvtc.demo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HandlingRecordVO {

    private Long id;
    private Long doctorId;
    private String doctorName;
    private String actionType;
    private String actionLabel;
    private String handlingComment;
    private Boolean followupRequired;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextFollowupAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
