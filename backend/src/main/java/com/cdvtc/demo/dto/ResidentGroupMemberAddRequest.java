package com.cdvtc.demo.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 向分组批量添加居民。 */
@Data
public class ResidentGroupMemberAddRequest {

    @NotEmpty(message = "请选择要添加的居民")
    private List<Long> residentIds;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}
