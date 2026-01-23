package com.xie.platform.controller;

import com.xie.platform.common.Response;
import com.xie.platform.dto.EmployeeLoginDTO;
import com.xie.platform.service.EmployeeAuthService;
import com.xie.platform.service.result.LoginResult;

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
                dto.getEmployeeName(),
                dto.getPassword()
        );

        if (!result.isSuccess()) {
            return Response.Fail(null, result.getMessage());
        }

        // 首次登录，强制修改密码
        if (result.isMustChangePassword()) {
            return Response.Success(
                    Map.of(
                            "employeeId", result.getEmployeeId(),
                            "mustChangePassword", true
                    ),
                    "首次登录，请修改密码"
            );
        }

        // 正常登录成功
        return Response.Success(
                Map.of(
                        "employeeId", result.getEmployeeId()
                ),
                "登录成功"
        );
    }
}
