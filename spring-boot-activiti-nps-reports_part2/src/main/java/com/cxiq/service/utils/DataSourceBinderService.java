package com.cxiq.service.utils;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
public class DataSourceBinderService {
	@Bean(name = "cxiq")
	@ConfigurationProperties("datasource.cxiq")
	public DataSource cxiqDataSource() {
		return DataSourceBuilder.create().build();
	}

	@Bean(name = "activiti")
	@ConfigurationProperties("datasource.activiti")
	@Primary
	public DataSource activitiDataSource() {
		return DataSourceBuilder.create().build();
	}
}
