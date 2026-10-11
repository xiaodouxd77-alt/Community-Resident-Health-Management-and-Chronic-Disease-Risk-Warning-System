package com.cdvtc.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoticeUpdateRequest {

    @NotBlank(message = "请输入公告标题")
    @Size(max = 100, message = "公告标题长度不能超过100")
    private String title;

    @NotBlank(message = "请填写公告正文")
    @Size(max = 5000, message = "公告正文长度不能超过5000")
    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime effectiveFrom;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime effectiveUntil;

    private Boolean isTop;
}
