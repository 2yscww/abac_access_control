package com.xie.platform.mapper;

import com.xie.platform.dto.DepartmentManagerHandoverTodoDTO;
import com.xie.platform.model.Department;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DepartmentMapper {

    Department selectById(@Param("deptId") Long deptId);

    int updateManagerId(@Param("deptId") Long deptId, @Param("managerId") Long managerId);

    List<Department> selectByManagerId(@Param("managerId") Long managerId);

    List<DepartmentManagerHandoverTodoDTO> selectWithInactiveManager();
}
