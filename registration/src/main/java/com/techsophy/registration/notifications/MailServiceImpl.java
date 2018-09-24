package com.techsophy.registration.notifications;

import java.util.Base64;

import javax.mail.internet.MimeMessage;

import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.techsophy.registration.constants.Constants;
import com.techsophy.registration.util.Utils;

@Component
public class MailServiceImpl implements IMailService {

	@Autowired
	private JavaMailSender mailSender;

	@Override
	public String notify(JSONObject obj) {
		String response = "fail";
		try {
			if (obj != null) {
				String status = obj.get("status").toString();
				obj.remove("status");
				switch (status) {
				case Constants.STATUS_VERIFICATION_PENDING:
					response = sendVerificationEmailToUser(obj);
					break;
				case Constants.STATUS_APPROVAL_PENDING:
					response = sendPendingApprovalEmailToUser(obj);
					response = sendApprovalEmailToApprover(obj);
					break;
				case Constants.STATUS_APPROVED:
					response = sendAccountCreationSuccessEmailToUser(obj);
					break;
				case Constants.STATUS_APPROVAL_REJECTED:
					response = sendRejectionEmailFromApprover(obj);
					break;
				default:
					return "fail";
				}

			}
		} catch (Exception e) {
		}
		return response;
	}

	public String sendVerificationEmailToUser(JSONObject obj) {
		String result = "fail";
		String product = Utils.getPropertyValue("product");

		try {
			String emailId = obj.get("emailId") != null ? obj.get("emailId").toString() : "";
			String[] emailIds = { emailId };

			String mailBody = Utils.getPropertyValue(Constants.REGISTRATION_VERIFICATION_EMAIL_BODY);
			
			if(mailBody.contains(".txt")) {
				mailBody = Utils.getMailBodyContent(mailBody);
			}

			mailBody = Utils.prepareVerificationMailBody(mailBody, product, obj);

			String mailSubject = Utils.getPropertyValue(Constants.REGISTRATION_VERIFICATION_EMAIL_SUBJECT);
			mailSubject = mailSubject.replace("$product", product);

			if (mailBody != null && !"".equalsIgnoreCase(mailBody)) {
				result = send(emailIds, mailSubject, mailBody);

			}
		} catch (Exception e) {

		}
		return result;
	}

	public String sendPendingApprovalEmailToUser(JSONObject obj) {
		String result = "fail";
		String product = Utils.getPropertyValue("product");

		try {
			String emailId = obj.get("emailId") != null ? obj.get("emailId").toString() : "";
			String firstName = obj.get("firstName") != null ? obj.get("firstName").toString() : "";

			String[] emailIds = { emailId };

			String mailBody = Utils.getPropertyValue(Constants.REGISTRATION_PENDING_APPROVAL_EMAIL_BODY);
			mailBody = mailBody.replace("$firstName", firstName);
			mailBody = mailBody.replace("$product", product);

			String mailSubject = Utils.getPropertyValue(Constants.REGISTRATION_PENDING_APPROVAL_EMAIL_SUBJECT);
			mailSubject = mailSubject.replace("$product", product);
			if (mailBody != null && !"".equalsIgnoreCase(mailBody)) {
				result = send(emailIds, mailSubject, mailBody);
			}
		} catch (Exception e) {

		}
		return result;
	}

	public String sendApprovalEmailToApprover(JSONObject obj) {
		String result = "fail";
		String product = Utils.getPropertyValue("product");

		try {
			String adminEmailIds = Utils.getPropertyValue("adminemail");
			String[] emailIds = { adminEmailIds };

			String mailBody = Utils.getPropertyValue(Constants.REGISTRATION_APPROVAL_EMAIL_BODY);
			mailBody = Utils.prepareApprovalMailBody(mailBody, product, obj);

			String mailSubject = Utils.getPropertyValue(Constants.REGISTRATION_APPROVAL_EMAIL_SUBJECT);
			mailSubject = mailSubject.replace("$product", product);

			if (mailBody != null && !"".equalsIgnoreCase(mailBody)) {
				result = send(emailIds, mailSubject, mailBody);
			}
		} catch (Exception e) {

		}
		return result;
	}

	public String sendRejectionEmailFromApprover(JSONObject obj) {
		String result = "fail";
		String product = Utils.getPropertyValue("product");
		try {
			String emailId = obj.get("emailId") != null ? obj.get("emailId").toString() : "";
			String firstName = obj.get("firstName") != null ? obj.get("firstName").toString() : "";
			String[] emailIds = { emailId };

			String mailBody = Utils.getPropertyValue(Constants.REGISTRATION_APPROVAL_REJCTION_EMAIL_BODY);
			mailBody = mailBody.replace("$firstName", firstName);
			mailBody = mailBody.replace("$product", product);

			String mailSubject = Utils.getPropertyValue(Constants.REGISTRATION_APPROVAL_REJECTION_EMAIL_SUBJECT);
			mailSubject = mailSubject.replace("$product", product);

			if (mailBody != null && !"".equalsIgnoreCase(mailBody)) {
				result = send(emailIds, mailSubject, mailBody);

			}
		} catch (Exception e) {

		}
		return result;
	}

	public String sendAccountCreationSuccessEmailToUser(JSONObject obj) {
		String result = "fail";
		String product = Utils.getPropertyValue("product");
		Base64.Encoder encoder = Base64.getEncoder();
		try {
			String emailId = obj.get("emailId") != null ? obj.get("emailId").toString() : "";
			String firstName = obj.get("firstName") != null ? obj.get("firstName").toString() : "";
			String contextURL = obj.getString("contextURL").toString();
			JSONObject obj1 = new JSONObject();
			obj1.put("key", new String(encoder.encodeToString(obj.toString().getBytes())));
			String link = contextURL + "/login?identifier=" + obj1.toString();
			String[] emailIds = { emailId };

			String mailBody = Utils.getPropertyValue(Constants.REGISTRATION_SUCCESS_EMAIL_BODY);
			mailBody = mailBody.replace("$firstName", firstName);
			mailBody = mailBody.replace("$product", product);
			mailBody = mailBody.replace("$link", link);

			String mailSubject = Utils.getPropertyValue(Constants.REGISTRATION_SUCCESS_EMAIL_SUBJECT);
			mailSubject = mailSubject.replace("$product", product);

			if (mailBody != null && !"".equalsIgnoreCase(mailBody)) {
				result = send(emailIds, mailSubject, mailBody);
			}
		} catch (Exception e) {

		}
		return result;
	}

	@Override
	public String send(String[] to, String subject, String msg) {
		String response = "success";
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, false, "utf-8");
			message.setContent(msg, "text/html");
			helper.setSubject(subject);
			helper.setTo(to);
			helper.setFrom("nagaraju.s@techsophy.com");
			mailSender.send(message);
		} catch (Exception e) {
			response = "fail";
			e.printStackTrace();
		}
		return response;
	}

}
