package com.techsophy.registration.notifications;

import org.codehaus.jettison.json.JSONObject;

public interface IMailService {

	public String notify(JSONObject obj);
	
	public String send(String[] to, String subject, String msg);
}
