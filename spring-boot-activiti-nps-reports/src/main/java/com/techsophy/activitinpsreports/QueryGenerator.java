package com.techsophy.activitinpsreports;

import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.springframework.stereotype.Service;

@Service
public class QueryGenerator {
	
	final Logger LOGGER = Logger.getLogger(QueryGenerator.class.getName());

	public String generateTasksPerUserQuery(Map<String, String> queryData) {
		String query = " select ari.user_id_ user_id,count(1) as count_of_tasks from ACT_HI_TASKINST art,ACT_HI_IDENTITYLINK ari  ";

		if (queryData.get("brand") != null && !"".equalsIgnoreCase(queryData.get("brand"))) {
			query = query + " ,ACT_HI_VARINST ahv where ahv.proc_inst_id_= art.proc_inst_id_ and ahv.text_='"
					+ queryData.get("brand") + "'";
		} else {
			query = query + " where 1=1 ";
		}
		query = query + " and art.proc_inst_id_=ari.proc_inst_id_ and ari.type_='starter' ";
		if (queryData.get("tenantId") != null && !"".equalsIgnoreCase(queryData.get("tenantId"))) {
			query = query + " and art.tenant_id_='" + queryData.get("tenantId") + "'";
		}
		if (queryData.get("processName") != null && !"".equalsIgnoreCase(queryData.get("processName"))) {
			if (queryData.get("tenantId") != null && !"".equalsIgnoreCase(queryData.get("tenantId"))) {
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ queryData.get("processName") + "' and arp.tenant_id_='" + queryData.get("tenantId") + "' ) ";
			} else
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ queryData.get("processName") + "' ) ";
		}
		if (queryData.get("taskName") != null && !"".equalsIgnoreCase(queryData.get("taskName"))) {
			query = query + " and art.name_ like '%" + queryData.get("taskName") + "'";
		}
		if (queryData.get("priority") != null && !"".equalsIgnoreCase(queryData.get("priority"))) {
			query = query + " and art.priority_ = '" + queryData.get("priority") + "'";
		}
		if ((queryData.get("fromDate") != null) && !"".equalsIgnoreCase(queryData.get("fromDate"))
				&& (queryData.get("toDate") != null) && !"".equalsIgnoreCase(queryData.get("toDate"))) {
			query = query + " and art.start_time_ between '" + queryData.get("fromDate") + "' and '"
					+ queryData.get("toDate") + "' ";
		}
		query = query + " group by ari.user_id_ order by count_of_tasks desc ";
		if (queryData.get("size") != null && !"".equalsIgnoreCase(queryData.get("size")))
			if (Integer.parseInt(queryData.get("size")) > 0)
				query = query + " LIMIT " + Integer.parseInt(queryData.get("size"));
		if (queryData.get("start") != null && !"".equalsIgnoreCase(queryData.get("start")))
			if (Integer.parseInt(queryData.get("start")) > 0)
				query = query + " OFFSET " + Integer.parseInt(queryData.get("start"));
		return query;
	}

	public String generateCountOfTasksByTaskNameQuery(Map<String, String> queryData) {

		String query = " select substring(art.name_,position('on' IN art.name_)+6) task_name,count(1) as count_of_tasks from ACT_HI_TASKINST art ";

		if (queryData.get("brand") != null && !"".equalsIgnoreCase(queryData.get("brand"))) {
			query = query + " ,ACT_HI_VARINST ahv where ahv.proc_inst_id_= art.proc_inst_id_ and ahv.text_='"
					+ queryData.get("brand") + "'";
		} else {
			query = query + " where 1=1 ";
		}
		if (queryData.get("tenantId") != null && !"".equalsIgnoreCase(queryData.get("tenantId"))) {
			query = query + " and art.tenant_id_='" + queryData.get("tenantId") + "'";
		}
		if (queryData.get("processName") != null && !"".equalsIgnoreCase(queryData.get("processName"))) {
			if (queryData.get("tenantId") != null && !"".equalsIgnoreCase(queryData.get("tenantId"))) {
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ queryData.get("processName") + "' and arp.tenant_id_='" + queryData.get("tenantId") + "' ) ";
			} else
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ queryData.get("processName") + "' ) ";
		}
		if (queryData.get("taskName") != null && !"".equalsIgnoreCase(queryData.get("taskName"))) {
			query = query + " and art.name_ like '%" + queryData.get("taskName") + "'";
		}
		if (queryData.get("priority") != null && !"".equalsIgnoreCase(queryData.get("priority"))) {
			query = query + " and art.priority_ = '" + queryData.get("priority") + "'";
		}
		if ((queryData.get("fromDate") != null) && !"".equalsIgnoreCase(queryData.get("fromDate"))
				&& (queryData.get("toDate") != null) && !"".equalsIgnoreCase(queryData.get("toDate"))) {
			query = query + " and art.start_time_ between '" + queryData.get("fromDate") + "' and '"
					+ queryData.get("toDate") + "' ";
		}
		query = query + " group by task_name order by count_of_tasks desc";
		if (queryData.get("size") != null && !"".equalsIgnoreCase(queryData.get("size")))
			if (Integer.parseInt(queryData.get("size")) > 0)
				query = query + " LIMIT " + Integer.parseInt(queryData.get("size"));
		if (queryData.get("start") != null && !"".equalsIgnoreCase(queryData.get("start")))
			if (Integer.parseInt(queryData.get("start")) > 0)
				query = query + " OFFSET " + Integer.parseInt(queryData.get("start"));
		return query;
	}

