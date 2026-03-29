package com.xie.platform.mapper;

import com.xie.platform.model.Branch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BranchMapper {

    /**
     * 根据分公司ID查询分公司
     */
    Branch selectById(@Param("branchId") Long branchId);

    List<Branch> selectAll();
}
