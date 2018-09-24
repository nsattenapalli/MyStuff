package com.techsophy.registration.rest;

import java.util.Base64;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.QueryParam;

import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.techsophy.registration.constants.Constants;
import com.techsophy.registration.controller.IUserRegistration;
import com.techsophy.registration.controller.UserRegistrationImpl;
import com.techsophy.registration.notifications.IMailService;
import com.techsophy.registration.notifications.MailServiceImpl;
import com.techsophy.registration.util.Utils;

@RestController
@RequestMapping("/rest")
public class CommonRegistrationServices {

	@Autowired
	IUserRegistration userRegistration = new UserRegistrationImpl();

	@Autowired
	IMailService mailService = new MailServiceImpl();

	Base64.Decoder decoder = Base64.getDecoder();

	@RequestMapping(value = "/user/verification/{input}", method = RequestMethod.POST) // PUT
	@ResponseBody
	public String userVerification(@PathVariable("input") String input) {
		String result = "fail";
		String emailId = "";
		try {
			JSONObject requestObj = new JSONObject(input);
			String key = requestObj.get("key") != null ? requestObj.get("key").toString() : "";
			key = new String(decoder.decode(key));
			JSONObject keyValueObj = new JSONObject(key);
			emailId = keyValueObj.get("emailId") != null ? keyValueObj.get("emailId").toString() : "";

			if (!userRegistration.checkIfUserAlreadyExists(emailId, Constants.STATUS_VERIFICATION_PENDING)) {
				result = userRegistration.saveTempUser(keyValueObj);
				keyValueObj.put("status", Constants.STATUS_VERIFICATION_PENDING);
				result = mailService.notify(keyValueObj);
			} else {
				result = "<center><b><font color=\"#197de1\">User already exist!</font></b></center>";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;

	}

	@RequestMapping(value = "/user/approval/{input}", method = RequestMethod.POST)
	@ResponseBody
	public String userApproval(@PathVariable("input") String input) {
		String emailId = "";
		long token;
		String result = "<center><b><font color=\"#197de1\">User verification failed!</font></b></center>";
		try {
			JSONObject requestObj = new JSONObject(input);
			String key = requestObj.get("key") != null ? requestObj.get("key").toString() : "";
			key = new String(decoder.decode(key));
			JSONObject keyValueObj = new JSONObject(key);
			emailId = keyValueObj.get("emailId") != null ? keyValueObj.get("emailId").toString() : "";
			token = Long.parseLong(keyValueObj.get("token").toString())
					+ (Integer.parseInt(Utils.getPropertyValue("verification_expiry_minutes")) * 60 * 1000);
			if (System.currentTimeMillis() < token) {
				if (!userRegistration.checkIfUserAlreadyExists(emailId, Constants.STATUS_APPROVAL_PENDING)) {
					result = userRegistration.updateTempUser(keyValueObj, Constants.STATUS_APPROVAL_PENDING);
					if ("success".equalsIgnoreCase(result)) {
						keyValueObj.put("status", Constants.STATUS_APPROVAL_PENDING);
						result = mailService.notify(keyValueObj);
					}
				} else {
					result = "<center><b><font color=\"#197de1\">User already exist!</font></b></center>";
				}
			} else {
				result = "<center><b><font color=\"#197de1\">Verification URL got expired/ Please re-generate the Verificaiton Mail</font></b></center>";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@RequestMapping(value = "/user/{input}", method = RequestMethod.POST)
	@ResponseBody
	public String registerUser(@PathVariable("input") String input, HttpServletRequest request) {
		String result = "<center><b><font color=\"#197de1\">User approval failed!</font></b></center>";
		result = registerUserAccount(input, request);
		return result;
	}

	@RequestMapping(value = "/user", method = RequestMethod.GET)
	@ResponseBody
	public String registerUserAcct(@QueryParam("input") String input, HttpServletRequest request) {
		String result = "<center><b><font color=\"#197de1\">User approval failed!</font></b></center>";
		result = registerUserAccount(input, request);
		return result;
	}

	public String registerUserAccount(String input, HttpServletRequest request) {
		String emailId = "", status = "", result = "";
		long token;
		try {
			JSONObject requestObj = new JSONObject(input);
			String key = requestObj.get("key") != null ? requestObj.get("key").toString() : "";
			key = new String(decoder.decode(key));
			JSONObject keyValueObj = new JSONObject(key);
			token = Long.parseLong(keyValueObj.get("token").toString())
					+ (Integer.parseInt(Utils.getPropertyValue("approval_expiry_minutes")) * 60 * 1000);
			if (System.currentTimeMillis() < token) {

				keyValueObj.put("contextURL",
						"http://" + request.getLocalAddr() + ":" + request.getServerPort() + request.getContextPath());
				emailId = keyValueObj.get("emailId") != null ? keyValueObj.get("emailId").toString() : "";
				status = requestObj.get("status") != null ? requestObj.get("status").toString() : "";

				if (userRegistration.checkIfUserAlreadyExists(emailId, Constants.STATUS_APPROVED)) {
					result = "<center><b><font color=\"#197de1\">User has already approved and created</font></b></center>";
					return result;
				} else {
					if ("approved".equalsIgnoreCase(status)) {
						result = userRegistration.createUser(keyValueObj);
						if ("success".equalsIgnoreCase(result)) {
							result = userRegistration.updateTempUser(keyValueObj, Constants.STATUS_APPROVED);
							if ("success".equalsIgnoreCase(result)) {
								keyValueObj.put("status", Constants.STATUS_APPROVED);
								result = mailService.notify(keyValueObj);
								result = "<center><b><font color=\"#197de1\">User approved and created</font></b></center>";
							}
						}
					} else {
						if (userRegistration.checkIfUserAlreadyExists(emailId, Constants.STATUS_APPROVAL_REJECTED)) {
							result = "<center><b><font color=\"#197de1\">User has already rejected</font></b></center>";
							return result;
						} else {
							result = userRegistration.updateTempUser(keyValueObj, Constants.STATUS_APPROVAL_REJECTED);
							if ("success".equalsIgnoreCase(result)) {
								keyValueObj.put("status", Constants.STATUS_APPROVAL_REJECTED);
								result = mailService.notify(keyValueObj);
								result = "<center><b><font color=\"#197de1\">User approval rejected</font></b></center>";
							}
						}
					}
				}

			} else {
				//json format
				result = "<center><b><font color=\"#197de1\">Verification URL got expired/re-generate the Verification Mail</font></b></center>";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;

	}

}
