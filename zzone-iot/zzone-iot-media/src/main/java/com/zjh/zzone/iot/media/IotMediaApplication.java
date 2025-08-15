package com.zjh.zzone.iot.media;

import com.ylg.security.annotation.EnableYlgFeignClients;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@ServletComponentScan("com.ylg")
@SpringBootApplication(scanBasePackages = "com.ylg")
@EnableYlgFeignClients
@EnableScheduling
@EnableCaching
@Slf4j
public class IotMediaApplication {

	public static void main(String[] args) {
		SpringApplication.run(IotMediaApplication.class, args);
	}

}