	public String generateCountOfOpenTasksByUserQuery(Map<String, String> queryData) {

		String query = " select art.assignee_ as user_id,count(1) as count_of_tasks_per_topic from ACT_HI_TASKINST art  ";
		if (queryData.get("brand") != null && !"".equalsIgnoreCase(queryData.get("brand"))) {
			query = query + " ,ACT_HI_VARINST ahv where ahv.proc_inst_id_= art.proc_inst_id_ and ahv.text_='"
					+ queryData.get("brand") + "' and art.delete_reason_ IS NULL ";
		} else {
			query = query + " where 1=1 and art.delete_reason_ IS NULL ";
		}
		if (queryData.get("tenantId") != null && !"".equalsIgnoreCase(queryData.get("tenantId"))) {
			query = query + " and art.tenant_id_='" + queryData.get("tenantId") + "'";
		}
		if (queryData.get("processName") != null && !"".equalsIgnoreCase(queryData.get("processName"))) {
			if (queryData.get("tenantId") != null && !"".equalsIgnoreCase(queryData.get("tenantId"))) {
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ queryData.get("processName") + "' and arp.tenant_id_='" + queryData.get("tenantId") + "' ) ";
			} else
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ queryData.get("processName") + "' ) ";
		}
		if (queryData.get("taskName") != null && !"".equalsIgnoreCase(queryData.get("taskName"))) {
			query = query + " and art.name_ like '%" + queryData.get("taskName") + "'";
		}
		if (queryData.get("priority") != null && !"".equalsIgnoreCase(queryData.get("priority"))) {
			query = query + " and art.priority_ = '" + queryData.get("priority") + "'";
		}
		if ((queryData.get("fromDate") != null) && !"".equalsIgnoreCase(queryData.get("fromDate"))
				&& (queryData.get("toDate") != null) && !"".equalsIgnoreCase(queryData.get("toDate"))) {
			query = query + " and art.start_time_ between '" + queryData.get("fromDate") + "' and '"
					+ queryData.get("toDate") + "' ";
		}
		query = query + " group by art.assignee_ order by count_of_tasks_per_topic desc ";
		if (queryData.get("size") != null && !"".equalsIgnoreCase(queryData.get("size")))
			if (Integer.parseInt(queryData.get("size")) > 0)
				query = query + " LIMIT " + Integer.parseInt(queryData.get("size"));
		if (queryData.get("start") != null && !"".equalsIgnoreCase(queryData.get("start")))
			if (Integer.parseInt(queryData.get("start")) > 0)
				query = query + " OFFSET " + Integer.parseInt(queryData.get("start"));
		return query;
	}

