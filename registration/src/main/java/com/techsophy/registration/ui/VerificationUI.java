package com.techsophy.registration.ui;

import java.util.Base64;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.web.client.RestTemplate;

import com.techsophy.registration.util.Utils;
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

public class VerificationUI extends VerticalLayout implements View {
	private static final long serialVersionUID = 1L;
	public static final String NAME = "";

	Validator VAL = new Validator();

	public VerificationUI() {
		Panel panel = new Panel("<center><b>Verification</b></center>");
		panel.setSizeUndefined();
		addComponent(panel);

		FormLayout content = new FormLayout();

		Label message = new Label();

		Button send = new Button("Register");

		if (System.currentTimeMillis() < (Long.parseLong(VaadinSession.getCurrent().getAttribute("token").toString())
				+ (Integer.parseInt(Utils.getPropertyValue("verification_expiry_minutes").toString()) * 60 * 1000))) {

			TextField emailId = new TextField("Email ID");
			emailId.setValue(VaadinSession.getCurrent().getAttribute("emailId").toString());
			emailId.setReadOnly(true);
			content.addComponent(emailId);
			TextField firstName = new TextField("First Name");
			firstName.setValue(VaadinSession.getCurrent().getAttribute("firstName").toString());
			firstName.setReadOnly(true);
			content.addComponent(firstName);
			TextField lastName = new TextField("Last Name");
			lastName.setValue(VaadinSession.getCurrent().getAttribute("lastName").toString());
			lastName.setReadOnly(true);
			content.addComponent(lastName);
			PasswordField password = new PasswordField("Password");
			password.setValue(VaadinSession.getCurrent().getAttribute("password").toString());
			password.setReadOnly(true);
			content.addComponent(password);
			TextField phone = new TextField("Phone");
			content.addComponent(phone);

			message.setValue(
					"<font color=\"#197de1\"><b>Your registration request is pending with approval</b></font>");
			message.setContentMode(ContentMode.HTML);
			message.setVisible(false);

			send.addClickListener(new ClickListener() {
				private static final long serialVersionUID = 1L;

				@Override
				public void buttonClick(ClickEvent event) {
					if (VAL.validate(emailId.getValue(), firstName.getValue(), lastName.getValue(), password.getValue())
							&& !phone.getValue().isEmpty()) {
						VaadinSession.getCurrent().setAttribute("emailId", emailId.getValue());
						VaadinSession.getCurrent().setAttribute("firstName", firstName.getValue());
						VaadinSession.getCurrent().setAttribute("lastName", lastName.getValue());
						VaadinSession.getCurrent().setAttribute("password", password.getValue());
						VaadinSession.getCurrent().setAttribute("phone", phone.getValue());
						String response = invokeServiceCall(emailId.getValue(), firstName.getValue(),
								lastName.getValue(), password.getValue(), phone.getValue());
						if (response.contains("success")) {
							message.setVisible(true);
							emailId.setVisible(false);
							firstName.setVisible(false);
							lastName.setVisible(false);
							password.setVisible(false);
							phone.setVisible(false);
							send.setVisible(false);
						} else if (response.contains("already exist")) {
							Notification n = new Notification("<b>User Already Exist!</b>",
									Notification.Type.ERROR_MESSAGE);
							n.setDelayMsec(2000);
							n.setHtmlContentAllowed(true);
							n.setPosition(Position.BOTTOM_LEFT);
							n.show(Page.getCurrent());
						} else if (response.contains("expired")) {
							message.setValue(
									"<font color=\"#197de1\"><b>Verification URL got expired/re-generate the Verification Mail</b></font>");
							message.setVisible(true);
							emailId.setVisible(false);
							firstName.setVisible(false);
							lastName.setVisible(false);
							password.setVisible(false);
							phone.setVisible(false);
							send.setVisible(false);
						} else {
							Notification n = new Notification("<b>Oops! something went wrong while registration</b>",
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
		} else {
			message.setValue(
					"<font color=\"#197de1\"><b>Verification URL got expired/re-generate the Verification Mail</b></font>");
			message.setContentMode(ContentMode.HTML);
		}
		content.addComponent(message);
		content.setSizeFull();
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

	public String invokeServiceCall(String email, String fName, String lName, String pwd, String ph) {
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
			obj.put("phone", ph);
			obj.put("contextURL", VaadinSession.getCurrent().getAttribute("contextURL").toString() + "/rest/user");
			obj.put("token", VaadinSession.getCurrent().getAttribute("token").toString());
			String keyVal = new String(encoder.encode(obj.toString().getBytes()));
			obj = new JSONObject();
			obj.put("key", keyVal);
			serviceURL = contextURL + "/rest/user/approval/{input}";

			RestTemplate restTemplate = new RestTemplate();
			serviceResponse = restTemplate.postForObject(serviceURL, "", String.class, obj);

		} catch (JSONException e) {
			e.printStackTrace();
		}
		return serviceResponse;
	}

}
