package com.xie.platform.mapper;

import com.xie.platform.model.Employees;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EmployeesMapper {

    /**
     * 根据员工登录名查询员工
     */
    Employees selectByEmployeeName(@Param("employeeName") String employeeName);

    /**
     * 根据员工 ID 查询员工（后面 ABAC 会用）
     */
    Employees selectByEmployeeId(@Param("employeeId") Long employeeId);

    /**
     * 更新密码 + 清除首次登录标记
     */
    int updatePassword(
            @Param("employeeId") Long employeeId,
            @Param("password") String password
    );
}
