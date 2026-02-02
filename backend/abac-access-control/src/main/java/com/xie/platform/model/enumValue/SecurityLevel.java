package com.xie.platform.model.enumValue;

public enum SecurityLevel {
    PUBLIC(1, "公开"),
    INTERNAL(2, "内部"),
    CONFIDENTIAL(3, "机密"),
    TOP_SECRET(4, "高度机密");

    private final int level;
    private final String desc;

    SecurityLevel(int level, String desc) {
        this.level = level;
        this.desc = desc;
    }

    public int getLevel() {
        return level;
    }

    public String getDesc() {
        return desc;
    }

    public static SecurityLevel fromLevel(Integer level) {
        if (level == null) {
            return null;
        }
        for (SecurityLevel securityLevel : values()) {
            if (securityLevel.level == level) {
                return securityLevel;
            }
        }
        throw new IllegalArgumentException("Unknown security level: " + level);
    }
}
