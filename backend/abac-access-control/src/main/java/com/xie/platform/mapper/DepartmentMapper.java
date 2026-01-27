package com.xie.platform.mapper;

import com.xie.platform.model.Department;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DepartmentMapper {

    /**
     * 根据部门ID查询部门
     */
    Department selectById(@Param("deptId") Long deptId);
}
