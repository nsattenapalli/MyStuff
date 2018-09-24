package com.cxiq.service.activiti;

import java.io.File;
import java.util.Arrays;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import sun.misc.BASE64Encoder;

@Service
public class ActivitiRestClientService {

	@Value("${activiti.hostname}")
	String activitiHost;

	@Value("${activiti.port}")
	String activitiPort;

	@Value("${cxiq.admin}")
	String cxiqAdmin;

	@Value("${cxiq.admin.password}")
	String cxiqAdminPassword;

	static final Logger LOGGER = Logger.getLogger(ActivitiRestClientService.class.getName());

	public String invokeRestCall(String url, String body, String username, String password, String methodType) {
		String output = "";
		String authString = username + ":" + password;
		String authStringEnc = new BASE64Encoder().encode(authString.getBytes());
		String authHeader = "Basic " + new String(authStringEnc);
		try {

			url = "http://" + activitiHost + ":" + activitiPort + "/cxiq-common-rest/" + url;

			HttpHeaders headers = new HttpHeaders();
			headers.setAccept(Arrays.asList(new MediaType[] { MediaType.APPLICATION_JSON }));
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("Authorization", authHeader);

			HttpEntity<String> entity = new HttpEntity<String>(body, headers);

			RestTemplate restTemplate = new RestTemplate();

			ResponseEntity<String> response;

			if ("GET".equalsIgnoreCase(methodType)) {
				response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

			}

			else if ("POST".equalsIgnoreCase(methodType)) {
				response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
			} else if ("DELETE".equalsIgnoreCase(methodType)) {
				response = restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
			} else {
				output = "Invalid method type - " + methodType;
				LOGGER.error("Invalid method type - " + methodType);
				throw new RuntimeException("Invalid method type - " + methodType);
			}

			if ("GET".equalsIgnoreCase(methodType) && response.getStatusCodeValue() != 200) {
				output = "Unable to connect to the rest service - " + url;
				LOGGER.error("Unable to connect to the rest service - " + output);
				throw new RuntimeException(output);
			} else if ("POST".equalsIgnoreCase(methodType)
					&& (response.getStatusCodeValue() != 200 && response.getStatusCodeValue() != 201)) {
				throw new RuntimeException("Failed : HTTP error code : " + response.getStatusCodeValue());
			} else if ("DELETE".equalsIgnoreCase(methodType) && response.getStatusCodeValue() != 204) {
				throw new RuntimeException("Failed : HTTP error code : " + response.getStatusCodeValue());
			} else
				output = response.getBody();
		} catch (Exception e) {
			LOGGER.error("Exception in executing the rest service - " + url + " :" + e.getMessage());
			output = e.getMessage();
		}
		return output;

	}

	public String getKeyValueFromResponse(String key, String response) {
		JSONObject jsonObj = null;
		String value = "";
		try {
			jsonObj = new JSONObject(response);

			if (jsonObj != null) {
				value = (String) jsonObj.get(key);
			}

		} catch (JSONException e) {
			LOGGER.error("Error in getting value for key: " + key + " - " + e);
		}
		return value;
	}

	public boolean deployProcesses(String tenantId, String filePath) {

		String authString = cxiqAdmin + ":" + cxiqAdminPassword;
		String authorization = "Basic " + new BASE64Encoder().encode(authString.getBytes());
		String url = "service/repository/deployments?tenantId=" + tenantId;
		url = "http://" + activitiHost + ":" + activitiPort + "/activiti-rest/" + url;
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("POST Request : url=" + url + ",filePath=" + filePath + ",tenantId=" + tenantId);
		}

		File fileToUpload = new File(filePath);

		MultiValueMap<String, Object> bodyMap = new LinkedMultiValueMap();

		bodyMap.add("file", fileToUpload);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		headers.set("Authorization", authorization);

		HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(bodyMap, headers);

		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("POST Request Result = " + response.getBody());
		}
		if (response.getStatusCodeValue() != 201) {
			LOGGER.error("In deployProcesses method catch block due to " + response.getBody());
			throw new RuntimeException(response.getBody());

		}
		return true;

	}

}