	/**
	 * Returns the Average Task Aging by Priority
	 * 
	 * @param tenantId
	 *            - Id of the tenant/organization (optional)
	 * @param processName
	 *            - Name of the process (optional)
	 * @param priority
	 *            - Priority Value (optional)
	 * @param fromDate
	 *            - From Date (optional)
	 * @param toDate
	 *            - To Date (optional)
	 * @return result of the query - {@link JSONArray} in {@link String} format
	 */
	public String generateAvgTaskAgingByPriorityQuery(Map<String, String> queryData) {

		String query = " select TaskAgeing.priority as priority,ROUND(CONVERT(AVG(TaskAgeing.days), UNSIGNED INTEGER),2) as avg_age from ("
				+ " select art.priority_ as priority, " + " CASE WHEN art.delete_reason_='completed' THEN "
				+ " DATEDIFF( art.end_time_, art.start_time_) " + " WHEN art.delete_reason_ IS NULL THEN  "
				+ " DATEDIFF( current_timestamp, art.start_time_) " + " ELSE 0 END "
				+ " as days from ACT_HI_TASKINST art ";

		if (queryData.get("brand") != null && !"".equalsIgnoreCase(queryData.get("brand"))) {
			query = query + " ,ACT_HI_VARINST ahv where ahv.proc_inst_id_= art.proc_inst_id_ and ahv.text_='"
					+ queryData.get("brand") + "'";
		} else {
			query = query + " where 1=1 ";
		}
		if (queryData.get("tenantId") != null && !"".equalsIgnoreCase(queryData.get("tenantId"))) {
			query = query + " and art.tenant_id_='" + queryData.get("tenantId") + "'";
		}
		if (queryData.get("processName") != null && !"".equalsIgnoreCase(queryData.get("processName"))) {
			if (queryData.get("tenantId") != null && !"".equalsIgnoreCase(queryData.get("tenantId"))) {
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ queryData.get("processName") + "' and arp.tenant_id_='" + queryData.get("tenantId") + "' ) ";
			} else
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ queryData.get("processName") + "' ) ";
		}
		if (queryData.get("taskName") != null && !"".equalsIgnoreCase(queryData.get("taskName"))) {
			query = query + " and art.name_ like '%" + queryData.get("taskName") + "'";
		}
		if (queryData.get("priority") != null && !"".equalsIgnoreCase(queryData.get("priority"))) {
			query = query + " and art.priority_ = '" + queryData.get("priority") + "'";
		}
		if ((queryData.get("fromDate") != null) && !"".equalsIgnoreCase(queryData.get("fromDate"))
				&& (queryData.get("toDate") != null) && !"".equalsIgnoreCase(queryData.get("toDate"))) {
			query = query + " and art.start_time_ between '" + queryData.get("fromDate") + "' and '"
					+ queryData.get("toDate") + "' ";
		}
		query = query + " ) as TaskAgeing group by priority order by priority desc ";
		if (queryData.get("size") != null && !"".equalsIgnoreCase(queryData.get("size")))
			if (Integer.parseInt(queryData.get("size")) > 0)
				query = query + " LIMIT " + Integer.parseInt(queryData.get("size"));
		if (queryData.get("start") != null && !"".equalsIgnoreCase(queryData.get("start")))
			if (Integer.parseInt(queryData.get("start")) > 0)
				query = query + " OFFSET " + Integer.parseInt(queryData.get("start"));
		return query;
	}

	/**
	 * Returns the Stuck Tasks by Age (in days)
	 * 
	 * @param tenantId
	 *            - Id of the tenant/organization (optional)
	 * @param processName
	 *            - Name of the process (optional)
	 * @param taskName
	 *            - Name of the Task (optional)
	 * @param priority
	 *            - Priority Value (optional)
	 * @param fromDate
	 *            - From Date (optional)
	 * @param toDate
	 *            - To Date (optional)
	 * @return result of the query - {@link JSONArray} in {@link String} format
	 */
	public String generateStuckTasksByAgeQuery(Map<String, String> queryData) {

		String query = " select * from ( select ari.user_id_ user_id,art.id_ as task_id,  DATEDIFF( current_timestamp, art.start_time_)  "
				+ "  as age from ACT_HI_TASKINST art,ACT_HI_IDENTITYLINK ari ";

		if (queryData.get("brand") != null && !"".equalsIgnoreCase(queryData.get("brand"))) {
			query = query + " ,ACT_HI_VARINST ahv where ahv.proc_inst_id_= art.proc_inst_id_ and ahv.text_='"
					+ queryData.get("brand") + "'";
		} else {
			query = query + " where 1=1 ";
		}
		query = query
				+ " and art.proc_inst_id_=ari.proc_inst_id_ and ari.type_='starter' and art.delete_reason_ IS NULL ";
		if (queryData.get("tenantId") != null && !"".equalsIgnoreCase(queryData.get("tenantId"))) {
			query = query + " and art.tenant_id_='" + queryData.get("tenantId") + "'";
		}
		if (queryData.get("processName") != null && !"".equalsIgnoreCase(queryData.get("processName"))) {
			if (queryData.get("tenantId") != null && !"".equalsIgnoreCase(queryData.get("tenantId"))) {
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ queryData.get("processName") + "' and arp.tenant_id_='" + queryData.get("tenantId") + "' ) ";
			} else
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ queryData.get("processName") + "' ) ";
		}
		if (queryData.get("taskName") != null && !"".equalsIgnoreCase(queryData.get("taskName"))) {
			query = query + " and art.name_ like '%" + queryData.get("taskName") + "'";
		}
		if (queryData.get("priority") != null && !"".equalsIgnoreCase(queryData.get("priority"))) {
			query = query + " and art.priority_ = '" + queryData.get("priority") + "'";
		}
		if ((queryData.get("fromDate") != null) && !"".equalsIgnoreCase(queryData.get("fromDate"))
				&& (queryData.get("toDate") != null) && !"".equalsIgnoreCase(queryData.get("toDate"))) {
			query = query + " and art.start_time_ between '" + queryData.get("fromDate") + "' and '"
					+ queryData.get("toDate") + "' ";
		}
		query = query + " ) as z where z.age>0 order by z.age desc";
		if (queryData.get("size") != null && !"".equalsIgnoreCase(queryData.get("size")))
			if (Integer.parseInt(queryData.get("size")) > 0)
				query = query + " LIMIT " + Integer.parseInt(queryData.get("size"));
		if (queryData.get("start") != null && !"".equalsIgnoreCase(queryData.get("start")))
			if (Integer.parseInt(queryData.get("start")) > 0)
				query = query + " OFFSET " + Integer.parseInt(queryData.get("start"));
		return query;
	}

