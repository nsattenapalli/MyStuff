package com.techsophy.registration.ui;

import java.util.Base64;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.web.client.RestTemplate;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.Position;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

public class RegistrationUI extends VerticalLayout implements View {
	private static final long serialVersionUID = 1L;
	public static final String NAME = "";

	Validator VAL = new Validator();

	public RegistrationUI() {
		Panel panel = new Panel("<center><b>Registration</b></center>");
		panel.setSizeUndefined();
		addComponent(panel);

		FormLayout content = new FormLayout();
		TextField emailId = new TextField("Email ID");
		content.addComponent(emailId);
		TextField firstName = new TextField("First Name");
		content.addComponent(firstName);
		TextField lastName = new TextField("Last Name");
		content.addComponent(lastName);
		PasswordField password = new PasswordField("Password");
		content.addComponent(password);

		Label message = new Label();
		message.setValue("<font color=\"#197de1\"><b>Verification Mail Sent</b></font>");
		message.setContentMode(ContentMode.HTML);
		message.setVisible(false);

		Button send = new Button("Sign Up");
		send.addClickListener(new ClickListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(ClickEvent event) {
				if (VAL.validate(emailId.getValue(), firstName.getValue(), lastName.getValue(), password.getValue())) {
					VaadinSession.getCurrent().setAttribute("emailId", emailId.getValue());
					VaadinSession.getCurrent().setAttribute("firstName", firstName.getValue());
					VaadinSession.getCurrent().setAttribute("lastName", lastName.getValue());
					VaadinSession.getCurrent().setAttribute("password", password.getValue());
					String response = invokeServiceCall(emailId.getValue(), firstName.getValue(), lastName.getValue(),
							password.getValue());
					if (response.contains("success")) {
						message.setVisible(true);
						emailId.setVisible(false);
						firstName.setVisible(false);
						lastName.setVisible(false);
						password.setVisible(false);
						send.setVisible(false);
					} else if (response.contains("already exist")) {
						Notification n = new Notification("<b>User Already Exist!</b>",
								Notification.Type.ERROR_MESSAGE);
						n.setDelayMsec(2000);
						n.setHtmlContentAllowed(true);
						n.setPosition(Position.BOTTOM_LEFT);
						n.show(Page.getCurrent());
					} else {
						Notification n = new Notification("<b>Problem in sending Verification Mail</b>",
								Notification.Type.ERROR_MESSAGE);
						n.setDelayMsec(2000);
						n.setHtmlContentAllowed(true);
						n.setPosition(Position.BOTTOM_LEFT);
						n.show(Page.getCurrent());
					}
				} else {
					Notification n = new Notification("<b>Please fill all details</b>",
							Notification.Type.ERROR_MESSAGE);
					n.setHtmlContentAllowed(true);
					n.show(Page.getCurrent());
				}
			}

		});
		content.addComponent(send);
		content.addComponent(message);
		content.setSizeUndefined();
		content.setMargin(true);
		panel.setContent(content);
		setComponentAlignment(panel, Alignment.MIDDLE_CENTER);

	}

	@Override
	public void enter(ViewChangeEvent event) {

		Notification n = new Notification("Welcome to trail account registration!",
				Notification.Type.HUMANIZED_MESSAGE);
		n.setDelayMsec(1000);
		n.setPosition(Position.TOP_CENTER);
		n.show(Page.getCurrent());

	}

	public String invokeServiceCall(String email, String fName, String lName, String pwd) {
		Base64.Encoder encoder = Base64.getEncoder();
		String serviceResponse = "";
		String contextURL = "";
		String serviceURL = "";
		try {
			contextURL = VaadinSession.getCurrent().getAttribute("contextURL").toString();
			JSONObject obj = new JSONObject();
			obj.put("emailId", email);
			obj.put("firstName", fName);
			obj.put("lastName", lName);
			obj.put("password", pwd);
			obj.put("contextURL", contextURL + "/verification");
			String keyVal = new String(encoder.encode(obj.toString().getBytes()));
			obj = new JSONObject();
			obj.put("key", keyVal);
			serviceURL = contextURL + "/rest/user/verification/{input}";

			RestTemplate restTemplate = new RestTemplate();
			serviceResponse = restTemplate.postForObject(serviceURL, "", String.class, obj);

		} catch (JSONException e) {
			e.printStackTrace();
		}
		return serviceResponse;
	}

}
