package com.stie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class StieApplication {

	public static void main(String[] args) {
		SpringApplication.run(StieApplication.class, args);
	}

}

