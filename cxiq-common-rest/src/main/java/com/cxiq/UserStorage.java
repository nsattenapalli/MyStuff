package com.cxiq;

import java.util.ArrayList;
import java.util.HashMap;

public final class UserStorage {
	public static HashMap<String, ArrayList<String>> userRolesMap = new HashMap<String, ArrayList<String>>();

	public static void addUser(String user, ArrayList<String> roles) {
		userRolesMap.put(user, roles);
	}

	public static ArrayList<String> getUserRoles(String user) {
		return userRolesMap.get(user);
	}

	public static void removeUser(String user) {
		userRolesMap.remove(user);
	}
}
