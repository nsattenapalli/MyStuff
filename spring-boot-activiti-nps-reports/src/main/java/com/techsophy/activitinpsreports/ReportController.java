package com.techsophy.activitinpsreports;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/activiti")
public class ReportController {

	static final Logger LOGGER = Logger.getLogger(ReportController.class.getName());
	
	@Autowired
	QueryGenerator qg;

	@Autowired
	Utils utils;

	@Qualifier("activiti")
	@Autowired
	DataSource activitiDataSource;

	@Qualifier("nps")
	@Autowired
	DataSource npsDataSource;

	@RequestMapping(value = "/report/{type}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String prcoessReport(@RequestBody Map<String, String> queryData, @PathVariable String type) {
		String reportQuery = "";
		switch (type) {
		case "gettasksperuser":
			reportQuery = qg.generateTasksPerUserQuery(queryData);
			break;
		case "gettasksbyname":
			reportQuery = qg.generateCountOfTasksByTaskNameQuery(queryData);
			break;
		case "gettasksbyuser":
			reportQuery = qg.generateCountOfOpenTasksByUserQuery(queryData);
			break;
		case "getavgtaskagebypriority":
			reportQuery = qg.generateAvgTaskAgingByPriorityQuery(queryData);
			break;
		case "getstucktasksbyage":
			reportQuery = qg.generateStuckTasksByAgeQuery(queryData);
			break;
		case "gettasksbyday":
			reportQuery = qg.generateCountOfTasksByDayQuery(queryData, true, false);
			break;
		case "gettasksbystatus":
			reportQuery = qg.generateCountOfTasksByStatusQuery(queryData, true);
			break;
		case "gettriagetasks":
			reportQuery = qg.generateCountOfTriageTasksQuery(queryData, true, true);
			break;
		case "gettriagetasksbystatus":
			reportQuery = qg.generateCountOfTriageTasksByStatusQuery(queryData, true);
			break;
		case "getavgtaskagebyname":
			reportQuery = qg.generateAvgTaskAgeingByNameQuery(queryData);
			break;
		case "gettasks":
			reportQuery = qg.generateTasksQuery(queryData);
			break;
		default:
			return "";
		}

		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Collaboration Report Initiated for: "+type);
		String result="";
		result=getJSONResultOnQuery(reportQuery);
		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
			
		}
		return result;

	}

	private String getJSONResultOnQuery(String reportQuery) {
		JSONArray response = new JSONArray();
		if (!reportQuery.isEmpty()) {
			try {
				ResultSet rs = activitiDataSource.getConnection().createStatement().executeQuery(reportQuery);
				ResultSetMetaData rsmd = rs.getMetaData();
				int columnCount = rsmd.getColumnCount();
				while (rs.next()) {
					response.put(utils.asJsonObject(columnCount, rsmd, rs));
				}
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return response.toString();
	}
}