	/**
	 * Returns the Count of Tasks by Day
	 * 
	 * @param tenantId
	 *            - Id of the tenant/organization (optional)
	 * @param processName
	 *            - Name of the process (optional)
	 * @param taskName
	 *            - Name of the Task (optional)
	 * @param priority
	 *            - Priority Value (optional)
	 * @param fromDate
	 *            - From Date (optional)
	 * @param toDate
	 *            - To Date (optional)
	 * @return result of the query - {@link JSONArray} in {@link String} format
	 */
	public String generateCountOfTasksByDayQuery(Map<String, String> queryData, boolean dateRequired, boolean nameRequired) {

		String query = " select  ";
		if (dateRequired) {
			query = query + " date_format(art.start_time_,'%Y-%m-%d') as day, ";
		}
		if (nameRequired) {
			query = query + " substring(art.name_, 0,position(' ' IN art.name_ )) as name, ";
		}

		query = query + " count(1) as count_of_tasks from ACT_HI_TASKINST art  ";
		if (queryData.get("brand") != null && !"".equalsIgnoreCase(queryData.get("brand"))) {
			query = query + " ,ACT_HI_VARINST ahv where ahv.proc_inst_id_= art.proc_inst_id_ and ahv.text_='"
					+ queryData.get("brand") + "'";
		} else {
			query = query + " where 1=1 ";
		}
		if (queryData.get("tenantId") != null && !"".equalsIgnoreCase(queryData.get("tenantId"))) {
			query = query + " and art.tenant_id_='" + queryData.get("tenantId") + "'";
		}
		if (queryData.get("processName") != null && !"".equalsIgnoreCase(queryData.get("processName"))) {
			if (queryData.get("tenantId") != null && !"".equalsIgnoreCase(queryData.get("tenantId"))) {
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ queryData.get("processName") + "' and arp.tenant_id_='" + queryData.get("tenantId") + "' ) ";
			} else
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ queryData.get("processName") + "' ) ";
		}
		if (queryData.get("taskName") != null && !"".equalsIgnoreCase(queryData.get("taskName"))) {
			query = query + " and art.name_ like '%" + queryData.get("taskName") + "'";
		}
		if (queryData.get("priority") != null && !"".equalsIgnoreCase(queryData.get("priority"))) {
			query = query + " and art.priority_ = '" + queryData.get("priority") + "'";
		}
		if ((queryData.get("fromDate") != null) && !"".equalsIgnoreCase(queryData.get("fromDate"))
				&& (queryData.get("toDate") != null) && !"".equalsIgnoreCase(queryData.get("toDate"))) {
			query = query + " and art.start_time_ between '" + queryData.get("fromDate") + "' and '"
					+ queryData.get("toDate") + "' ";
		}
		if (!nameRequired && dateRequired) {
			query = query + " group by day order by day asc ";
		}
		if (nameRequired && dateRequired) {
			query = query + " group by day,name order by day asc, name asc ";
		}
		if (nameRequired && !dateRequired) {
			query = query + " group by name  order by name asc ";
		}
		if (queryData.get("size") != null && !"".equalsIgnoreCase(queryData.get("size")))
			if (Integer.parseInt(queryData.get("size")) > 0)
				query = query + " LIMIT " + Integer.parseInt(queryData.get("size"));
		if (queryData.get("start") != null && !"".equalsIgnoreCase(queryData.get("start")))
			if (Integer.parseInt(queryData.get("start")) > 0)
				query = query + " OFFSET " + Integer.parseInt(queryData.get("start"));
		return query;
	}

