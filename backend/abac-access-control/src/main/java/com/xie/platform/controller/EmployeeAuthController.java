package com.xie.platform.controller;

import com.xie.platform.common.Response;
import com.xie.platform.dto.ChangePasswdDTO;
import com.xie.platform.dto.CreateEmployeeDTO;
import com.xie.platform.dto.EmployeeActiveQueryDTO;
import com.xie.platform.dto.EmployeeOnboardOptionsDTO;
import com.xie.platform.dto.EmployeeOnboardResultDTO;
import com.xie.platform.dto.EmployeeLoginDTO;
import com.xie.platform.dto.EmployeeOptionDTO;
import com.xie.platform.dto.EmployeeProfileDTO;
import com.xie.platform.dto.OffboardEmployeeDTO;
import com.xie.platform.service.EmployeeAuthService;
import com.xie.platform.service.result.LoginResult;
import com.xie.platform.utils.CurrentUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employee")
public class EmployeeAuthController {

    @Autowired
    private EmployeeAuthService employeeAuthService;

    @PostMapping("/login")
    public Response<Object> login(@RequestBody EmployeeLoginDTO dto) {
        LoginResult result = employeeAuthService.login(dto.getEmployeeCode(), dto.getPassword());
        if (!result.isSuccess()) {
            return Response.Fail(null, result.getMessage());
        }

        if (result.isMustChangePassword()) {
            return Response.Success(
                    Map.of(
                            "tempToken", result.getTempToken(),
                            "employeeId", result.getEmployeeId(),
                            "mustChangePassword", true
                    ),
                    "首次登录，请修改密码"
            );
        }

        return Response.Success(
                Map.of(
                        "employeeId", result.getEmployeeId(),
                        "token", result.getToken()
                ),
                "登录成功"
        );
    }

    @PostMapping("/change-password")
    public Response<Object> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ChangePasswdDTO dto) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Response.Fail(null, "临时凭证不能为空");
            }
            String tempToken = authHeader.substring(7);

            if (dto.getOldPassword() == null || dto.getOldPassword().isEmpty()) {
                return Response.Fail(null, "原密码不能为空");
            }
            if (dto.getNewPassword() == null || dto.getNewPassword().isEmpty()) {
                return Response.Fail(null, "新密码不能为空");
            }

            String token = employeeAuthService.changePassword(
                    tempToken,
                    dto.getOldPassword(),
                    dto.getNewPassword()
            );
            return Response.Success(Map.of("token", token), "密码修改成功");
        } catch (RuntimeException exception) {
            String errorMsg = exception.getMessage() != null ? exception.getMessage() : "修改密码失败";
            return Response.Fail(null, errorMsg);
        } catch (Exception exception) {
            return Response.Fail(null, "系统异常: " + exception.getMessage());
        }
    }

    @PostMapping("/create")
    public Response<EmployeeOnboardResultDTO> createEmployee(@RequestBody CreateEmployeeDTO dto) {
        Long operatorEmployeeId = CurrentUserContext.getRequiredEmployeeId();
        EmployeeOnboardResultDTO result = employeeAuthService.createEmployee(dto, operatorEmployeeId);
        return Response.Success(result, "员工入职创建成功");
    }

    @GetMapping("/active-list")
    public Response<List<EmployeeOptionDTO>> queryActiveEmployees(EmployeeActiveQueryDTO query) {
        Long operatorEmployeeId = CurrentUserContext.getRequiredEmployeeId();
        List<EmployeeOptionDTO> result = employeeAuthService.queryActiveEmployees(query, operatorEmployeeId);
        return Response.Success(result, null);
    }

    @GetMapping("/onboard-options")
    public Response<EmployeeOnboardOptionsDTO> getEmployeeOnboardOptions() {
        Long operatorEmployeeId = CurrentUserContext.getRequiredEmployeeId();
        EmployeeOnboardOptionsDTO result = employeeAuthService.getEmployeeOnboardOptions(operatorEmployeeId);
        return Response.Success(result, null);
    }

    @GetMapping("/me")
    public Response<EmployeeProfileDTO> getCurrentEmployeeProfile() {
        Long employeeId = CurrentUserContext.getRequiredEmployeeId();
        EmployeeProfileDTO profile = employeeAuthService.getCurrentEmployeeProfile(employeeId);
        return Response.Success(profile, null);
    }

    @PutMapping("/offboard")
    public Response<Void> offboardEmployee(@RequestBody OffboardEmployeeDTO dto) {
        Long operatorEmployeeId = CurrentUserContext.getRequiredEmployeeId();
        employeeAuthService.offboardEmployee(dto, operatorEmployeeId);
        return Response.Success(null, "员工离职办理成功");
    }
}
