package com.cxiq.service.cxiq;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cxiq.constants.CXIQConstants;
import com.cxiq.service.activiti.ActivitiProcessService;
import com.cxiq.service.activiti.ActivitiRestClientService;
import com.cxiq.service.utils.DBUtils;
import com.cxiq.service.utils.EmailService;
import com.cxiq.service.utils.Utils;

@Service
public class RegistrationService {

	static final Logger LOGGER = Logger.getLogger(RegistrationService.class.getName());

	@Autowired
	AccountService accountService;

	@Autowired
	CXIQAdminService adminController;

	@Autowired
	DBUtils dbUtils;

	@Autowired
	ActivitiRestClientService activitiClient;

	@Autowired
	Utils utils;

	@Autowired
	EmailService emailService;

	@Autowired
	ActivitiProcessService activitiProcessService;

	@Value("${cxiq.admin}")
	String cxiqAdmin;

	@Value("${cxiq.admin.password}")
	String cxiqAdminPassword;

	@Value("${registration.email.invalidate.days}")
	long emailInvalidateDays;

	public String register(String emailId, String link, String status) {
		String result = "{\"nps\":\"false\"}";
		String firstName = "", lastName = "", password = "", tenantId = "", currentStatus = "";

		String response = accountService.getAccountDetails(emailId);
		tenantId = getValueFromJSONResult("tenant_id", response);
		firstName = getValueFromJSONResult("first_name", response);
		lastName = getValueFromJSONResult("last_name", response);
		currentStatus = getValueFromJSONResult("status", response);
		password = getValueFromJSONResult("pwd", response);

		if (!"PendingApproval".equalsIgnoreCase(currentStatus)) {
			result = "{\"nps\":\"Tenant already registered\"}";
			LOGGER.error("Tenant already registered -" + tenantId);
		} else {
			if ("Approved".equalsIgnoreCase(status)) {
				String response2 = adminController.createTenant(tenantId, tenantId);
				if ("false".equalsIgnoreCase(response2)) {
					LOGGER.error("Unable to create tenant -" + tenantId);
					return result;
				} else {
					boolean isExecuted = false;
					String dir = AccountService.class.getClassLoader().getResource("").getPath();
					HashMap<String, String> variables = new HashMap<String, String>();
					variables.put("$db", tenantId);
					variables.put("$user", emailId);
					// variables.put("$angry", "angry");
					// variables.put("$dissatisfied", "dissatisfied");
					// variables.put("$happy", "happy");
					isExecuted = dbUtils.executeSQLFile(variables, dir + CXIQConstants.CREATE_BRAND_SQL_FILE, "cxiq");
					if (isExecuted)
						isExecuted = dbUtils.executeSQLFile(variables, dir + CXIQConstants.IMPORT_BRAND_DATA_SQL_FILE,
								"cxiq");
					if (isExecuted)
						isExecuted = createAdminUserInTenant(emailId, password, firstName, lastName, link, tenantId);
					if (isExecuted)
						isExecuted = createCXIQTasks(tenantId, emailId, link);
					if (isExecuted)
						isExecuted = createTriageTasks(tenantId, emailId, variables);
					// update account status
					if (isExecuted)
						isExecuted = accountService.updateAccountDetails(emailId, tenantId, "", "Registered");
					String type = "";
					if (isExecuted) {
						// NPSAdmin.updateGroupInBrand(tenantId,NPSConstants.REG_ACCOUNT_BRAND,"angry","Angry
						// Customers","{"rule":"(nps_score>0 &&
						// nps_score<=6)","json":{"group":{"operator":"and","rules":[{"condition":">","column":"nps_score","value":"0","dataType":"number"},{"condition":"<=","column":"nps_score","value":"6","dataType":"number"}]}}}");
						type = CXIQConstants.REGISTRATION_SUCCESS_EMAIL;
					} else {
						adminController.deleteTenant(tenantId);
						type = CXIQConstants.REGISTRATION_FAILED_EMAIL;
					}

					String comments = getFileContent(type);
					comments = comments.replace("$firstName", firstName);
					comments = comments.replace("$link", link);
					comments = comments.replace("$tenantId", tenantId);

					if (comments != null && !"".equalsIgnoreCase(comments)) {
						String response1 = emailService.sendEmail(emailId,
								CXIQConstants.REGISTRATION_SUCCESS_EMAIL_SUBJECT, comments);
						if (response1 != null && !"".equalsIgnoreCase(response1) && isExecuted) {
							if (LOGGER.isDebugEnabled())
								LOGGER.debug("User -'" + emailId + "' registered successfully");
							result = "{\"nps\":\"true\"}";
						} else {
							LOGGER.error("Unable to register user -" + emailId);
						}
					}
					return result;
				}
			} else {
				if (LOGGER.isDebugEnabled())
					LOGGER.debug(" Rejected User - " + emailId);

				if (accountService.updateAccountDetails(emailId, tenantId, "", "Rejected"))
					result = sendRejectionEmailFromApprover(emailId, firstName);

			}
		}
		return result;

	}

