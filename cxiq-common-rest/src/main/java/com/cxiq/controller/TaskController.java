package com.cxiq.controller;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cxiq.authentication.CXIQAuthenticationProvider;
import com.cxiq.service.cxiq.CXIQAdminService;
import com.cxiq.service.utils.QueryGeneratorService;

@RestController
@RequestMapping(value = "/service")
public class TaskController {
	static final Logger LOGGER = Logger.getLogger(TaskController.class.getName());

	@Autowired
	CXIQAdminService cxiqAdmin;

	@Autowired
	QueryGeneratorService qg;

	@Autowired
	CXIQAuthenticationProvider authProvider;

	@PreAuthorize("hasAnyAuthority('user', 'manager', 'admin')")
	@RequestMapping(value = "/tasks/{taskId}/reassign", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String reassignTask(@PathVariable("taskId") String taskId, @RequestParam("fromUserID") String fromUserID,
			@RequestParam("toUserID") String toUserID) {

		if (LOGGER.isDebugEnabled())
			LOGGER.debug("ReAssign Task : taskId= " + taskId + ",fromUserID=" + fromUserID + ",toUserID=" + toUserID);
		String result = "{\"nps\":0}";
		boolean result1 = qg.reassignTask(taskId, fromUserID, toUserID);
		if (result1)
			result = "{\"nps\":1}";
		return result;

	}

	@PreAuthorize("hasAuthority('admin')")
	@RequestMapping(value = "/messages/triage/status", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String updateTriageStatusForMessages(@RequestParam("tenantId") String tenantId,
			@RequestParam("brand") String brand, @RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "limit", required = false) String limit) {
		int limitValue = limit!=null ? Integer.parseInt(limit):0;
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Update triage status for messages ");
		String result = "";
		result = cxiqAdmin.updateTriageStatusForMessages(tenantId, brand, status, limitValue);
		return result;
	}

}
