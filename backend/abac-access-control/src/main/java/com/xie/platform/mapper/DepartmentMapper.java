package com.xie.platform.mapper;

import com.xie.platform.dto.DepartmentManagerHandoverTodoDTO;
import com.xie.platform.model.Department;
import com.xie.platform.model.enumValue.DeptType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DepartmentMapper {

    Department selectById(@Param("deptId") Long deptId);

    List<Department> selectAll();

    int updateManagerId(@Param("deptId") Long deptId, @Param("managerId") Long managerId);

    List<Department> selectByManagerId(@Param("managerId") Long managerId);

    Department selectByDeptType(@Param("deptType") DeptType deptType);

    List<DepartmentManagerHandoverTodoDTO> selectWithInactiveManager();
}