	/**
	 * Returns the Count of Tasks by Status (Open & Closed)
	 * 
	 * @param tenantId
	 *            - Id of the tenant/organization (optional)
	 * @param processName
	 *            - Name of the process (optional)
	 * @param taskName
	 *            - Name of the Task (optional)
	 * @param priority
	 *            - Priority Value (optional)
	 * @param fromDate
	 *            - From Date (optional)
	 * @param toDate
	 *            - To Date (optional)
	 * @param nameRequired
	 *            - if 'true' returns count of tasks by name and status else returns
	 *            by status (optional)
	 * @return result of the query - {@link JSONArray} in {@link String} format
	 */
	public String generateCountOfTasksByStatusQuery(Map<String, String> queryData, boolean nameRequired) {

		String query = " select ";
		if (nameRequired) {
			query = query + " art.name_ as name, ";
		}
		query = query + " SUM(CASE WHEN art.delete_reason_ IS NULL THEN 1 ELSE 0 END)  count_of_opentasks, "
				+ " SUM(CASE WHEN art.delete_reason_ ='completed' THEN 1 ELSE 0 END)  count_of_closedtasks ";

		query = query + " from ACT_HI_TASKINST art ";
		if (queryData.get("brand") != null && !"".equalsIgnoreCase(queryData.get("brand"))) {
			query = query + " ,ACT_HI_VARINST ahv where ahv.proc_inst_id_= art.proc_inst_id_ and ahv.text_='"
					+ queryData.get("brand") + "'";
		} else {
			query = query + " where 1=1 ";
		}
		if (queryData.get("tenantId") != null && !"".equalsIgnoreCase(queryData.get("tenantId"))) {
			query = query + " and art.tenant_id_='" + queryData.get("tenantId") + "'";
		}
		if (queryData.get("processName") != null && !"".equalsIgnoreCase(queryData.get("processName"))) {
			if (queryData.get("tenantId") != null && !"".equalsIgnoreCase(queryData.get("tenantId"))) {
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ queryData.get("processName") + "' and arp.tenant_id_='" + queryData.get("tenantId") + "' ) ";
			} else
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ queryData.get("processName") + "' ) ";
		}
		if (queryData.get("taskName") != null && !"".equalsIgnoreCase(queryData.get("taskName"))) {
			query = query + " and art.name_ like '%" + queryData.get("taskName") + "'";
		}
		if (queryData.get("priority") != null && !"".equalsIgnoreCase(queryData.get("priority"))) {
			query = query + " and art.priority_ = '" + queryData.get("priority") + "'";
		}
		if ((queryData.get("fromDate") != null) && !"".equalsIgnoreCase(queryData.get("fromDate"))
				&& (queryData.get("toDate") != null) && !"".equalsIgnoreCase(queryData.get("toDate"))) {
			query = query + " and art.start_time_ between '" + queryData.get("fromDate") + "' and '"
					+ queryData.get("toDate") + "' ";
		}

		if (nameRequired)
			query = query + " group by art.name_ order by art.name_ asc";
		if (queryData.get("size") != null && !"".equalsIgnoreCase(queryData.get("size")))
			if (Integer.parseInt(queryData.get("size")) > 0)
				query = query + " LIMIT " + Integer.parseInt(queryData.get("size"));
		if (queryData.get("start") != null && !"".equalsIgnoreCase(queryData.get("start")))
			if (Integer.parseInt(queryData.get("start")) > 0)
				query = query + " OFFSET " + Integer.parseInt(queryData.get("start"));
		return query;
	}

	/**
	 * Returns the Count of Triage Tasks
	 * 
	 * @param tenantId
	 *            - Id of the tenant/organization (optional)
	 * @param processName
	 *            - Name of the process (optional)
	 * @param taskName
	 *            - Name of the Task (optional)
	 * @param priority
	 *            - Priority Value (optional)
	 * @param fromDate
	 *            - From Date (optional)
	 * @param toDate
	 *            - To Date (optional)
	 * @param dateRequired
	 *            - if 'true' returns tasks by Day (optional)
	 * @param nameRequired
	 *            - if 'true' returns tasks by Task Name (optional)
	 * @return result of the query - {@link JSONArray} in {@link String} format
	 */
	public String generateCountOfTriageTasksQuery(Map<String, String> queryData, boolean dateRequired, boolean nameRequired) {

		String query = " select  ";
		int len = queryData.get("tenantId").length() + queryData.get("brand").length() + 13;
		if (dateRequired) {
			query = query + " date_format(art.start_time_,'%Y-%m-%d') as day, ";
		}
		if (nameRequired) {
			query = query + " substring(ari.group_id_," + len + ")  as name, ";
		}

		query = query + " count(1) as count_of_tasks from ACT_HI_TASKINST art,ACT_HI_IDENTITYLINK ari ";
		if (queryData.get("brand") != null && !"".equalsIgnoreCase(queryData.get("brand"))) {
			query = query + " ,ACT_HI_VARINST ahv where ahv.proc_inst_id_= art.proc_inst_id_ and ahv.text_='"
					+ queryData.get("brand") + "'";
		} else {
			query = query + " where 1=1 ";
		}
		query = query + " and ari.task_id_=art.id_ and substring(ari.group_id_," + len + ") !='' ";
		if (queryData.get("tenantId") != null && !"".equalsIgnoreCase(queryData.get("tenantId"))) {
			query = query + " and art.tenant_id_='" + queryData.get("tenantId") + "'";
		}
		if (queryData.get("processName") != null && !"".equalsIgnoreCase(queryData.get("processName"))) {
			if (queryData.get("tenantId") != null && !"".equalsIgnoreCase(queryData.get("tenantId"))) {
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ queryData.get("processName") + "' and arp.tenant_id_='" + queryData.get("tenantId") + "' ) ";
			} else
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ queryData.get("processName") + "' ) ";
		}
		if (queryData.get("taskName") != null && !"".equalsIgnoreCase(queryData.get("taskName"))) {
			query = query + " and art.name_ like '%" + queryData.get("taskName") + "'";
		}
		if (queryData.get("priority") != null && !"".equalsIgnoreCase(queryData.get("priority"))) {
			query = query + " and art.priority_ = '" + queryData.get("priority") + "'";
		}
		if ((queryData.get("fromDate") != null) && !"".equalsIgnoreCase(queryData.get("fromDate"))
				&& (queryData.get("toDate") != null) && !"".equalsIgnoreCase(queryData.get("toDate"))) {
			query = query + " and art.start_time_ between '" + queryData.get("fromDate") + "' and '"
					+ queryData.get("toDate") + "' ";
		}
		if (!nameRequired && dateRequired) {
			query = query + " group by day order by day asc ";
		}
		if (nameRequired && dateRequired) {
			query = query + " group by day,name order by day asc, name asc ";
		}
		if (nameRequired && !dateRequired) {
			query = query + " group by name  order by name asc ";
		}
		if (queryData.get("size") != null && !"".equalsIgnoreCase(queryData.get("size")))
			if (Integer.parseInt(queryData.get("size")) > 0)
				query = query + " LIMIT " + Integer.parseInt(queryData.get("size"));
		if (queryData.get("start") != null && !"".equalsIgnoreCase(queryData.get("start")))
			if (Integer.parseInt(queryData.get("start")) > 0)
				query = query + " OFFSET " + Integer.parseInt(queryData.get("start"));
		return query;
	}

