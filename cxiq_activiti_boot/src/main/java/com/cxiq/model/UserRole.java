package com.cxiq.model;

public class UserRole {
	String name;
	String password;
	String role;

	public UserRole(String name, String password, String role) {
		this.name = name;
		this.password = password;
		this.role = role;
	}

	boolean index(String name, String password) {
		return this.name.equals(name) && this.password.equals(password);
	}
}