	public String sendAccountForApproval(String emailId, String password, String approvedLink, String rejectedLink) {

		String result = "{\"nps\":\"false\"}";
		String firstName = "", lastName = "", phoneNo = "", tenantId = "", createdDate = "";

		// get account details
		String response = accountService.getAccountDetails(emailId);
		tenantId = getValueFromJSONResult("tenant_id", response);
		firstName = getValueFromJSONResult("first_name", response);
		lastName = getValueFromJSONResult("last_name", response);
		phoneNo = getValueFromJSONResult("phone_no", response);
		createdDate = getValueFromJSONResult("created_date", response);

		if (utils.getNoOfDaysFromDate(createdDate) >= emailInvalidateDays) {
			result = "{\"nps\":\"Invalidated\"}";
			return result;
		}
		// update password in account table
		if (!accountService.updateAccountDetails(emailId, tenantId, password, "PendingApproval"))
			return result;

		// Check if the email id already exists

		if (!adminController.checkIfTenantAlreadyExists(tenantId)) {
			result = sendApprovalEmailToApprover(tenantId, emailId, firstName, lastName, phoneNo, approvedLink,
					rejectedLink);
			String isCreated = activitiClient.getKeyValueFromResponse("cxiq", response);
			if ("false".equalsIgnoreCase(isCreated))
				return result;
			result = sendPendingApprovalEmailToUser(emailId, firstName);
		} else {
			LOGGER.error("Tenant already exists -" + emailId);
		}
		return result;
	}

	public String sendAccountForVerification(String emailId, String tenantId, String firstName, String lastName,
			String phoneNo, String link) {
		String result = "{\"nps\":\"false\"}";
		// Check if the tenant already exists
		if (!accountService.checkIfTenantAlreadyExists(tenantId) && !accountService.checkIfUserAlreadyExists(emailId)) {
			// create account in registration table
			boolean isCreated = accountService.createAccount(emailId, firstName, lastName, "", tenantId, phoneNo,
					"Submitted", "Trial");
			// Send verification mail to user
			if (isCreated)
				result = sendVerificationEmailToUser(emailId, firstName, link);
		} else {
			LOGGER.error("User or Tenant already exists -" + emailId);
		}
		return result;
	}

	public static String[] getIdentifierValues(String identifier) {
		Base64.Decoder decoder = Base64.getDecoder();
		String identifierVal = new String(decoder.decode(identifier));

		String[] contents = identifierVal.split(",");
		return contents;
	}

	public static String getValueFromJSONResult(String key, String response) {
		String result = "";
		JSONObject j = null;
		try {
			JSONArray a = new JSONArray(response);
			j = new JSONObject(a.getString(0));
			result = (String) j.get(key);
		} catch (JSONException e) {
			LOGGER.error("Error while parsing json array - " + response + " : " + e);
		}
		return result;
	}

	public String sendPendingApprovalEmailToUser(String emailId, String firstName) {

		String result = "{\"nps\":\"false\"}";

		String comments = getFileContent(CXIQConstants.REGISTRATION_PENDING_APPROVAL_EMAIL);
		comments = comments.replace("$firstName", firstName);

		if (comments != null && !"".equalsIgnoreCase(comments)) {
			String response = emailService.sendEmail(emailId, CXIQConstants.REGISTRATION_PENDING_APPROVAL_EMAIL_SUBJECT,
					comments);
			if (response != null && !"".equalsIgnoreCase(response)) {
				if (LOGGER.isDebugEnabled())
					LOGGER.debug("Pending Approval email sent to user -" + emailId + " successfully");
				result = "{\"nps\":\"true\"}";
			} else {
				LOGGER.error("Unable to send Pending Approval email to user -" + emailId);
			}
		} else {
			LOGGER.error("Unable to find email body in file - " + CXIQConstants.REGISTRATION_PENDING_APPROVAL_EMAIL);
		}
		return result;
	}

