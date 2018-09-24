package com.cxiq.controller;

import java.util.Map;

import org.activiti.engine.RuntimeService;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/process")
public class ProcessController {

	@Autowired
	private RuntimeService runtimeService;

	@RequestMapping(value = "/triage", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
	public String startTriageProcess() {
		try {
			long processCount = 0;
			runtimeService.startProcessInstanceByKey("TriageProcess");
			processCount = runtimeService.createProcessInstanceQuery().count();
			JSONObject response = new JSONObject();
			response.put("TriageProcess", processCount);
			return response.toString();
		} catch (JSONException e) {
			e.printStackTrace();
		}
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
