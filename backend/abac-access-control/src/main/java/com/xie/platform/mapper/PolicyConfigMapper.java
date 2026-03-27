package com.xie.platform.mapper;

import com.xie.platform.model.PolicyConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PolicyConfigMapper {

    PolicyConfig selectByPolicyName(@Param("policyName") String policyName);

    List<PolicyConfig> selectByPolicyNames(@Param("policyNames") List<String> policyNames);

    int insert(PolicyConfig policyConfig);

    int update(PolicyConfig policyConfig);
}
