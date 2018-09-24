package com.techsophy.registration.ui;

import java.net.URI;

import com.vaadin.annotations.Theme;
import com.vaadin.navigator.Navigator;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.UI;

@SpringUI(path = "/app")
@Theme("valo")
public class Registration extends UI {

	private static final long serialVersionUID = 1L;
	String contextURL = "";
	URI serverURL;

	@Override
	protected void init(VaadinRequest request) {
		new Navigator(this, this);
		serverURL = Page.getCurrent().getLocation();
		contextURL = "http://" + serverURL.getHost() + ":" + serverURL.getPort() + request.getContextPath();
		VaadinSession.getCurrent().setAttribute("contextURL", contextURL);
		getNavigator().addView(RegistrationUI.NAME, RegistrationUI.class);
		getNavigator().setErrorView(RegistrationUI.class);

		router("");
	}

	public void router(String route) {
		getNavigator().navigateTo(RegistrationUI.NAME);
	}

}