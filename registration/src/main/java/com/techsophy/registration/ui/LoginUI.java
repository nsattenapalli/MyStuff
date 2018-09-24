package com.techsophy.registration.ui;

import com.techsophy.registration.dao.UserDAO;
import com.techsophy.registration.entities.Users;
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

public class LoginUI extends VerticalLayout implements View {
	private static final long serialVersionUID = 1L;
	public static final String NAME = "";

	public LoginUI() {
		Panel panel = new Panel("<center><b>Login</b></center>");
		panel.setSizeUndefined();
		addComponent(panel);

		FormLayout content = new FormLayout();
		TextField emailId = new TextField("Email ID");
		if (VaadinSession.getCurrent().getAttribute("emailId") != null) {
			emailId.setValue(VaadinSession.getCurrent().getAttribute("emailId").toString());
			emailId.setReadOnly(true);
		}
		content.addComponent(emailId);

		PasswordField password = new PasswordField("Password");
		content.addComponent(password);

		Label message = new Label();
		message.setValue("");
		message.setContentMode(ContentMode.HTML);
		message.setVisible(false);

		Button send = new Button("Login");
		send.addClickListener(new ClickListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(ClickEvent event) {
				if (!emailId.getValue().isEmpty() && !password.getValue().isEmpty()) {
					VaadinSession.getCurrent().setAttribute("emailId", emailId.getValue());
					VaadinSession.getCurrent().setAttribute("password", password.getValue());
					String firstName = invokeDB(emailId.getValue(), password.getValue());
					if (!firstName.isEmpty()) {
						emailId.setVisible(false);
						password.setVisible(false);
						send.setVisible(false);
						message.setValue("<b>Welcome back " + firstName + "</b>");
						message.setVisible(true);
					} else {
						message.setValue("<b>Please check your credentials</b>");
						message.setVisible(true);
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
		content.setSizeFull();
		content.setMargin(true);
		panel.setContent(content);
		setComponentAlignment(panel, Alignment.MIDDLE_CENTER);

	}

	@Override
	public void enter(ViewChangeEvent event) {

		Notification n = new Notification("Welcome to trail account login!", Notification.Type.HUMANIZED_MESSAGE);
		n.setDelayMsec(1000);
		n.setPosition(Position.TOP_CENTER);
		n.show(Page.getCurrent());

	}

	public String invokeDB(String email, String pwd) {
		UserDAO userDAO = (UserDAO) VaadinSession.getCurrent().getAttribute("userDAO");
		Users users = userDAO.get(email);
		if (users != null && users.getPassword().equals(pwd)) {
			return users.getFirstName();
		}
		return "";
	}

}
