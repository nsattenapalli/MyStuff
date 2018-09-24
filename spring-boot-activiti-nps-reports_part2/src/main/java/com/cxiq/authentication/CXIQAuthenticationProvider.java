package com.cxiq.authentication;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
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

import com.cxiq.service.activiti.ActivitiRestClientService;

@Component
public class CXIQAuthenticationProvider implements AuthenticationProvider {

	private static final Logger logger = Logger.getLogger(CXIQAuthenticationProvider.class);

	private static HashMap<String, ArrayList<String>> userRolesMap = new HashMap<String, ArrayList<String>>();

	@Qualifier("activiti")
	@Autowired
	DataSource activitiDatasource;

	@Autowired
	ActivitiRestClientService activitiClient;

	@Override
	public Authentication authenticate(Authentication authentication) throws AuthenticationException {

		ArrayList<String> roles = new ArrayList<>();
		List<GrantedAuthority> grantedAuths = new ArrayList<>();
		if (userRolesMap.containsKey(authentication.getName()))
			roles = userRolesMap.get(authentication.getName());
		else {
			roles = getRoles(authentication.getName(), authentication.getCredentials().toString());
		}
		if (roles != null && roles.size() > 0) {
			userRolesMap.put(authentication.getName(), roles);
			for (int i = 0; i < roles.size(); i++) {
				if ((roles.get(i).endsWith("admin") || roles.get(i).equalsIgnoreCase("admin"))
						&& !grantedAuths.contains(new SimpleGrantedAuthority("admin"))) {
					grantedAuths.add(new SimpleGrantedAuthority("admin"));
				} else if (roles.get(i).endsWith("manager")
						&& !grantedAuths.contains(new SimpleGrantedAuthority("manager"))) {
					grantedAuths.add(new SimpleGrantedAuthority("manager"));
				} else if (roles.get(i).startsWith("nps_tenant")) {
					if (!grantedAuths.contains(new SimpleGrantedAuthority("user")))
						grantedAuths.add(new SimpleGrantedAuthority("user"));
				}

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

	public ArrayList<String> getRoles(String username, String pwd) {
		Connection connection = null;
		Statement stmt = null;
		ArrayList<String> roles = new ArrayList<String>();
		try {
			connection = activitiDatasource.getConnection();
			stmt = connection.createStatement();

			String query1 = "SELECT m.group_id_ FROM act_id_user u, ACT_ID_MEMBERSHIP m where m.USER_ID_=u.id_ and u.id_='"
					+ username + "' and u.pwd_='" + pwd + "' ";
			ResultSet rs1 = stmt.executeQuery(query1);

			while (rs1.next()) {
				String role = rs1.getString(1);
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

	public boolean checkInActiviti(String id, String authUser, String authPassword) {
		if (logger.isDebugEnabled())
			logger.debug("Checking credentials in Activiti for  : username=" + authUser + ",passowrd=" + authPassword);
		boolean validUser = false;
		String result = activitiClient.invokeRestCall("identity/users/" + id, "", authUser, authPassword, "GET");
		JSONObject jsonObj = null;
		try {
			jsonObj = new JSONObject(result);

			if (jsonObj != null) {
				String userId = (String) jsonObj.get("id");
				if (userId.equalsIgnoreCase(id))
					validUser = true;
			}

		} catch (JSONException e) {
			logger.error("Error in getting user credentials from Activiti " + e);
		}

		return validUser;
	}

	public void removeUser(String userName) {
		if (userName != null && !"".equalsIgnoreCase(userName) && userRolesMap.get(userName) != null)
			userRolesMap.remove(userName);
	}

}
