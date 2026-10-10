package com.cdvtc.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AlertHandleRequest {

    /** START / PROGRESS / RESOLVE */
    @NotBlank(message = "处理动作不能为空")
    private String actionType;

    @NotBlank(message = "处理意见不能为空")
    @Size(max = 500, message = "处理意见长度不能超过500")
    private String handlingComment;

    private Boolean followupRequired;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextFollowupAt;
}
