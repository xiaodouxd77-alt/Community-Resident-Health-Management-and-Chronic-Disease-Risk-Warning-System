package com.cdvtc.demo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConsultationMessageVO {

    private Long id;

    private Long consultationId;

    private Long senderId;

    private String senderName;

    /** RESIDENT / DOCTOR */
    private String senderRole;

    private String senderRoleLabel;

    private String content;

    private LocalDateTime createTime;
}
