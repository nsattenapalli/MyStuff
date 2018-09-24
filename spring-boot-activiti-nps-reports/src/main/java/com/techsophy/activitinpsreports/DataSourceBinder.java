package com.techsophy.activitinpsreports;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
public class DataSourceBinder {
	@Bean(name = "nps")
	@ConfigurationProperties("datasource.nps")
	@Primary
	public DataSource npsDataSource() {
		return DataSourceBuilder.create().build();
	}

	@Bean(name = "activiti")
	@ConfigurationProperties("datasource.activiti")
	public DataSource activitiDataSource() {
		return DataSourceBuilder.create().build();
	}
}
