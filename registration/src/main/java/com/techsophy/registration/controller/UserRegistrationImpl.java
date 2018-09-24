package com.techsophy.registration.controller;

import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.techsophy.registration.constants.Constants;
import com.techsophy.registration.dao.UserDAO;
import com.techsophy.registration.entities.Users;

@Component
public class UserRegistrationImpl implements IUserRegistration {

	@Autowired
	UserDAO userDAO;
	
	@Override
	public String saveTempUser(JSONObject obj) {
		try {
			Users users = new Users();
			users.setEmailId(obj.get("emailId").toString());
			users.setFirstName(obj.get("firstName").toString());
			users.setLastName(obj.get("lastName").toString());
			users.setPassword(obj.get("password").toString());
			users.setStatus(Constants.STATUS_VERIFICATION_PENDING);
			userDAO.save(users);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "success";
	}

	@Override
	public String updateTempUser(JSONObject obj, String status) {
		try {
			Users users = userDAO.get(obj.get("emailId").toString());
			users.setStatus(status);
			users.setPhone(obj.get("phone").toString());
			userDAO.save(users);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "success";
	}

	@Override
	public String deleteTempUser(String emailId) {
		return "success";
	}

	@Override
	public String createUser(JSONObject obj) {
		return "success";
	}

	@Override
	public boolean checkIfUserAlreadyExists(String emailId, String status) {
		boolean exist = false;
		Users users = new Users();
		users = userDAO.get(emailId);
		if(users!=null && users.getEmailId()!=null && (users.getStatus().equalsIgnoreCase(status) || users.getStatus().equalsIgnoreCase(Constants.STATUS_APPROVED))) {
			exist = true;
		}
		return exist;
	}

}
