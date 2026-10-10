package com.cdvtc.demo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChronicHistoryVO {

    private Long id;
    private String action;
    private String actionLabel;
    private String detail;
    private Long operatorId;
    private String operatorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
