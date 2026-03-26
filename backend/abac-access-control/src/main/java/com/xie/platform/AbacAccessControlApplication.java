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
		
		// TODO 项目中目前没有安全审计，没能闭环

		// TODO 项目中还没有接入 minio

		// TODO 管理员自定义策略参数还没有确认

		// TODO 现在项目内应该给部门表添加负责人

		// TODO 将项目成员组正式拉入业务流程中

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
