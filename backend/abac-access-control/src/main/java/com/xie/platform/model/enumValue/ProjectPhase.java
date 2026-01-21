package com.xie.platform.model.enumValue;

public enum ProjectPhase {
    INIT(1, "立项"),
    REQUIREMENT(2, "需求设计"),
    DEVELOPMENT(3, "研发实现"),
    TEST(4, "测试验证"),
    RELEASE(5, "上线交付"),
    ARCHIVED(6, "归档");

    private final int code;
    private final String desc;

    ProjectPhase(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ProjectPhase fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ProjectPhase phase : values()) {
            if (phase.code == code) {
                return phase;
            }
        }
        throw new IllegalArgumentException("Unknown project phase code: " + code);
    }
}