	public String sendApprovalEmailToApprover(String tenantId, String emailId, String firstName, String lastName,
			String phoneNo, String approvedLink, String rejectedLink) {

		String result = "{\"nps\":\"false\"}";

		String comments = getFileContent(CXIQConstants.REGISTRATION_APPROVAL_EMAIL);
		String admin = cxiqAdmin;
		comments = comments.replace("$tenantId", tenantId);
		comments = comments.replace("$admin", admin);
		comments = comments.replace("$emailId", emailId);
		comments = comments.replace("$firstName", firstName);
		comments = comments.replace("$lastName", lastName);
		comments = comments.replace("$phoneNo", phoneNo);
		comments = comments.replace("$approvedLink", approvedLink);
		comments = comments.replace("$rejectedLink", rejectedLink);

		if (comments != null && !"".equalsIgnoreCase(comments)) {
			String response = emailService.sendEmail(admin, CXIQConstants.REGISTRATION_APPROVAL_EMAIL_SUBJECT, comments);
			if (response != null && !"".equalsIgnoreCase(response)) {
				if (LOGGER.isDebugEnabled())
					LOGGER.debug("Approval email sent to admin -" + emailId + " successfully");
				result = "{\"nps\":\"true\"}";
			} else {
				LOGGER.error("Unable to send approval email to admin - " + admin);
			}

		} else {
			LOGGER.error("Unable to find email body in file - " + CXIQConstants.REGISTRATION_PENDING_APPROVAL_EMAIL);
		}
		return result;
	}

	public String sendRejectionEmailFromApprover(String emailId, String firstName) {

		String result = "{\"nps\":\"false\"}";

		String comments = getFileContent(CXIQConstants.REGISTRATION_APPROVAL_REJCTION_EMAIL);
		comments = comments.replace("$firstName", firstName);

		if (comments != null && !"".equalsIgnoreCase(comments)) {
			String response = emailService.sendEmail(emailId,
					CXIQConstants.REGISTRATION_APPROVAL_REJECTION_EMAIL_SUBJECT, comments);
			if (response != null && !"".equalsIgnoreCase(response)) {
				if (LOGGER.isDebugEnabled())
					LOGGER.debug("Approval rejection email sent to user -" + emailId + " successfully");
				result = "{\"nps\":\"true\"}";
			} else {
				LOGGER.error("Unable to send approval rejection email to user - " + emailId);
			}

		} else {
			LOGGER.error("Unable to find email body in file - " + CXIQConstants.REGISTRATION_PENDING_APPROVAL_EMAIL);
		}
		return result;
	}

	public boolean checkIfUserAlreadyExists(String emailId) {
		boolean result = false;

		String authUser = cxiqAdmin;
		String authPassword = cxiqAdminPassword;

		result = checkInActiviti(emailId, authUser, authPassword);
		return result;
	}

	public String sendVerificationEmailToUser(String emailId, String firstName, String link) {

		String result = "{\"nps\":\"false\"}";

		String comments = getFileContent(CXIQConstants.REGISTRATION_VERIFICATION_EMAIL);
		comments = comments.replace("$firstName", firstName);
		comments = comments.replace("$link", link);

		if (comments != null && !"".equalsIgnoreCase(comments)) {
			String response = emailService.sendEmail(emailId, CXIQConstants.REGISTRATION_VERIFICATION_EMAIL_SUBJECT,
					comments);
			if (response != null && !"".equalsIgnoreCase(response)) {
				if (LOGGER.isDebugEnabled())
					LOGGER.debug("Verification email sent to user -" + emailId + " successfully");
				result = "{\"nps\":\"true\"}";
			} else {
				LOGGER.error("Unable to send verification email to user - " + emailId);
			}

		} else {
			LOGGER.error("Unable to find email body in file - " + CXIQConstants.REGISTRATION_PENDING_APPROVAL_EMAIL);
		}
		return result;
	}

	public boolean createAdminUserInTenant(String emailId, String password, String firstName, String lastName,
			String link, String tenantId) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Creating User -" + emailId);

		boolean isRegistered = false;

		String response = "";
		String body = "{" + "\"id\":\"" + emailId + "\"," + "\"firstName\":\"" + firstName + "\"," + "\"lastName\":\""
				+ lastName + "\"," + "\"email\":\"" + emailId + "\"," + "\"password\":\"" + password + "\"" + "}";
		String authUser = cxiqAdmin;
		String authPassword = cxiqAdminPassword;
		response = activitiClient.invokeRestCall("identity/users", body, authUser, authPassword, "POST");
		String id = activitiClient.getKeyValueFromResponse("id", response);

