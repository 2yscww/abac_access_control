package com.xie.platform.controller;

import com.xie.platform.common.Response;
import com.xie.platform.dto.ChangePasswdDTO;
import com.xie.platform.dto.CreateEmployeeDTO;
import com.xie.platform.dto.EmployeeLoginDTO;
import com.xie.platform.service.EmployeeAuthService;
import com.xie.platform.service.result.LoginResult;
import com.xie.platform.utils.CurrentUserContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/employee")
public class EmployeeAuthController {

        @Autowired
        private EmployeeAuthService employeeAuthService;

        @PostMapping("/login")
        public Response<Object> login(@RequestBody EmployeeLoginDTO dto) {

                LoginResult result = employeeAuthService.login(
                                dto.getEmployeeCode(),
                                dto.getPassword());

                if (!result.isSuccess()) {
                        return Response.Fail(null, result.getMessage());
                }

                // 首次登录，强制修改密码
                if (result.isMustChangePassword()) {
                        return Response.Success(
                                        Map.of("tempToken", result.getTempToken(),
                                                "employeeId", result.getEmployeeId(),
                                                "mustChangePassword", true),
                                        "首次登录，请修改密码");
                }

                // 正常登录成功
                return Response.Success(
                                Map.of("employeeId", result.getEmployeeId(),
                                                "token", result.getToken()),
                                "登录成功");
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
                                        dto.getNewPassword());
                        return Response.Success(Map.of("token", token), "密码修改成功");
                } catch (RuntimeException e) {
                        String errorMsg = e.getMessage() != null ? e.getMessage() : "修改密码失败";
                        return Response.Fail(null, errorMsg);
                } catch (Exception e) {
                        return Response.Fail(null, "系统异常：" + e.getMessage());
                }
        }

        // 创建员工
        @PostMapping("/create")
        public Response<Void> createEmployee(@RequestBody CreateEmployeeDTO dto) {
                Long operatorEmployeeId = CurrentUserContext.getRequiredEmployeeId();
                employeeAuthService.createEmployee(dto, operatorEmployeeId);
                return Response.Success(null, null);
        }

}