	/**
	 * Returns the Count of Triage Tasks by Status (Open & Closed)
	 * 
	 * @param tenantId
	 *            - Id of the tenant/organization (optional)
	 * @param processName
	 *            - Name of the process (optional)
	 * @param taskName
	 *            - Name of the Task (optional)
	 * @param priority
	 *            - Priority Value (optional)
	 * @param fromDate
	 *            - From Date (optional)
	 * @param toDate
	 *            - To Date (optional)
	 * @param nameRequired
	 *            - if 'true' returns tasks by Task Name
	 * @return result of the query - {@link JSONArray} in {@link String} format
	 */
	public String generateCountOfTriageTasksByStatusQuery(Map<String, String> queryData, boolean nameRequired) {
		String query = " select ";
		int len = queryData.get("tenantId").length() + queryData.get("brand").length() + 13;
		if (nameRequired) {
			query = query + " substring(ari.group_id_," + len + ") as name, ";
		}
		query = query + " SUM(CASE WHEN art.delete_reason_ IS NULL THEN 1 ELSE 0 END)  count_of_opentasks, "
				+ " SUM(CASE WHEN art.delete_reason_ ='completed' THEN 1 ELSE 0 END)  count_of_closedtasks ";

		query = query + " from ACT_HI_TASKINST art,ACT_HI_IDENTITYLINK ari  ";
		if (queryData.get("brand") != null && !"".equalsIgnoreCase(queryData.get("brand"))) {
			query = query + " ,ACT_HI_VARINST ahv where ahv.proc_inst_id_= art.proc_inst_id_ and ahv.text_='"
					+ queryData.get("brand") + "'";
		} else {
			query = query + " where 1=1 ";
		}
		query = query + " and ari.task_id_=art.id_ and substring(ari.group_id_," + len + ") !='' ";
		if (queryData.get("tenantId") != null && !"".equalsIgnoreCase(queryData.get("tenantId"))) {
			query = query + " and art.tenant_id_='" + queryData.get("tenantId") + "'";
		}
		if (queryData.get("processName") != null && !"".equalsIgnoreCase(queryData.get("processName"))) {
			if (queryData.get("tenantId") != null && !"".equalsIgnoreCase(queryData.get("tenantId"))) {
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ queryData.get("processName") + "' and arp.tenant_id_='" + queryData.get("tenantId") + "' ) ";
			} else
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ queryData.get("processName") + "' ) ";
		}
		if (queryData.get("taskName") != null && !"".equalsIgnoreCase(queryData.get("taskName"))) {
			query = query + " and art.name_ like '%" + queryData.get("taskName") + "'";
		}
		if (queryData.get("priority") != null && !"".equalsIgnoreCase(queryData.get("priority"))) {
			query = query + " and art.priority_ = '" + queryData.get("priority") + "'";
		}
		if ((queryData.get("fromDate") != null) && !"".equalsIgnoreCase(queryData.get("fromDate"))
				&& (queryData.get("toDate") != null) && !"".equalsIgnoreCase(queryData.get("toDate"))) {
			query = query + " and art.start_time_ between '" + queryData.get("fromDate") + "' and '"
					+ queryData.get("toDate") + "' ";
		}

		if (nameRequired)
			query = query + " group by name order by name asc";
		if (queryData.get("size") != null && !"".equalsIgnoreCase(queryData.get("size")))
			if (Integer.parseInt(queryData.get("size")) > 0)
				query = query + " LIMIT " + Integer.parseInt(queryData.get("size"));
		if (queryData.get("start") != null && !"".equalsIgnoreCase(queryData.get("start")))
			if (Integer.parseInt(queryData.get("start")) > 0)
				query = query + " OFFSET " + Integer.parseInt(queryData.get("start"));
		return query;
	}