		if (id.equalsIgnoreCase(emailId))
			isRegistered = true;
		else {
			LOGGER.error("Unable to create user in activiti -" + emailId);
		}
		if (isRegistered) {
			String adminRole = "nps_role_" + tenantId + "_admin";
			response = activitiClient.invokeRestCall("identity/groups/" + adminRole + "/members",
					"{\"userId\":\"" + emailId + "\"}", authUser, authPassword, "POST");
			String userId = activitiClient.getKeyValueFromResponse("userId", response);
			response = activitiClient.invokeRestCall("identity/groups/" + "nps_tenant_" + tenantId + "/members",
					"{\"userId\":\"" + emailId + "\"}", authUser, authPassword, "POST");
			userId = activitiClient.getKeyValueFromResponse("userId", response);
			if (userId.equalsIgnoreCase(emailId))
				isRegistered = true;
			else
				isRegistered = false;
		} else {
			LOGGER.error("Unable to assign admin role to user in tenant - " + tenantId);
		}

		return isRegistered;
	}

	public String getFileContent(String type) {
		String content = "";
		String filePath = RegistrationService.class.getClassLoader().getResource("").getPath() + "//" + type;
		try {
			content = new String(Files.readAllBytes(Paths.get(filePath)));
		} catch (IOException e) {
			LOGGER.error("In getFileContent method catch block due to " + e.getMessage());
		}
		return content;
	}

	public boolean createCXIQTasks(String tenantId, String emailId, String link) {
		boolean isCreated = false;
		HashMap<Integer, String> subject = new HashMap<Integer, String>();
		subject.put(1, "Ammenities GYM feedback not good");
		subject.put(2, "Room Cleanliness requires more attention");
		HashMap<Integer, String> description = new HashMap<Integer, String>();
		description.put(1, "Please followUp with the gym facilities");
		description.put(2, "Room Cleanliness issue has to be resolved immediately");
		HashMap<Integer, String> topic = new HashMap<Integer, String>();
		topic.put(1, "ammenities_gym");
		topic.put(2, "ammenities_room");
		HashMap<Integer, String> taskpriority = new HashMap<Integer, String>();
		taskpriority.put(1, "95");
		taskpriority.put(2, "50");
		HashMap<Integer, String> priority = new HashMap<Integer, String>();
		priority.put(1, "High");
		priority.put(2, "Medium");

		for (Object i : subject.keySet()) {
			isCreated = activitiProcessService.TriggerCXIQProcess(tenantId, cxiqAdmin, emailId, subject.get(i),
					description.get(i), topic.get(i), taskpriority.get(i), priority.get(i), link, link + "#/my-tasks",
					CXIQConstants.REG_ACCOUNT_BRAND);
			if (!isCreated)
				break;
			else {
				LOGGER.error("Unable to create task in activiti for tenant- " + tenantId);
			}
		}
		return isCreated;

	}

	public boolean createTriageTasks(String tenantId, String user, HashMap<String, String> variables) {
		boolean isExecuted = false;
		String dir = RegistrationService.class.getClassLoader().getResource("").getPath();
		isExecuted = dbUtils.executeSQLFile(variables, dir + CXIQConstants.IMPORT_TRIAGE_DATA_SQL_FILE, "activiti");
		if (isExecuted) {
			String result = adminController.updateTriageStatusForMessages(tenantId, CXIQConstants.REG_ACCOUNT_BRAND, "",
					10);
			String id = activitiClient.getKeyValueFromResponse("cxiq", result);
			if ("true".equalsIgnoreCase(id))
				isExecuted = true;
			else {
				LOGGER.error("Unable to update status for triage records in tenant - " + tenantId);
			}
		}
		if (isExecuted)
			isExecuted = activitiProcessService.TriggerTriageProcess(tenantId, CXIQConstants.REG_ACCOUNT_BRAND);
		else {
			LOGGER.error("Unable to trigger triage process for tenant - " + tenantId);
		}
		return isExecuted;
	}
	
	public boolean checkInActiviti(String id, String authUser, String authPassword) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Checking credentials in Activiti for  : username=" + authUser + ",passowrd=" + authPassword);
		boolean validUser = false;
		String result = activitiClient.invokeRestCall("identity/users/" + id, "", authUser, authPassword,
				"GET");
		JSONObject jsonObj = null;
		try {
			jsonObj = new JSONObject(result);

			if (jsonObj != null) {
				String userId = (String) jsonObj.get("id");
				if (userId.equalsIgnoreCase(id))
					validUser = true;
			}

		} catch (JSONException e) {
			LOGGER.error("Error in getting user credentials from Activiti " + e);
		}

		return validUser;
	}
}
