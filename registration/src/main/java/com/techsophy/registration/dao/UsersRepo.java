package com.techsophy.registration.dao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.techsophy.registration.entities.Users;
@Repository
public interface UsersRepo extends JpaRepository<Users,Long>{
	
	public Users getByEmailId(String emailId);
	
}
