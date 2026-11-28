package com.drogia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DrogiaApplication {

	public static void main(String[] args) {
		SpringApplication.run(DrogiaApplication.class, args);
	}

}
