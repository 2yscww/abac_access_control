package com.xie.platform.mapper;

import com.xie.platform.dto.EmployeeOptionDTO;
import com.xie.platform.model.Employees;
import com.xie.platform.model.enumValue.EmployeeStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EmployeesMapper {

    Employees selectByEmployeeName(@Param("employeeName") String employeeName);

    Employees selectByEmployeeCode(@Param("employeeCode") String employeeCode);

    Employees selectByEmployeeId(@Param("employeeId") Long employeeId);

    int updatePassword(
            @Param("employeeId") Long employeeId,
            @Param("password") String password
    );

    int insert(Employees employee);

    int updateEmployeeCode(@Param("employeeId") Long employeeId, @Param("employeeCode") String employeeCode);

    int updateStatus(@Param("employeeId") Long employeeId, @Param("status") EmployeeStatus status);

    List<EmployeeOptionDTO> selectActiveOptions(@Param("keyword") String keyword);

    List<EmployeeOptionDTO> selectActiveOptionsByDeptId(@Param("deptId") Long deptId);
}
