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

	// TODO 用户的初始密码是明文存储，但是后端会把密码加密，可能因为如此导致的不匹配，需要继续修改

}
