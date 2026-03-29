package com.xie.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@MapperScan("com.xie.platform.mapper")
public class AbacAccessControlApplication {

    public static void main(String[] args) {
        SpringApplication.run(AbacAccessControlApplication.class, args);


        // TODO 项目启动前，需要有一个临时接口去让DBA添加部分员工，让这些行政/管理/HR去把系统信息跑起来
		




		// TODO 管理员自定义策略参数还没有确认

		// TODO 员工没法提出离职

		// TODO 项目中上传资产时可以选择非本阶段

		// TODO 下载和引用文件的方式不统一
		





		// 创建加密器
		PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

		// 原始密码
		String rawPassword = "ABACtest";

		// 加密
		String encodedPassword = passwordEncoder.encode(rawPassword);

		// 输出
		System.out.println("原始密码: " + rawPassword);
		System.out.println("加密后: " + encodedPassword);
    }
}
