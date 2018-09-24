package com.techsophy.registration.dao;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.techsophy.registration.entities.Users;

@Service
public class UserDAO {
	@Autowired
	UsersRepo userRepo;

	public Users get(String emailId) {

		Users user = new Users();
		user = userRepo.getByEmailId(emailId);
		return user;
	}

	public void save(Users users) {

		userRepo.save(users);
	}
	
	public void update(String status, String phone) {

		//userRepo.update(status);
	}

	public List<Users> getAll() {
		List<Users> users = new ArrayList<Users>();
		users = (List<Users>) userRepo.findAll();
		return users;
	}

}
