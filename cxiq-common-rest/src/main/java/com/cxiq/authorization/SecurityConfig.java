package com.cxiq.authorization;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

import com.cxiq.authentication.CXIQAuthenticationProvider;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Order(1)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	private CXIQAuthenticationProvider customAuthenticationProvider;

	@Autowired
	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.authenticationProvider(customAuthenticationProvider);
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and().authorizeRequests()
				.antMatchers("/service/**", "/registration/**").hasAnyAuthority("user", "manager", "admin")
				.antMatchers(HttpMethod.GET, "/identity/**", "/runtime/**", "/query/**").hasAuthority("user")
				.antMatchers(HttpMethod.POST, "/runtime/tasks/**", "/query/tasks/**",
						"/query/historic-task-instances/**")
				.hasAuthority("user")
				.antMatchers(HttpMethod.PUT, "/runtime/tasks/**", "/query/tasks/**",
						"/query/historic-task-instances/**")
				.hasAuthority("user")
				.antMatchers(HttpMethod.DELETE, "/runtime/tasks/**", "/query/tasks/**",
						"/query/historic-task-instances/**")
				.hasAuthority("user").antMatchers("/identity/**", "/runtime/**", "/query/**")
				.hasAnyAuthority("manager", "admin").and().httpBasic().and().csrf().disable();
	}

}
