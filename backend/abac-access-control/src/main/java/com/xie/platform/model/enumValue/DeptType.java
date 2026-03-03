package com.xie.platform.model.enumValue;

/**
 * 部门类型枚举
 * 数据库 dept_type 字段存储枚举的 name()（即字符串），业务层使用枚举比较。
 */
public enum DeptType {

    PRODUCT("产品部"),
    RD("研发部"),
    QA("测试部"),
    OPS("运维部"),
    MANAGEMENT("管理层");

    private final String desc;

    DeptType(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    public static DeptType fromName(String name) {
        for (DeptType type : values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown dept type: " + name);
    }
}
