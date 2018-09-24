package com.cxiq.service.utils;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cxiq.constants.CXIQConstants;
import com.cxiq.service.activiti.ActivitiRestClientService;

@Service
public class EmailService {
	static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());

	@Autowired
	ActivitiRestClientService activitiClient;

	@Value("${cxiq.admin}")
	String cxiqAdmin;

	@Value("${cxiq.admin.password}")
	String cxiqAdminPassword;

	public String sendEmail(String emailId, String subject, String comments) {
		String tenantId = CXIQConstants.DEFAULT_TENANT;
		String body3 = "{" + "\"processDefinitionKey\":\"CXIQSendMailWithAttachment\"," + "\"tenantId\": \"" + tenantId
				+ "\"," + "\"variables\": [" + "{" + "\"name\":\"assignee\"," + "\"value\":\"" + emailId + "\"" + "} ,"
				+ "{" + "\"name\":\"subject\"," + "\"value\":\"" + subject + "\"" + "} ," + "{"
				+ "\"name\":\"comments\"," + "\"value\":\"" + comments + "\"" + "}" + "]" + "}";

		String authUser = cxiqAdmin;
		String authPassword = cxiqAdminPassword;
		String response = "", procInstId = "";
		response = activitiClient.invokeRestCall("runtime/process-instances", body3, authUser, authPassword,
				"POST");
		procInstId = activitiClient.getKeyValueFromResponse("id", response);
		if (procInstId == null && "".equalsIgnoreCase(procInstId)) {
			LOGGER.error("Unable to send email - " + response);
			procInstId = "";
		}

		return procInstId;
	}
}
