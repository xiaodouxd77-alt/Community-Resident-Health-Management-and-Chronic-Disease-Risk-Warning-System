package com.cdvtc.demo.enums;

/**
 * 系统角色。注册接口固定创建 RESIDENT，不允许用户自行提升角色。
 */
public enum RoleEnum {

    RESIDENT("居民"),
    DOCTOR("社区医生"),
    ADMIN("管理员");

    private final String label;

    RoleEnum(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static RoleEnum of(String role) {
        if (role == null) {
            return RESIDENT;
        }
        for (RoleEnum r : values()) {
            if (r.name().equalsIgnoreCase(role.trim())) {
                return r;
            }
        }
        return RESIDENT;
    }
}
