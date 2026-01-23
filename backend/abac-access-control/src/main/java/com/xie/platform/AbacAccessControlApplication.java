package com.xie.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.xie.platform.mapper")
// 启动类
@SpringBootApplication
public class AbacAccessControlApplication {

	public static void main(String[] args) {
		SpringApplication.run(AbacAccessControlApplication.class, args);
	}

}
