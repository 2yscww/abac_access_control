package com.xie.platform.model.enumValue;


public enum AssetType {
    REQUIREMENT_DOC(1, "需求文档"),
    DESIGN_DOC(2, "设计文档"),
    SOURCE_CODE(3, "源代码"),
    TEST_REPORT(4, "测试报告"),
    DEPLOY_SCRIPT(5, "部署脚本"),
    OPS_DOC(6, "运维文档");

    private final int code;
    private final String desc;

    AssetType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static AssetType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (AssetType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown asset type code: " + code);
    }
}
