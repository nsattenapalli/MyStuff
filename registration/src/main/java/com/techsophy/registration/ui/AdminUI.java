package com.techsophy.registration.ui;

import java.net.URI;
import java.util.Base64;

import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

import com.techsophy.registration.dao.UserDAO;
import com.techsophy.registration.entities.Users;
import com.techsophy.registration.rest.CommonRegistrationServices;
import com.vaadin.annotations.Theme;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.Position;
import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

@SpringUI(path = "/admin")
@Theme("valo")
public class AdminUI extends UI {

	private static final long serialVersionUID = 1L;
	Grid<Users> grid = new Grid<>();
	@Autowired
	UserDAO userDAO;
	String contextURL = "";
	URI serverURL;
	@Autowired
	CommonRegistrationServices services;
	
	@Override
	protected void init(VaadinRequest request) {
		serverURL = Page.getCurrent().getLocation();
		contextURL = "http://" + serverURL.getHost() + ":" + serverURL.getPort() + request.getContextPath();
		Panel panel = new Panel("<center><font size=\"5\" color=\"#197de1\"><b>Trail Accounts</b></font></center>");

		grid.setItems(userDAO.getAll());
		grid.addColumn(Users::getEmailId).setCaption("Email ID").setExpandRatio(1);
		grid.addColumn(Users::getFirstName).setCaption("First name").setExpandRatio(1);
		grid.addColumn(Users::getLastName).setCaption("Last name").setExpandRatio(1);
		grid.addColumn(Users::getPhone).setCaption("Phone").setExpandRatio(1);
		grid.addColumn(Users::getStatus).setCaption("Current Status").setExpandRatio(1);
		grid.addComponentColumn(this::buildApproveButton);
		grid.addComponentColumn(this::buildRejectButton);

		panel.setSizeFull();
		grid.setSizeFull();
		panel.setContent(grid);
		setContent(panel);

	}

	private Button buildApproveButton(Users u) {
		Button button = new Button("Approve");
		button.setEnabled(false);
		if (u.getStatus().equalsIgnoreCase("Approval Pending")) {
			button.setEnabled(true);
		}
		button.setCaption("Approve");
		button.addStyleName(ValoTheme.BUTTON_SMALL);
		button.addClickListener(e -> approveUser(u));
		return button;
	}

	private void approveUser(Users u) {
		invokeServiceCall(u.getEmailId(), u.getFirstName(), u.getLastName(), u.getPhone(), "approved");
		grid.setItems(userDAO.getAll());
	}

	private Button buildRejectButton(Users u) {
		Button button = new Button("Reject");
		button.setEnabled(false);
		if (u.getStatus().equalsIgnoreCase("Approval Pending")) {
			button.setEnabled(true);
		}
		button.setCaption("Reject");
		button.addStyleName(ValoTheme.BUTTON_SMALL);
		button.addClickListener(e -> rejectUser(u));
		return button;
	}

	private void rejectUser(Users u) {
		invokeServiceCall(u.getEmailId(), u.getFirstName(), u.getLastName(), u.getPhone(), "rejected");
		grid.setItems(userDAO.getAll());
	}

	public void invokeServiceCall(String email, String fName, String lName, String ph, String status) {
		Base64.Encoder encoder = Base64.getEncoder();
		String serviceURL = "";
		try {
			JSONObject obj = new JSONObject();
			obj.put("emailId", email);
			obj.put("firstName", fName);
			obj.put("lastName", lName);
			obj.put("phone", ph);
			obj.put("contextURL", contextURL);
			obj.put("token", System.currentTimeMillis());
			String keyVal = new String(encoder.encode(obj.toString().getBytes()));
			obj = new JSONObject();
			obj.put("key", keyVal);
			obj.put("status", status);
			
			serviceURL = contextURL + "/rest/user/{input}";
			
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.postForObject(serviceURL, "", String.class, obj);
			
			//services.registerUser(obj.toString());
			
			if (status.toLowerCase().contains("approved")) {
				Notification n = new Notification("<b>User Account Approved</b>", Notification.Type.HUMANIZED_MESSAGE);
				n.setDelayMsec(2000);
				n.setHtmlContentAllowed(true);
				n.setPosition(Position.BOTTOM_LEFT);
				n.show(Page.getCurrent());
			} else {
				Notification n = new Notification("<b>User Account Rejected</b>", Notification.Type.WARNING_MESSAGE);
				n.setDelayMsec(2000);
				n.setHtmlContentAllowed(true);
				n.setPosition(Position.BOTTOM_LEFT);
				n.show(Page.getCurrent());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}