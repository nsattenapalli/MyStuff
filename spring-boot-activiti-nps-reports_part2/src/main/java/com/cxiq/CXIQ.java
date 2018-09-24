package com.cxiq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;

@SpringBootApplication
public class CXIQ extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(CXIQ.class);
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(CXIQ.class, args);
	}

	/*
	 * public static void main(String[] args) { SpringApplication.run(CXIQ.class,
	 * args); }
	 */

}
