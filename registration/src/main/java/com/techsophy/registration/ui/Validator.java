package com.techsophy.registration.ui;

public class Validator {
	
	private String emailId;
	private String firstName;
	private String lastName;
	private String password;
	private String phone;
	
	public Validator() {
		setEmailId("nagaraju.s@techsophy.com");
		setFirstName("Nagaraju");
		setLastName("s");
	}

	
	public String getEmailId() {
		return emailId;
	}


	public void setEmailId(String emailId) {
		this.emailId = emailId;
	}


	public String getFirstName() {
		return firstName;
	}


	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}


	public String getLastName() {
		return lastName;
	}


	public void setLastName(String lastName) {
		this.lastName = lastName;
	}


	public String getPassword() {
		return password;
	}


	public void setPassword(String password) {
		this.password = password;
	}


	public String getPhone() {
		return phone;
	}


	public void setPhone(String phone) {
		this.phone = phone;
	}


	public Boolean validate(String emailId, String firstName, String lastName, String password){
		if(!emailId.isEmpty() && !firstName.isEmpty() && !lastName.isEmpty() && !password.isEmpty()){
			return true;
		}
		return false;
	}

}
