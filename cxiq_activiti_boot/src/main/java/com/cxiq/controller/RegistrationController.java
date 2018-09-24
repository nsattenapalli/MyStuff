package com.cxiq.controller;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cxiq.service.cxiq.AccountService;
import com.cxiq.service.cxiq.CXIQAdminService;
import com.cxiq.service.cxiq.RegistrationService;

@RestController
@RequestMapping(value = "/registration")
public class RegistrationController {
	static final Logger LOGGER = Logger.getLogger(RegistrationController.class.getName());

	@Autowired
	CXIQAdminService adminController;

	@Autowired
	AccountService accountService;
	
	@Autowired
	RegistrationService accountRegService;

	@RequestMapping(value = "/account/verification", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String sendAccountForVerification(@RequestParam("identifier") String identifier) {
		String emailId = "", firstName = "", lastName = "", link = "", tenantId = "", phoneNo = "";
		String result = "{\"nps\":\"false\"}";

		String[] contents = RegistrationService.getIdentifierValues(identifier);
		tenantId = contents[0];
		emailId = contents[1];
		firstName = contents[2];
		lastName = contents[3];
		phoneNo = contents[4];
		link = contents[5];

		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Account Verification : emailId - " + emailId);

		result = accountRegService.sendAccountForVerification(emailId, tenantId, firstName, lastName, phoneNo, link);
		return result;
	}

	@RequestMapping(value = "/account/approval", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String sendAccountForApproval(@RequestParam("identifier") String identifier) {

		String result = "{\"nps\":\"false\"}";
		String emailId = "", password = "", approvedLink = "", rejectedLink = "";

		String[] contents = RegistrationService.getIdentifierValues(identifier);
		emailId = contents[0];
		password = contents[1];
		approvedLink = contents[2];
		rejectedLink = contents[3];

		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Account Approval : emailId - " + emailId);

		result = accountRegService.sendAccountForApproval(emailId, password, approvedLink, rejectedLink);
		return result;
	}

	@RequestMapping(value = "/account", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
	public String registerAccount(@RequestParam("identifier") String identifier, @RequestParam("status") String status) {
		String result = "{\"nps\":\"false\"}";
		String emailId = "", link = "";
		String[] contents = RegistrationService.getIdentifierValues(identifier);
		emailId = contents[0];
		link = contents[1];
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Account Registration : emailId - " + emailId);

		result = accountRegService.register(emailId, link, status);
		return result;
	}

	@RequestMapping(value = "/account", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getAccountDetails(@RequestParam("userId") String userId) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Get Account Details for UserId - " + userId);
		String result = null;
		result = accountService.getAccountDetails(userId);
		return result;
	}

	@RequestMapping(value = "/accounts", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getAccounts() {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Get All Accounts ");
		String result = null;
		result = accountService.getAccounts();
		return result;
	}

	@RequestMapping(value = "/accounts/count_status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getCountOfAccountsByStatus() {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Get count of Accounts by status");
		String result = null;
		result = accountService.getCountOfAccountsByStatus();
		return result;
	}

	@RequestMapping(value = "/account/status", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String updateAccount(@RequestParam("userId") String userId, @RequestParam("tenantId") String tenantId,
			@RequestParam("status") String status) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Update account - " + userId);

		String result = "{\"nps\":\"false\"}";
		if (accountService.updateAccountDetails(userId, tenantId, "", status))
			result = "{\"nps\":\"true\"}";
		return result;
	}

	@RequestMapping(value = "/account/validation", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String isAccountValid(@RequestParam("userId") String userId, @RequestParam("tenantId") String tenantId) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Validating UserId - " + userId + " & tenant-" + tenantId);
		String result = "{\"nps\":\"false\"}";

		if (accountService.isAccountValid(userId, tenantId))
			result = "{\"nps\":\"true\"}";
		return result;
	}

	@RequestMapping(value = "/account", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
	public String deleteAccount(@RequestParam("tenantId") String tenantId, @RequestParam("userId") String userId) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Deleting Account for UserId - " + userId);
		String result = "{\"nps\":\"false\"}";
		String response = adminController.deleteTenant(tenantId);
		if ("true".equalsIgnoreCase(response)) {
			if (accountService.deleteAccount(userId))
				result = "{\"nps\":\"true\"}";
		}
		return result;
	}
}
