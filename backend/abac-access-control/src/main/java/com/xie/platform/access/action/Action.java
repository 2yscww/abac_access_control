package com.xie.platform.access.action;

/**
 * ABAC 操作类型定义
 * 代表主体对资源发起的动作
 */
public enum Action {

    /** 读取资源（查看详情、列表） */
    READ,

    /** 写入资源（创建、修改内容） */
    WRITE,

    /** 删除资源 */
    DELETE,

    /** 推进项目阶段 */
    ADVANCE_PHASE,

    /** HR 发起员工离职 */
    OFFBOARD_EMPLOYEE,

    /** 管理层指定部门负责人 */
    ASSIGN_DEPARTMENT_MANAGER,

    /** 导出资源（未来扩展） */
    EXPORT
}
