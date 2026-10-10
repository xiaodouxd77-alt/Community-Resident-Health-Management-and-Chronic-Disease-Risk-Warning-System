package com.cdvtc.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResidentGroupCreateRequest {

    @NotBlank(message = "请输入分组名称")
    @Size(max = 100, message = "分组名称长度不能超过100")
    private String groupName;

    @Size(max = 32, message = "分组编码长度不能超过32")
    private String groupCode;

    @Size(max = 255, message = "分组说明长度不能超过255")
    private String description;

    private Long communityId;

    /** 负责医生；为空时默认当前医生 */
    private Long managerDoctorId;
}
