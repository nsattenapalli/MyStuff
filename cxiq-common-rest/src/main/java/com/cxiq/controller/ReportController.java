package com.cxiq.controller;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cxiq.service.cxiq.CXIQFilterService;
import com.cxiq.service.utils.DBUtils;
import com.cxiq.service.utils.QueryGeneratorService;

@RestController
@RequestMapping(value = "/service")
public class ReportController {

	static final Logger LOGGER = Logger.getLogger(ReportController.class.getName());

	static ConcurrentHashMap<String, List<String>> columnsMap = new ConcurrentHashMap<String, List<String>>();

	@Qualifier("cxiq")
	@Autowired
	DataSource cxiqDataSource;

	@Autowired
	QueryGeneratorService qg;

	@Autowired
	DBUtils dbUtils;

	@Autowired
	CXIQFilterService cxiqFilter;

	@PreAuthorize("hasAnyAuthority('manager','admin')")
	@RequestMapping(value = "/reports/collaboration/tasks/count_initiated", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getCountOfInitiatedTasksForUser(@RequestParam("tenantId") String tenantId,
			@RequestParam("brand") String brand, @RequestParam("processName") String processName,
			@RequestParam("topic") String topic, @RequestParam("priority") String priority,
			@RequestParam("from") String fromDate, @RequestParam("to") String toDate,
			@RequestParam(value = "size", required = false) Long size,
			@RequestParam(value = "start", required = false) Long start) {
		String reportQuery = "";
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Collaboration Reports Initiated Count : fromDate=" + fromDate + ",toDate=" + toDate
					+ ",processName=" + processName + ",topic=" + topic + ",priority=" + priority);
		reportQuery = qg.generateTasksPerUserQuery(tenantId, brand, processName, topic, priority, fromDate, toDate,
				size, start);

		String result = null;
		result = dbUtils.getJSONResultOnQuery(reportQuery, "ACTIVITI");
		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		}
		return result;
	}

	@PreAuthorize("hasAnyAuthority('manager','admin')")
	@RequestMapping(value = "/reports/tasks", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getTasks(@RequestParam("tenantId") String tenantId, @RequestParam("brand") String brand,
			@RequestParam("processName") String processName,
			@RequestParam(value = "taskDescription", required = false) String taskDescription,
			@RequestParam("topic") String topic, @RequestParam("priority") String priority,
			@RequestParam("from") String fromDate, @RequestParam("to") String toDate,
			@RequestParam("startedBy") String startedBy, @RequestParam("assignee") String assignee,
			@RequestParam("status") String status, @RequestParam("groupName") String groupName,
			@RequestParam("isStuck") String isStuck, @RequestParam(value = "size", required = false) Long size,
			@RequestParam(value = "start", required = false) Long start) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Tasks : fromDate=" + fromDate + ",toDate=" + toDate + ",processName=" + processName
					+ ",topic=" + topic + ",priority=" + priority + ",startedBy=" + startedBy + ",assignee=" + assignee
					+ ",status=" + status + ",groupName=" + groupName);
		String reportQuery = "";
		reportQuery = qg.generateTasksQuery(tenantId, brand, processName, topic, taskDescription, priority, fromDate,
				toDate, startedBy, assignee, status, groupName, isStuck, size, start);
		String result = null;
		result = dbUtils.getJSONResultOnQuery(reportQuery, "ACTIVITI");
		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		}
		return result;
	}

	@PreAuthorize("hasAnyAuthority('manager','admin')")
	@RequestMapping(value = "/reports/tasks/{taskId}/journey", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getTaskJourney(@PathVariable("taskId") String taskId) {
		String result = "";
		result = qg.getTaskJourney(taskId);
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Task Journey : taskId=" + taskId);
		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";

		}
		return result;
	}

	@PreAuthorize("hasAnyAuthority('manager','admin')")
	@RequestMapping(value = "/reports/collaboration/tasks/daywise", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getCountOfTasksByDay(@RequestParam("tenantId") String tenantId, @RequestParam("brand") String brand,
			@RequestParam("processName") String processName, @RequestParam("topic") String topic,
			@RequestParam("priority") String priority, @RequestParam("from") String fromDate,
			@RequestParam("to") String toDate, @RequestParam(value = "size", required = false) Long size,
			@RequestParam(value = "start", required = false) Long start) {
		String reportQuery = "";
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Collaboration Reports - Count of Initiated Tasks Per Day : fromDate=" + fromDate + ",toDate="
					+ toDate + ",processName=" + processName + ",topic=" + topic + ",priority=" + priority);
		reportQuery = qg.generateCountOfTasksByDayQuery(tenantId, brand, processName, topic, priority, fromDate, toDate,
				true, false, size, start);

		String result = null;
		result = dbUtils.getJSONResultOnQuery(reportQuery, "ACTIVITI");
		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		}
		return result;
	}

	@PreAuthorize("hasAnyAuthority('manager','admin')")
	@RequestMapping(value = "/reports/collaboration/tasks/count_topic", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getCountOfTasksByTaskName(@RequestParam("tenantId") String tenantId,
			@RequestParam("brand") String brand, @RequestParam("processName") String processName,
			@RequestParam("topic") String topic, @RequestParam("priority") String priority,
			@RequestParam("from") String fromDate, @RequestParam("to") String toDate,
			@RequestParam(value = "size", required = false) Long size,
			@RequestParam(value = "start", required = false) Long start) {
		String reportQuery = "";
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Collaboration Reports - Count Of Tasks By Task Name : fromDate=" + fromDate + ",toDate="
					+ toDate + ",processName=" + processName + ",topic=" + topic + ",priority=" + priority);
		reportQuery = qg.generateCountOfTasksByTaskNameQuery(tenantId, brand, processName, topic, priority, fromDate,
				toDate, size, start);

		String result = null;
		result = dbUtils.getJSONResultOnQuery(reportQuery, "ACTIVITI");
		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		}
		return result;
	}

	@PreAuthorize("hasAnyAuthority('manager','admin')")
	@RequestMapping(value = "/reports/collaboration/tasks/count_assigned", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getCountOfOpenTasksByUser(@RequestParam("tenantId") String tenantId,
			@RequestParam("brand") String brand, @RequestParam("processName") String processName,
			@RequestParam("topic") String topic, @RequestParam("priority") String priority,
			@RequestParam("from") String fromDate, @RequestParam("to") String toDate,
			@RequestParam(value = "size", required = false) Long size,
			@RequestParam(value = "start", required = false) Long start) {
		String reportQuery = "";
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Collaboration Reports - Count Of Assigned Tasks By User : fromDate=" + fromDate + ",toDate="
					+ toDate + ",processName=" + processName + ",topic=" + topic + ",priority=" + priority);
		reportQuery = qg.generateCountOfOpenTasksByUserQuery(tenantId, brand, processName, topic, priority, fromDate,
				toDate, size, start);

		String result = null;
		result = dbUtils.getJSONResultOnQuery(reportQuery, "ACTIVITI");
		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		}
		return result;
	}

	@PreAuthorize("hasAnyAuthority('manager','admin')")
	@RequestMapping(value = "/reports/collaboration/tasks/count_status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getCountOfTasksByStatus(@RequestParam("tenantId") String tenantId,
			@RequestParam("brand") String brand, @RequestParam("processName") String processName,
			@RequestParam("topic") String topic, @RequestParam("priority") String priority,
			@RequestParam("from") String fromDate, @RequestParam("to") String toDate,
			@RequestParam(value = "size", required = false) Long size,
			@RequestParam(value = "start", required = false) Long start) {
		String reportQuery = "";
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Collaboration Reports - Count Of Tasks By Status : fromDate=" + fromDate + ",toDate=" + toDate
					+ ",processName=" + processName + ",topic=" + topic + ",priority=" + priority);
		reportQuery = qg.generateCountOfTasksByStatusQuery(tenantId, brand, processName, topic, priority, fromDate,
				toDate, false, size, start);

		String result = null;
		result = dbUtils.getJSONResultOnQuery(reportQuery, "ACTIVITI");
		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		}
		return result;
	}

	@PreAuthorize("hasAnyAuthority('manager','admin')")
	@RequestMapping(value = "/reports/collaboration/tasks/avg_age", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getAvgTaskAgingByPriority(@RequestParam("tenantId") String tenantId,
			@RequestParam("brand") String brand, @RequestParam("processName") String processName,
			@RequestParam("topic") String topic, @RequestParam("priority") String priority,
			@RequestParam("from") String fromDate, @RequestParam("to") String toDate,
			@RequestParam(value = "size", required = false) Long size,
			@RequestParam(value = "start", required = false) Long start) {
		String reportQuery = "";
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Collaboration Reports - Avg Task Ageing By Priority : fromDate=" + fromDate + ",toDate="
					+ toDate + ",processName=" + processName + ",topic=" + topic + ",priority=" + priority);
		reportQuery = qg.generateAvgTaskAgingByPriorityQuery(tenantId, brand, processName, topic, priority, fromDate,
				toDate, size, start);

		String result = null;
		result = dbUtils.getJSONResultOnQuery(reportQuery, "ACTIVITI");
		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		}
		return result;
	}

	@PreAuthorize("hasAnyAuthority('manager','admin')")
	@RequestMapping(value = "/reports/collaboration/tasks/stuck", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getStuckTasksByAge(@RequestParam("tenantId") String tenantId, @RequestParam("brand") String brand,
			@RequestParam("processName") String processName, @RequestParam("topic") String topic,
			@RequestParam("priority") String priority, @RequestParam("from") String fromDate,
			@RequestParam("to") String toDate, @RequestParam(value = "size", required = false) Long size,
			@RequestParam(value = "start", required = false) Long start) {
		String reportQuery = "";
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Collaboration Reports - Stuck Tasks By Age : fromDate=" + fromDate + ",toDate=" + toDate
					+ ",processName=" + processName + ",topic=" + topic + ",priority=" + priority);
		reportQuery = qg.generateStuckTasksByAgeQuery(tenantId, brand, processName, topic, priority, fromDate, toDate,
				size, start);

		String result = null;
		result = dbUtils.getJSONResultOnQuery(reportQuery, "ACTIVITI");
		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		}
		return result;
	}

	@PreAuthorize("hasAnyAuthority('manager','admin')")
	@RequestMapping(value = "/reports/triage/tasks/daywise", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getCountOfTraigeTasks(@RequestParam("tenantId") String tenantId, @RequestParam("brand") String brand,
			@RequestParam("processName") String processName, @RequestParam("topic") String topic,
			@RequestParam("priority") String priority, @RequestParam("from") String fromDate,
			@RequestParam("to") String toDate, @RequestParam(value = "size", required = false) Long size,
			@RequestParam(value = "start", required = false) Long start) {
		String reportQuery = "";
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Triage Reports - Count of Tasks : fromDate=" + fromDate + ",toDate=" + toDate
					+ ",processName=" + processName + ",topic=" + topic + ",priority=" + priority);
		reportQuery = qg.generateCountOfTriageTasksQuery(tenantId, brand, processName, topic, priority, fromDate,
				toDate, true, true, size, start);

		String result = null;
		result = dbUtils.getJSONResultOnQuery(reportQuery, "ACTIVITI");
		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		}
		return result;
	}

	@PreAuthorize("hasAnyAuthority('manager','admin')")
	@RequestMapping(value = "/reports/triage/tasks/count_channel", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getCountOfTriageTasksByChannel(@RequestParam("tenantId") String tenantId,
			@RequestParam("brand") String brand, @RequestParam("processName") String processName,
			@RequestParam("topic") String topic, @RequestParam("priority") String priority,
			@RequestParam("from") String fromDate, @RequestParam("to") String toDate,
			@RequestParam(value = "size", required = false) Long size,
			@RequestParam(value = "start", required = false) Long start) {
		String reportQuery = "";
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Triage Reports - Count of Tasks By Channel : fromDate=" + fromDate + ",toDate=" + toDate
					+ ",processName=" + processName + ",topic=" + topic + ",priority=" + priority);
		reportQuery = qg.generateCountOfTriageTasksByStatusQuery(tenantId, brand, processName, topic, priority,
				fromDate, toDate, true, size, start);

		String result = null;
		result = dbUtils.getJSONResultOnQuery(reportQuery, "ACTIVITI");
		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		}
		return result;

	}

	@PreAuthorize("hasAnyAuthority('manager','admin')")
	@RequestMapping(value = "/reports/triage/tasks/count_status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getCountOfTriageTasksByStatus(@RequestParam("tenantId") String tenantId,
			@RequestParam("brand") String brand, @RequestParam("processName") String processName,
			@RequestParam("topic") String topic, @RequestParam("priority") String priority,
			@RequestParam("from") String fromDate, @RequestParam("to") String toDate,
			@RequestParam(value = "size", required = false) Long size,
			@RequestParam(value = "start", required = false) Long start) {
		String reportQuery = "";
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Triage Reports - Count of Tasks : fromDate=" + fromDate + ",toDate=" + toDate
					+ ",processName=" + processName + ",topic=" + topic + ",priority=" + priority);

		reportQuery = qg.generateCountOfTriageTasksByStatusQuery(tenantId, brand, processName, topic, priority,
				fromDate, toDate, true, size, start);

		String result = null;
		result = dbUtils.getJSONResultOnQuery(reportQuery, "ACTIVITI");
		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		}
		return result;

	}

	@PreAuthorize("hasAnyAuthority('manager','admin')")
	@RequestMapping(value = "/reports/triage/tasks/avg_age", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getAvgTaskAgeingByChannel(@RequestParam("tenantId") String tenantId,
			@RequestParam("brand") String brand, @RequestParam("processName") String processName,
			@RequestParam("topic") String topic, @RequestParam("priority") String priority,
			@RequestParam("from") String fromDate, @RequestParam("to") String toDate,
			@RequestParam(value = "size", required = false) Long size,
			@RequestParam(value = "start", required = false) Long start) {
		String reportQuery = "";
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Triage Reports - Avg Task Ageing By Channel : fromDate=" + fromDate + ",toDate=" + toDate
					+ ",processName=" + processName + ",topic=" + topic + ",priority=" + priority);

		reportQuery = qg.generateAvgTaskAgeingByNameQuery(tenantId, brand, processName, topic, priority, fromDate,
				toDate, size, start);

		String result = null;
		result = dbUtils.getJSONResultOnQuery(reportQuery, "ACTIVITI");
		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		}
		return result;

	}

	@PreAuthorize("hasAnyAuthority('manager','admin')")
	@RequestMapping(value = "/reports/triage/task", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getTriageTasksByContentID(@RequestParam("contentId") String contentId) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Triage Reports - Task by Content ID : Content ID= " + contentId);
		String reportQuery = "";
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Triage Reports - Triage Task  By Content : contentID=" + contentId);

		reportQuery = qg.generateTriageTasksByContentIDQuery(contentId);

		String result = null;
		result = dbUtils.getJSONResultOnQuery(reportQuery, "ACTIVITI");
		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		}
		return result;

	}

	@PreAuthorize("hasAnyAuthority('manager','admin')")
	@RequestMapping(value = "/reports/summaryperformance/daywise", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getTopicVolumePerDay(@RequestParam("from") String fromDate, @RequestParam("to") String toDate,
			@RequestParam("tenantId") String tenantId, @RequestParam("brand") String brand,
			@RequestParam("topic") String topic, @RequestBody(required = false) String filter) {

		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Summary Performance Reports - Topic Volume Per Day : fromDate=" + fromDate + ",toDate="
					+ toDate + ",topic=" + topic);
		String npsTable = dbUtils.getCXIQTable(tenantId, brand);

		// PREPARE query for fetching topics in descending order of given
		// filter
		String npsScoreQuery = "SELECT ";
		String topicPrefix = "topic~";
		String result = null;
		if ("all".equalsIgnoreCase(topic)) {
			npsScoreQuery += " SUM(CASE WHEN sourcedata_nps_score > 8 THEN 1 ELSE 0 END) AS \"positive\" , "
					+ " SUM(CASE WHEN sourcedata_nps_score = 7 or sourcedata_nps_score = 8 THEN 1 ELSE 0 END) AS \"neutral\" , "
					+ " SUM(CASE WHEN sourcedata_nps_score < 7 THEN 1 ELSE 0 END) AS \"negative\" ";
		} else {

			npsScoreQuery += " SUM(CASE WHEN sourcedata_nps_score > 8 and " + topicPrefix + topic
					+ " !=0  THEN 1 ELSE 0 END) AS \"positive\" , "
					+ " SUM(CASE WHEN sourcedata_nps_score = 7 or sourcedata_nps_score = 8 and " + topicPrefix + topic
					+ " !=0  THEN 1 ELSE 0 END) AS \"neutral\" , " + " SUM(CASE WHEN sourcedata_nps_score < 7 and "
					+ topicPrefix + topic + " !=0  THEN 1 ELSE 0 END) AS \"negative\" ";

		}
		npsScoreQuery += " ,content_published_date from " + npsTable + " where 1=1 ";
		if ((fromDate != null) && (toDate != null) && (!toDate.equals("")) && (!fromDate.equals(""))) {
			npsScoreQuery = npsScoreQuery + " and content_published_date between '" + fromDate + "' and '" + toDate
					+ "' ";
		}
		if (filter != null && !filter.equals("")) {
			String hierarchyFilter = cxiqFilter.getHierarchyFilter(filter);
			if (hierarchyFilter != null && !"".equalsIgnoreCase(hierarchyFilter)) {
				npsScoreQuery += "and " + hierarchyFilter;

			}
			String categoryFilter = cxiqFilter.getCategoryFilter(filter);
			if (categoryFilter != null && !"".equalsIgnoreCase(categoryFilter)) {
				npsScoreQuery += "and " + categoryFilter;
			}
		}
		npsScoreQuery = npsScoreQuery + " group by content_published_date  order by  content_published_date asc ";
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Summary performance topics by daywise \n " + npsScoreQuery);
		}
		result = dbUtils.getJSONResultOnQuery(npsScoreQuery, "CXIQ");

		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		}
		return result;
	}

	@PreAuthorize("hasAnyAuthority('manager','admin')")
	@RequestMapping(value = "/reports/summaryperformance/verbatims", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getVarbatims(@RequestParam("topic") String topic, @RequestParam("from") String fromDate,
			@RequestParam("to") String toDate, @RequestParam("tenantId") String tenantId,
			@RequestParam("brand") String brand, @RequestParam("type") String type, @RequestParam("limit") String limit,
			@RequestParam("item") String item, @RequestParam("itemValue") String itemValue,
			@RequestBody(required = false) String filter) {

		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Summary Performance Reports - Verbatims : fromDate=" + fromDate + ",toDate=" + toDate
					+ ",type=" + type + ",limit=" + limit);
		String npsTable = dbUtils.getCXIQTable(tenantId, brand);
		String result = null, query = "";
		query = query + "select distinct decooda_content_id, ";
		if (item != null && !"".equalsIgnoreCase(item))
			query = query + " sourcedata_" + item + ",";
		query += " content_published_date,sourcedata_nps_score,sourcedata_author,content, CASE WHEN sourcedata_nps_score > 8 THEN 'Promoter' WHEN sourcedata_nps_score < 7 THEN 'Detractor'ELSE 'Passive' END AS emotion from "
				+ npsTable + " where 1=1 ";
		if (item != null && !"".equalsIgnoreCase(item) && itemValue != null && !"".equalsIgnoreCase(itemValue)) {
			query = query + " and sourcedata_" + item + " = '" + itemValue + "'";

		}
		if ("positive".equalsIgnoreCase(type)) {
			query = query + " and sourcedata_nps_score > 8 ";
		} else if ("negative".equalsIgnoreCase(type)) {
			query = query + " and sourcedata_nps_score < 7 ";
		} else if ("neutral".equalsIgnoreCase(type)) {
			query = query + " and (sourcedata_nps_score = 7 or sourcedata_nps_score = 8) ";
		}

		if ((fromDate != null) && (toDate != null) && (!toDate.equals("")) && (!fromDate.equals(""))) {
			query = query + " and content_published_date between '" + fromDate + "' and '" + toDate + "' ";
		}
		if (filter != null && !filter.equals("")) {
			String hierarchyFilter = cxiqFilter.getHierarchyFilter(filter);
			if (hierarchyFilter != null && !"".equalsIgnoreCase(hierarchyFilter)) {
				query += "and " + hierarchyFilter;

			}
			String categoryFilter = cxiqFilter.getCategoryFilter(filter);
			if (categoryFilter != null && !"".equalsIgnoreCase(categoryFilter)) {
				query += "and " + categoryFilter;
			}
		}
		if ("positive".equalsIgnoreCase(type) && (topic != null && !"".equalsIgnoreCase(topic)))
			query = query + " order by  sourcedata_" + item + " asc ";
		if ("negative".equalsIgnoreCase(type) && (topic != null && !"".equalsIgnoreCase(topic)))
			query = query + " order by sourcedata_" + item + " desc ";

		if (limit != null && !"".equalsIgnoreCase(limit))
			query = query + " limit " + limit;
		result = dbUtils.getJSONResultOnQuery(query, "ACTIVITI");

		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		}
		return result;

	}

	@PreAuthorize("hasAnyAuthority('manager','admin')")
	@RequestMapping(value = "/reports/summaryperformance", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getSummaryPerformance(@RequestParam("from") String fromDate, @RequestParam("to") String toDate,
			@RequestParam(value = "audience", required = false) String audience,
			@RequestParam(value = "filter", required = false) String topicsFilter, @RequestParam("order") String order,
			@RequestParam("limit") String limit, @RequestParam("tenantId") String tenantId,
			@RequestParam("brand") String brand, @RequestBody(required = false) String filter,
			@RequestParam("item") String item) {

		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Summary Performance Report : fromDate=" + fromDate + ",toDate=" + toDate + ",order=" + order
					+ ",limit=" + limit);
		String npsTable = dbUtils.getCXIQTable(tenantId, brand);

		String result = "{TOPIC:0}";
		Connection connection = null;
		try {
			String filterQuery = " where sourcedata_product!='' ";

			if (filter != null && !filter.equals("")) {
				String hierarchyFilter = cxiqFilter.getHierarchyFilter(filter);
				if (hierarchyFilter != null && !"".equalsIgnoreCase(hierarchyFilter)) {
					filterQuery += "and " + hierarchyFilter;

				}
				String categoryFilter = cxiqFilter.getCategoryFilter(filter);
				if (categoryFilter != null && !"".equalsIgnoreCase(categoryFilter)) {
					filterQuery += "and " + categoryFilter;
				}
			}
			if (fromDate == null || toDate == null || fromDate.equals("") || toDate.equals("")) {
				filterQuery = filterQuery + "";
			} else {
				filterQuery += ((filterQuery.equals("")) ? " WHERE " : " AND ") + " ( content_published_date between '"
						+ fromDate + "' and '" + toDate + "') ";
			}

			if (audience != null && !audience.equals("")) {
				String audienceFilter = cxiqFilter.getAudienceFilter(audience);
				if (audienceFilter != null) {
					filterQuery += ((filterQuery.equals("")) ? " WHERE " : " AND ") + audienceFilter;
				}
			}

			if (topicsFilter != null && !topicsFilter.equals("")) {
				List<String> columns = getColumns(npsTable);

				String topicsQueryFilter = cxiqFilter.getTopicsQueryFilter(columns, topicsFilter);
				if (topicsQueryFilter != null) {
					filterQuery += ((filterQuery.equals("")) ? " WHERE " : " AND ") + topicsQueryFilter;
				}

			}

			String sortOrder = "desc";
			if ("asc".equalsIgnoreCase(order))
				sortOrder = "asc";

			String itemQuery = "select sourcedata_" + item
					+ ",ROUND(((positive/cnt)-(negative/cnt))*100,2) as NPS from ( " + " select sourcedata_" + item
					+ ",SUM(CASE WHEN sourcedata_nps_score > 8  THEN 1 ELSE 0 END) AS positive, "
					+ " SUM(CASE WHEN sourcedata_nps_score = 7 or sourcedata_nps_score = 8  THEN 1 ELSE 0 END) AS neutral, "
					+ " SUM(CASE WHEN sourcedata_nps_score < 7  THEN 1 ELSE 0 END) AS negative,count(1) as cnt ";

			itemQuery += " FROM " + npsTable + filterQuery + " group by sourcedata_" + item
					+ ") as hotels order by NPS " + sortOrder;

			if (limit != null && !"".equalsIgnoreCase(limit))
				itemQuery += " limit " + limit;
			connection = cxiqDataSource.getConnection();
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery(itemQuery);
			JSONArray jsonArray = new JSONArray();
			while (rs.next()) {
				String itemVal = rs.getString("sourcedata_" + item + "");
				String npsPercent = rs.getString("NPS");
				String itemFilter = " and sourcedata_" + item + "='" + itemVal + "' ";
				itemFilter = filterQuery + itemFilter;
				String hotelQuery = " select sourcedata_" + item
						+ ",content_published_date,SUM(CASE WHEN sourcedata_nps_score > 8  THEN 1 ELSE 0 END) AS positive, "
						+ " SUM(CASE WHEN sourcedata_nps_score = 7 or sourcedata_nps_score = 8  THEN 1 ELSE 0 END) AS neutral, "
						+ " SUM(CASE WHEN sourcedata_nps_score < 7  THEN 1 ELSE 0 END) AS negative,count(1) as cnt "
						+ " from " + npsTable + itemFilter + " group by sourcedata_" + item + ",content_published_date "
						+ "  order by content_published_date asc ";
				Statement stmt2 = connection.createStatement();
				ResultSet rs2 = stmt2.executeQuery(hotelQuery);
				while (rs2.next()) {
					JSONObject jsonObj = new JSONObject();

					jsonObj.put("id", itemVal);
					jsonObj.put("nps_score", npsPercent);
					jsonObj.put("day", rs2.getString("content_published_date"));
					jsonObj.put("count_promoters", rs2.getString("positive"));
					jsonObj.put("count_passive", rs2.getString("neutral"));
					jsonObj.put("count_detractors", rs2.getString("negative"));

					jsonArray.put(jsonObj);
				}
				rs2.close();

			}
			rs.close();

			stmt.close();
			result = jsonArray.toString();

		} catch (Exception e) {
			LOGGER.error(e);
			return null;
		} finally {
			if (connection != null)
				try {
					connection.close();
				} catch (SQLException e) {
					LOGGER.error(e);
				}
		}

		return result;
	}

	private List<String> getColumns(String tableName) {
		List<String> columnList = columnsMap.get(tableName);

		if (columnList != null)
			return columnList;
		columnList = new ArrayList<String>();
		Connection connection = null;
		try {
			connection = cxiqDataSource.getConnection();
			Statement stamt = connection.createStatement();
			ResultSet rs = stamt.executeQuery("SELECT * FROM " + tableName + " where 1=0");
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnCount = rsmd.getColumnCount();
			for (int i = 1; i <= columnCount; i++) {
				columnList.add(rsmd.getColumnName(i));
			}
			rs.close();
		} catch (SQLException sqle) {
			LOGGER.error(sqle);
		} finally {
			if (connection != null)
				try {
					connection.close();
				} catch (SQLException e) {
					LOGGER.error(e);
				}
		}

		columnsMap.putIfAbsent(tableName, columnList);
		return columnList;
	}

}
