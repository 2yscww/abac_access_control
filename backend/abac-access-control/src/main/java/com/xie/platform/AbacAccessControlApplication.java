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
