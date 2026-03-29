package com.xie.platform.service;

import com.xie.platform.dto.CreateEmployeeDTO;
import com.xie.platform.dto.EmployeeActiveQueryDTO;
import com.xie.platform.dto.EmployeeOnboardOptionsDTO;
import com.xie.platform.dto.EmployeeOnboardResultDTO;
import com.xie.platform.dto.EmployeeOptionDTO;
import com.xie.platform.dto.EmployeeProfileDTO;
import com.xie.platform.dto.OffboardEmployeeDTO;
import com.xie.platform.service.result.LoginResult;

import java.util.List;

public interface EmployeeAuthService {

    LoginResult login(String employeeCode, String rawPassword);

    String changePassword(String tempToken, String oldPassword, String newPassword);

    EmployeeOnboardResultDTO createEmployee(CreateEmployeeDTO dto, Long operatorEmployeeId);

    EmployeeOnboardOptionsDTO getEmployeeOnboardOptions(Long operatorEmployeeId);

    void offboardEmployee(OffboardEmployeeDTO dto, Long operatorEmployeeId);

    List<EmployeeOptionDTO> queryActiveEmployees(EmployeeActiveQueryDTO query, Long operatorEmployeeId);

    EmployeeProfileDTO getCurrentEmployeeProfile(Long employeeId);
}
