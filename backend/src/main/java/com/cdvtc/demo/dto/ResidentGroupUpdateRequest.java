package com.cdvtc.demo.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResidentGroupUpdateRequest {

    @Size(max = 100, message = "分组名称长度不能超过100")
    private String groupName;

    @Size(max = 255, message = "分组说明长度不能超过255")
    private String description;

    private Long communityId;

    private Long managerDoctorId;

    /** ACTIVE / DISABLED */
    private String status;
}