	/**
	 * Returns the Average Task Aging by Task Name
	 * 
	 * @param tenantId
	 *            - Id of the tenant/organization (optional)
	 * @param processName
	 *            - Name of the process (optional)
	 * @param taskName
	 *            - Name of the Task (optional)
	 * @param priority
	 *            - Priority Value (optional)
	 * @param fromDate
	 *            - From Date (optional)
	 * @param toDate
	 *            - To Date (optional)
	 * @return result of the query - {@link JSONArray} in {@link String} format
	 */
	public String generateAvgTaskAgeingByNameQuery(Map<String, String> queryData) {
		int len = queryData.get("tenantId").length() + queryData.get("brand").length() + 13;
		String query = " select TaskAgeing.name,ROUND(CONVERT(AVG(TaskAgeing.days), UNSIGNED INTEGER),2) as avgdays from ("
				+ " select substring(ari.group_id_," + len + ") as name, "
				+ " CASE WHEN art.delete_reason_='completed' THEN " + " DATEDIFF( art.end_time_, art.start_time_) "
				+ " WHEN art.delete_reason_ IS NULL THEN  " + " DATEDIFF( current_timestamp, art.start_time_) "
				+ " ELSE 0 END " + " as days from ACT_HI_TASKINST art,ACT_HI_IDENTITYLINK ari ";

		if (queryData.get("brand") != null && !"".equalsIgnoreCase(queryData.get("brand"))) {
			query = query + " ,ACT_HI_VARINST ahv where ahv.proc_inst_id_= art.proc_inst_id_ and ahv.text_='"
					+ queryData.get("brand") + "'";
		} else {
			query = query + " where 1=1 ";
		}
		query = query + " and ari.task_id_=art.id_ and substring(ari.group_id_," + len + ") !='' ";
		if (queryData.get("tenantId") != null && !"".equalsIgnoreCase(queryData.get("tenantId"))) {
			query = query + " and art.tenant_id_='" + queryData.get("tenantId") + "'";
		}
		if (queryData.get("processName") != null && !"".equalsIgnoreCase(queryData.get("processName"))) {
			if (queryData.get("tenantId") != null && !"".equalsIgnoreCase(queryData.get("tenantId"))) {
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ queryData.get("processName") + "' and arp.tenant_id_='" + queryData.get("tenantId") + "' ) ";
			} else
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ queryData.get("processName") + "' ) ";
		}
		if (queryData.get("taskName") != null && !"".equalsIgnoreCase(queryData.get("taskName"))) {
			query = query + " and art.name_ like '%" + queryData.get("taskName") + "'";
		}
		if (queryData.get("priority") != null && !"".equalsIgnoreCase(queryData.get("priority"))) {
			query = query + " and art.priority_ = '" + queryData.get("priority") + "'";
		}
		if ((queryData.get("fromDate") != null) && !"".equalsIgnoreCase(queryData.get("fromDate"))
				&& (queryData.get("toDate") != null) && !"".equalsIgnoreCase(queryData.get("toDate"))) {
			query = query + " and art.start_time_ between '" + queryData.get("fromDate") + "' and '"
					+ queryData.get("toDate") + "' ";
		}
		query = query + " ) as TaskAgeing group by TaskAgeing.name order by avgdays desc ";
		if (queryData.get("size") != null && !"".equalsIgnoreCase(queryData.get("size")))
			if (Integer.parseInt(queryData.get("size")) > 0)
				query = query + " LIMIT " + Integer.parseInt(queryData.get("size"));
		if (queryData.get("start") != null && !"".equalsIgnoreCase(queryData.get("start")))
			if (Integer.parseInt(queryData.get("start")) > 0)
				query = query + " OFFSET " + Integer.parseInt(queryData.get("start"));
		return query;
	}

	/**
	 * Returns the list of users with given string using like operator in sql query
	 * 
	 * @param emailLike
	 *            - Name of the process (optional)
	 * @return result of the query - {@link JSONArray} in {@link String} format
	 */
	public String getUsers(String tenantId, String emailLike) {

		String query = " select id_ id,email_ email,first_ as first,last_ as last from  ACT_ID_USER where email_ like '"
				+ emailLike + "%' and id_ in (select id_ from ACT_ID_MEMBERSHIP where group_id_='" + tenantId + "')";

		return query;

	}

