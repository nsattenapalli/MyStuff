package com.cxiq;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@Order(1)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	private CustomAuthenticationProvider customAuthenticationProvider;

	@Autowired
	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.authenticationProvider(customAuthenticationProvider);
		/*auth.inMemoryAuthentication().withUser("user1").password("secret1")
		.roles("USER").and().withUser("admin1").password("secret1")
		.roles("USER", "ADMIN");*/
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.httpBasic().and().authorizeRequests().antMatchers("/test/**").hasAuthority("user").antMatchers("/**")
				.hasAuthority("admin").and().csrf().disable();
		
		/*http.httpBasic().and().authorizeRequests().antMatchers("/test/**")
		.hasRole("USER").antMatchers("/**").hasRole("ADMIN").and()
		.csrf().disable().headers().frameOptions().disable();*/
	}

}
