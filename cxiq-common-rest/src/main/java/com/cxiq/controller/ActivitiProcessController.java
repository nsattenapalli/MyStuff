package com.cxiq.controller;

import java.util.Map;

import org.activiti.engine.RuntimeService;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cxiq.service.activiti.ActivitiRestClientService;

@RestController
@RequestMapping(value = "/service")
public class ActivitiProcessController {

	@Autowired
	ActivitiRestClientService activitiClient;

	@Value("${cxiq.admin}")
	String cxiqAdmin;

	@Value("${cxiq.admin.password}")
	String cxiqAdminPassword;

	@Autowired
	private RuntimeService runtimeService;

	@RequestMapping(value = "/triage", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
	public String startTriageProcess() {
		try {
			long processCount = 0;
			runtimeService.startProcessInstanceByKey("CXIQTriageProcess");
			processCount = runtimeService.createProcessInstanceQuery().count();
			JSONObject response = new JSONObject();
			response.put("TriageProcess", processCount);
			return response.toString();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return "{\"nps\":0}";

	}

	@RequestMapping(value = "/triageProcess", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String createTriageProcess(@RequestParam(required=false, value="tenantId") String tenantId, @RequestParam("brand") String brand) {
		String response = "";
		String body = " {" + "\"processDefinitionKey\": \"CXIQTriageProcess\"," + " \"tenantId\": \"" + tenantId + "\","
				+ "  \"variables\": [{" + "  \"name\": \"brand\"," + "  \"value\": \"" + brand + "\"" + "}]}";
		response = activitiClient.invokeRestCall("runtime/process-instances", body, cxiqAdmin, cxiqAdminPassword,
				"POST");
		String id = activitiClient.getKeyValueFromResponse("id", response);

		if (id != null && !"".equalsIgnoreCase(id))
			return "{\"nps\":1}";

		return "{\"nps\":0}";

	}

	@RequestMapping(value = "/nps", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
	public String startCollaborationProcess(@RequestBody Map<String, Object> data) {
		try {
			long processCount = 0;
			runtimeService.startProcessInstanceByKey("CXIQProcess", data);
			processCount = runtimeService.createProcessInstanceQuery().count();
			JSONObject response = new JSONObject();
			response.put("CXIQProcess", processCount);
			return response.toString();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return "{\"nps\":0}";
	}
}
