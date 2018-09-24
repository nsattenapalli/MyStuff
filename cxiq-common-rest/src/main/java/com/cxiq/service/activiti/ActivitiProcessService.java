package com.cxiq.service.activiti;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ActivitiProcessService {

	static final Logger LOGGER = Logger.getLogger(ActivitiProcessService.class.getName());

	@Autowired
	ActivitiRestClientService activitiClient;

	@Value("${cxiq.admin}")
	String cxiqAdmin;

	@Value("${cxiq.admin.password}")
	String cxiqAdminPassword;

	public boolean TriggerCXIQProcess(String tenantId, String taskCreator, String assignee, String subject,
			String description, String topic, String taskpriority, String priority, String homeUrl,
			String taskDirectUrl, String brand) {

		boolean isCreated = false;
		String response = "";
		String body = " {" + "\"processDefinitionKey\": \"CXIQProcess\"," + " \"tenantId\": \"" + tenantId + "\","
				+ "  \"variables\": [{" + "  \"name\": \"topic\"," + "  \"value\": \"" + topic + "\"" + "  }, {"
				+ "  \"name\": \"subject\"," + " \"value\": \"" + subject + "\"" + "  }, {"
				+ " \"name\": \"description\"," + " \"value\": \"" + description + "\"" + " }, {"
				+ "   \"name\": \"taskpriority\"," + "  \"value\": \"" + taskpriority + "\"" + "  }, {"
				+ "   \"name\": \"taskCreator\"," + " \"value\": \"" + taskCreator + "\"" + "        }, {"
				+ "  \"name\": \"homeUrl\"," + " \"value\": \"" + homeUrl + "\"" + "  }, {" + " \"name\": \"priority\","
				+ " \"value\": \"" + priority + "\"" + " }, {" + " \"name\": \"taskDirectUrl\"," + " \"value\": \""
				+ taskDirectUrl + "\"" + " }, {" + " \"name\": \"assignee\"," + " \"value\": \"" + assignee + "\""
				+ "}, {" + "\"name\": \"brand\"," + "\"value\": \"" + brand + "\"" + "}]" + "}";
		response = activitiClient.invokeRestCall("runtime/process-instances", body, cxiqAdmin, cxiqAdminPassword,
				"POST");
		String id = activitiClient.getKeyValueFromResponse("id", response);

		if (id != null && !"".equalsIgnoreCase(id))
			isCreated = true;

		return isCreated;
	}

	public boolean TriggerTriageProcess(String tenantId, String brand) {
		boolean isCreated = false;
		String response = "";
		String body = " {" + "\"processDefinitionKey\": \"TriageProcess\"," + " \"tenantId\": \"" + tenantId + "\","
				+ "  \"variables\": [{" + "  \"name\": \"brand\"," + "  \"value\": \"" + brand + "\"" + "}]}";
		response = activitiClient.invokeRestCall("runtime/process-instances", body, cxiqAdmin, cxiqAdminPassword,
				"POST");
		String id = activitiClient.getKeyValueFromResponse("id", response);

		if (id != null && !"".equalsIgnoreCase(id))
			isCreated = true;

		return isCreated;
	}

	public boolean deleteActivitiData(String tenantId) {
		boolean isDeleted = false;
		String response = "";
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Get deployments for - " + tenantId);

		response = activitiClient.invokeRestCall("repository/deployments?tenantId=" + tenantId, "", cxiqAdmin,
				cxiqAdminPassword, "GET");
		JSONObject jsonObj = null;
		JSONArray data = null;
		try {
			jsonObj = new JSONObject(response);
			if (jsonObj != null) {
				data = jsonObj.getJSONArray("data");
				if (data.length() == 0)
					return true;

				for (int i = 0; i < data.length(); i++) {
					JSONObject jsonObj2 = data.getJSONObject(i);
					String deploymentID = (String) jsonObj2.get("id");
					isDeleted = deleteDeployment(deploymentID);
					if (!isDeleted)
						break;
				}
			}
		} catch (JSONException e) {
			LOGGER.error("Error in parsing json " + e);
		}
		return isDeleted;
	}

	private boolean deleteDeployment(String deploymentID) {
		boolean isDeleted = false;
		String response = "";
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Get process definitions for deployment - " + deploymentID);

		response = activitiClient.invokeRestCall("repository/process-definitions?deploymentId=" + deploymentID, "",
				cxiqAdmin, cxiqAdminPassword, "GET");
		JSONObject jsonObj = null;
		JSONArray data = null;
		try {
			jsonObj = new JSONObject(response);
			if (jsonObj != null) {
				data = jsonObj.getJSONArray("data");
				if (data.length() == 0)
					return true;

				for (int i = 0; i < data.length(); i++) {
					JSONObject jsonObj2 = data.getJSONObject(i);
					String procDefID = (String) jsonObj2.get("id");
					isDeleted = deleteProcessDefinitions(procDefID);
					if (!isDeleted)
						break;
				}
				if (isDeleted) {
					if (LOGGER.isDebugEnabled())
						LOGGER.debug("Deleting deployment - " + deploymentID);
					response = activitiClient.invokeRestCall("repository/deployments/" + deploymentID, "", cxiqAdmin,
							cxiqAdminPassword, "DELETE");
					if (response.indexOf("204") > 0) {
						if (LOGGER.isDebugEnabled())
							LOGGER.debug("Deleted deployment - " + deploymentID);
						isDeleted = true;
					}
				}
			}
		} catch (JSONException e) {
			LOGGER.error("Error in parsing json " + e);
		}
		return isDeleted;
	}

	private boolean deleteProcessDefinitions(String procDefID) {
		boolean isDeleted = false;
		String response = "";
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Get process instances for process definition - " + procDefID);

		response = activitiClient.invokeRestCall("runtime/process-instances?processDefinitionId=" + procDefID, "",
				cxiqAdmin, cxiqAdminPassword, "GET");
		JSONObject jsonObj = null;
		JSONArray data = null;
		try {
			jsonObj = new JSONObject(response);
			if (jsonObj != null) {
				data = jsonObj.getJSONArray("data");
				if (data.length() == 0)
					return true;

				for (int i = 0; i < data.length(); i++) {
					JSONObject jsonObj2 = data.getJSONObject(i);
					String procInstID = (String) jsonObj2.get("id");
					isDeleted = deleteProcessInstance(procInstID);
					if (!isDeleted)
						break;
				}
			}
		} catch (JSONException e) {
			LOGGER.error("Error in parsing json " + e);
		}
		return isDeleted;
	}

	private boolean deleteProcessInstance(String procInstID) {
		boolean isDeleted = false;
		String response = "";
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Deleting process instance - " + procInstID);
		response = activitiClient.invokeRestCall("runtime/process-instances/" + procInstID, "", cxiqAdmin,
				cxiqAdminPassword, "DELETE");
		if (response.indexOf("204") > 0) {
			if (LOGGER.isDebugEnabled())
				LOGGER.debug("Deleted process instance - " + procInstID);

			response = activitiClient.invokeRestCall("history/historic-process-instances/" + procInstID, "", cxiqAdmin,
					cxiqAdminPassword, "DELETE");
			if (response.indexOf("204") > 0) {
				if (LOGGER.isDebugEnabled())
					LOGGER.debug("Deleted history process instance - " + procInstID);

				isDeleted = true;
			}
		}
		return isDeleted;
	}
}
