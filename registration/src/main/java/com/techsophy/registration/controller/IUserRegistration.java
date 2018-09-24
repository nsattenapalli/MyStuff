package com.techsophy.registration.controller;

import org.codehaus.jettison.json.JSONObject;


public interface IUserRegistration {

	public boolean checkIfUserAlreadyExists(String emailId, String status);
	
	// Saves the user data into stage table
	public String saveTempUser(JSONObject obj);

	// Updates the stage table with the user and its status
	public String updateTempUser(JSONObject obj, String status);

	// Deletes the user from stage table once the user is created in master
	// table
	public String deleteTempUser(String emailId);

	public String createUser(JSONObject obj);

}
