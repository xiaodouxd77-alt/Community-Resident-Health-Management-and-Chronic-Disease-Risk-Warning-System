package com.cdvtc.demo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConsultationVO {

    private Long id;

    private Long residentId;

    private String residentNo;

    private String residentName;

    private Long doctorId;

    private String doctorName;

    private String title;

    private String category;

    private String categoryLabel;

    private String description;

    private String status;

    private String statusLabel;

    private LocalDateTime acceptedAt;

    private LocalDateTime resolvedAt;

    private LocalDateTime closedAt;

    /** 消息条数 */
    private Integer messageCount;

    /** 最近一条消息时间 */
    private LocalDateTime lastMessageAt;

    /** 居民是否可取消（待受理） */
    private Boolean canCancel;

    /** 居民是否可继续补充消息 */
    private Boolean residentCanMessage;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