	/**
	 * Returns the list of tasks
	 * 
	 * @param tenantId
	 *            - Id of the tenant/organization (optional)
	 * @param processName
	 *            - Name of the process (optional)
	 * @param taskName
	 *            - Name of the Task (optional)
	 * @param priority
	 *            - Priority Value (optional)
	 * @param fromDate
	 *            - From Date (optional)
	 * @param toDate
	 *            - To Date (optional)
	 * @param startedBy
	 *            - Name of the user who started
	 * @param assignee
	 *            - Name of the assignee
	 * @param status
	 *            - Status of the task
	 * @param groupName
	 *            - Group Name
	 * @param isStuck
	 *            - if 'true' returns the tasks stuck
	 * @return result of the query - {@link JSONArray} in {@link String} format
	 */
	public String generateTasksQuery(Map<String, String> queryData) {

		String query = " select distinct art.id_ as task_id,art.description_ as description,substring(art.name_,position('on' IN art.name_)+6) as toIDpic,art.start_time_ as start_time,art.name_ as name, art.description_ as description,art.assignee_ as assignee,art.duration_ as duration,art.delete_reason_ as delete_reason,art.priority_ as priority from ACT_HI_TASKINST art,ACT_HI_IDENTITYLINK ari  ";
		if (queryData.get("brand") != null && !"".equalsIgnoreCase(queryData.get("brand"))) {
			query = query + " ,ACT_HI_VARINST ahv where ahv.proc_inst_id_= art.proc_inst_id_ and ahv.text_='"
					+ queryData.get("brand") + "'";
		} else {
			query = query + " where 1=1 ";
		}
		if (queryData.get("tenantId") != null && !"".equalsIgnoreCase(queryData.get("tenantId"))) {
			query = query + " and art.tenant_id_='" + queryData.get("tenantId") + "'";
		}
		if (queryData.get("processName") != null && !"".equalsIgnoreCase(queryData.get("processName"))) {
			if (queryData.get("tenantId") != null && !"".equalsIgnoreCase(queryData.get("tenantId"))) {
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ queryData.get("processName") + "' and arp.tenant_id_='" + queryData.get("tenantId") + "' ) ";
			} else
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ queryData.get("processName") + "' ) ";
			query = query + " and (art.proc_inst_id_=ari.proc_inst_id_ or art.id_=ari.task_id_) ";
		}
		if (queryData.get("startedBy") != null && !"".equalsIgnoreCase(queryData.get("startedBy"))) {
			query = query + "  and (ari.type_='starter' or ari.type_='candidate') and ari.user_id_='"
					+ queryData.get("startedBy") + "'";
		}
		if (queryData.get("taskDescription") != null && !"".equalsIgnoreCase(queryData.get("taskDescription"))) {
			query = query + " and art.description_='" + queryData.get("taskDescription") + "'";
		}
		if (queryData.get("status") != null && !"".equalsIgnoreCase(queryData.get("status"))) {
			if ("closed".equalsIgnoreCase(queryData.get("status"))) {
				query = query + " and art.delete_reason_='completed' ";
			}
			if ("open".equalsIgnoreCase(queryData.get("status"))) {
				query = query + " and art.delete_reason_ IS NULL ";
			}
		}
		if (queryData.get("assignee") != null && !"".equalsIgnoreCase(queryData.get("assignee"))) {
			if ("all".equalsIgnoreCase(queryData.get("assignee")))
				query = query + " and art.assignee_ IS NOT NULL";
			else
				query = query + " and art.assignee_='" + queryData.get("assignee") + "'";
		}
		if (queryData.get("groupName") != null && !"".equalsIgnoreCase(queryData.get("groupName"))) {
			query = query + " and ari.group_id_='" + queryData.get("groupName") + "'";
		}
		if (queryData.get("isStuck") != null && !"".equalsIgnoreCase(queryData.get("isStuck"))) {
			if ("true".equalsIgnoreCase(queryData.get("isStuck"))) {
				query = query + " and art.duration_ >0 ";
			}
		}
		if (queryData.get("taskName") != null && !"".equalsIgnoreCase(queryData.get("taskName"))) {
			query = query + " and art.name_ like '%" + queryData.get("taskName") + "'";
		}
		if (queryData.get("priority") != null && !"".equalsIgnoreCase(queryData.get("priority"))) {
			query = query + " and art.priority_ = '" + queryData.get("priority") + "'";
		}
		if ((queryData.get("fromDate") != null) && !"".equalsIgnoreCase(queryData.get("fromDate"))
				&& (queryData.get("toDate") != null) && !"".equalsIgnoreCase(queryData.get("toDate"))) {
			query = query + " and art.start_time_ between '" + queryData.get("fromDate") + "' and '"
					+ queryData.get("toDate") + "' ";
		}
		query = query + "  order by art.start_time_ desc ";
		if (queryData.get("size") != null && !"".equalsIgnoreCase(queryData.get("size")))
			if (Integer.parseInt(queryData.get("size")) > 0)
				query = query + " LIMIT " + Integer.parseInt(queryData.get("size"));
		if (queryData.get("start") != null && !"".equalsIgnoreCase(queryData.get("start")))
			if (Integer.parseInt(queryData.get("start")) > 0)
				query = query + " OFFSET " + Integer.parseInt(queryData.get("start"));
		return query;
	}

}
