package com.techsophy.registration.ui;

import java.net.URI;
import java.util.Base64;

import org.codehaus.jettison.json.JSONObject;

import com.vaadin.annotations.Theme;
import com.vaadin.navigator.Navigator;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.UI;

@SpringUI(path = "/verification")
@Theme("valo")
public class Verification extends UI {

	private static final long serialVersionUID = 1L;
	String contextURL = "";
	URI serverURL;

	@Override
	protected void init(VaadinRequest request) {
		serverURL = Page.getCurrent().getLocation();
		contextURL = "http://" + serverURL.getHost() + ":" + serverURL.getPort() + request.getContextPath();
		new Navigator(this, this);
		try {
			JSONObject queryObj = new JSONObject(request.getParameter("identifier"));
			String key = queryObj.get("key") != null ? queryObj.get("key").toString() : "";
			key = new String(Base64.getDecoder().decode(key));
			JSONObject keyValueObj = new JSONObject(key);
			VaadinSession.getCurrent().setAttribute("emailId", keyValueObj.get("emailId"));
			VaadinSession.getCurrent().setAttribute("firstName", keyValueObj.get("firstName"));
			VaadinSession.getCurrent().setAttribute("lastName", keyValueObj.get("lastName"));
			VaadinSession.getCurrent().setAttribute("password", keyValueObj.get("password"));
			VaadinSession.getCurrent().setAttribute("contextURL", contextURL);
			VaadinSession.getCurrent().setAttribute("token", keyValueObj.get("token"));
			getNavigator().addView(VerificationUI.NAME, VerificationUI.class);
			getNavigator().setErrorView(VerificationUI.class);
		} catch (Exception e) {
		}

	}

}