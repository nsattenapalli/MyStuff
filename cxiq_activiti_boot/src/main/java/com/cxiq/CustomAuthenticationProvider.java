package com.cxiq;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

	private static final Logger logger = Logger.getLogger(CustomAuthenticationProvider.class);

	private static HashMap<String, ArrayList<String>> userRolesMap = new HashMap<String, ArrayList<String>>();
	
	@Qualifier("activiti")
	@Autowired
	DataSource activitiDatasource;

	public CustomAuthenticationProvider() {
		logger.info("CustomAuthenticationProvider created");
	}

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {

		ArrayList<String> roles = new ArrayList<>();
		List<GrantedAuthority> grantedAuths = new ArrayList<>();
		if (userRolesMap.containsKey(authentication.getName()))
			roles = userRolesMap.get(authentication.getName());
		else {
			roles = getRoles(authentication.getName());
			userRolesMap.put(authentication.getName(), roles);
		}
		if (roles != null && roles.size() > 0) {
			for (int i = 0; i < roles.size(); i++) {
				grantedAuths.add(new SimpleGrantedAuthority(roles.get(i)));
			}
			return new UsernamePasswordAuthenticationToken(authentication.getName(), authentication.getCredentials(),
					grantedAuths);
		} else {
			logger.error("Authentication failed for user = " + authentication.getName());
			throw new BadCredentialsException("Authentication failed for user " + authentication.getName());
		}

	}

	@Override
	public boolean supports(Class<?> authentication) {
		return authentication.equals(UsernamePasswordAuthenticationToken.class);
	}

	public ArrayList<String> getRoles(String username) {
		Connection connection = null;
		Statement stmt = null;
		ArrayList<String> roles = new ArrayList<String>();
		try {
			connection = activitiDatasource.getConnection();
			stmt = connection.createStatement();

			String query1 = "SELECT * FROM ACT_ID_MEMBERSHIP where USER_ID_='" + username + "' ";
			ResultSet rs1 = stmt.executeQuery(query1);

			while (rs1.next()) {
				String role = rs1.getString("GROUP_ID_");
				roles.add(role);

			}
		} catch (Exception e) {
			logger.error("In CXIQAuthorization-doFilter method catch block due to " + e);
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					logger.error(e);
				}
			}
		}
		return roles;
	}

}
