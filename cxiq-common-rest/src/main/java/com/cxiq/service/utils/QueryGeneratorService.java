package com.cxiq.service.utils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class QueryGeneratorService {

	final Logger LOGGER = Logger.getLogger(QueryGeneratorService.class.getName());

	@Qualifier("activiti")
	@Autowired
	DataSource activitiDataSource;

	@Qualifier("cxiq")
	@Autowired
	DataSource cxiqDataSource;

	@Value("${cxiq.admin}")
	String cxiqAdmin;

	public String generateTasksPerUserQuery(String tenantId, String brand, String processName, String taskName,
			String priority, String fromDate, String toDate, Long size, Long start) {
		String query = " select ari.user_id_ user_id,count(1) as count_of_tasks from ACT_HI_TASKINST art,ACT_HI_IDENTITYLINK ari  ";

		if (brand != null && !"".equalsIgnoreCase(brand)) {
			query = query + " ,ACT_HI_VARINST ahv where ahv.proc_inst_id_= art.proc_inst_id_ and ahv.text_='" + brand
					+ "'";
		} else {
			query = query + " where 1=1 ";
		}
		query = query + " and art.proc_inst_id_=ari.proc_inst_id_ and ari.type_='starter' ";
		if (tenantId != null && !"".equalsIgnoreCase(tenantId)) {
			query = query + " and art.tenant_id_='" + tenantId + "'";
		}
		if (processName != null && !"".equalsIgnoreCase(processName)) {
			if (tenantId != null && !"".equalsIgnoreCase(tenantId)) {
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ processName + "' and arp.tenant_id_='" + tenantId + "' ) ";
			} else
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ processName + "' ) ";
		}
		if (taskName != null && !"".equalsIgnoreCase(taskName)) {
			query = query + " and art.name_ like '%" + taskName + "'";
		}
		if (priority != null && !"".equalsIgnoreCase(priority)) {
			query = query + " and art.priority_ = '" + priority + "'";
		}
		if ((fromDate != null) && (toDate != null) && (!toDate.equals("")) && (!fromDate.equals(""))) {
			query = query + " and art.start_time_ between '" + fromDate + "' and '" + toDate + "' ";
		}
		query = query + " group by ari.user_id_ order by count_of_tasks desc ";
		if (size != null && size > 0)
			query = query + " LIMIT " + size;
		if (start != null && start > 0)
			query = query + " OFFSET " + start;
		return query;
	}

	public String generateCountOfTasksByTaskNameQuery(String tenantId, String brand, String processName,
			String taskName, String priority, String fromDate, String toDate, Long size, Long start) {

		String query = " select substring(art.name_,position('on' IN art.name_)+6) task_name,count(1) as count_of_tasks from ACT_HI_TASKINST art ";

		if (brand != null && !"".equalsIgnoreCase(brand)) {
			query = query + " ,ACT_HI_VARINST ahv where ahv.proc_inst_id_= art.proc_inst_id_ and ahv.text_='" + brand
					+ "'";
		} else {
			query = query + " where 1=1 ";
		}
		if (tenantId != null && !"".equalsIgnoreCase(tenantId)) {
			query = query + " and art.tenant_id_='" + tenantId + "'";
		}
		if (processName != null && !"".equalsIgnoreCase(processName)) {
			if (tenantId != null && !"".equalsIgnoreCase(tenantId)) {
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ processName + "' and arp.tenant_id_='" + tenantId + "' ) ";
			} else
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ processName + "' ) ";
		}
		if (taskName != null && !"".equalsIgnoreCase(taskName)) {
			query = query + " and art.name_ like '%" + taskName + "'";
		}
		if (priority != null && !"".equalsIgnoreCase(priority)) {
			query = query + " and art.priority_ = '" + priority + "'";
		}
		if ((fromDate != null) && (toDate != null) && (!toDate.equals("")) && (!fromDate.equals(""))) {
			query = query + " and art.start_time_ between '" + fromDate + "' and '" + toDate + "' ";
		}
		query = query + " group by task_name order by count_of_tasks desc";
		if (size != null && size > 0)
			query = query + " LIMIT " + size;
		if (start != null && start > 0)
			query = query + " OFFSET " + start;
		return query;
	}

	public String generateCountOfOpenTasksByUserQuery(String tenantId, String brand, String processName,
			String taskName, String priority, String fromDate, String toDate, Long size, Long start) {

		String query = " select art.assignee_ as user_id,count(1) as count_of_tasks_per_topic from ACT_HI_TASKINST art  ";
		if (brand != null && !"".equalsIgnoreCase(brand)) {
			query = query + " ,ACT_HI_VARINST ahv where ahv.proc_inst_id_= art.proc_inst_id_ and ahv.text_='" + brand
					+ "' and art.delete_reason_ IS NULL ";
		} else {
			query = query + " where 1=1 and art.delete_reason_ IS NULL ";
		}
		if (tenantId != null && !"".equalsIgnoreCase(tenantId)) {
			query = query + " and art.tenant_id_='" + tenantId + "'";
		}
		if (processName != null && !"".equalsIgnoreCase(processName)) {
			if (tenantId != null && !"".equalsIgnoreCase(tenantId)) {
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ processName + "' and arp.tenant_id_='" + tenantId + "' ) ";
			} else
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ processName + "' ) ";
		}
		if (taskName != null && !"".equalsIgnoreCase(taskName)) {
			query = query + " and art.name_ like '%" + taskName + "'";
		}
		if (priority != null && !"".equalsIgnoreCase(priority)) {
			query = query + " and art.priority_ = '" + priority + "'";
		}
		if ((fromDate != null) && (toDate != null) && (!toDate.equals("")) && (!fromDate.equals(""))) {
			query = query + " and art.start_time_ between '" + fromDate + "' and '" + toDate + "' ";
		}
		query = query + " group by art.assignee_ order by count_of_tasks_per_topic desc ";
		if (size != null && size > 0)
			query = query + " LIMIT " + size;
		if (start != null && start > 0)
			query = query + " OFFSET " + start;
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
	public String generateAvgTaskAgingByPriorityQuery(String tenantId, String brand, String processName,
			String taskName, String priority, String fromDate, String toDate, Long size, Long start) {

		String query = " select TaskAgeing.priority as priority,ROUND(CONVERT(AVG(TaskAgeing.days), UNSIGNED INTEGER),2) as avg_age from ("
				+ " select art.priority_ as priority, " + " CASE WHEN art.delete_reason_='completed' THEN "
				+ " DATEDIFF( art.end_time_, art.start_time_) " + " WHEN art.delete_reason_ IS NULL THEN  "
				+ " DATEDIFF( current_timestamp, art.start_time_) " + " ELSE 0 END "
				+ " as days from ACT_HI_TASKINST art ";

		if (brand != null && !"".equalsIgnoreCase(brand)) {
			query = query + " ,ACT_HI_VARINST ahv where ahv.proc_inst_id_= art.proc_inst_id_ and ahv.text_='" + brand
					+ "'";
		} else {
			query = query + " where 1=1 ";
		}
		if (tenantId != null && !"".equalsIgnoreCase(tenantId)) {
			query = query + " and art.tenant_id_='" + tenantId + "'";
		}
		if (processName != null && !"".equalsIgnoreCase(processName)) {
			if (tenantId != null && !"".equalsIgnoreCase(tenantId)) {
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ processName + "' and arp.tenant_id_='" + tenantId + "' ) ";
			} else
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ processName + "' ) ";
		}
		if (taskName != null && !"".equalsIgnoreCase(taskName)) {
			query = query + " and art.name_ like '%" + taskName + "'";
		}
		if (priority != null && !"".equalsIgnoreCase(priority)) {
			query = query + " and art.priority_ = '" + priority + "'";
		}
		if ((fromDate != null) && (toDate != null) && (!toDate.equals("")) && (!fromDate.equals(""))) {
			query = query + " and art.start_time_ between '" + fromDate + "' and '" + toDate + "' ";
		}
		query = query + " ) as TaskAgeing group by priority order by priority desc ";
		if (size != null && size > 0)
			query = query + " LIMIT " + size;
		if (start != null && start > 0)
			query = query + " OFFSET " + start;
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
	public String generateStuckTasksByAgeQuery(String tenantId, String brand, String processName, String taskName,
			String priority, String fromDate, String toDate, Long size, Long start) {

		String query = " select * from ( select ari.user_id_ user_id,art.id_ as task_id,  DATEDIFF( current_timestamp, art.start_time_)  "
				+ "  as age from ACT_HI_TASKINST art,ACT_HI_IDENTITYLINK ari ";

		if (brand != null && !"".equalsIgnoreCase(brand)) {
			query = query + " ,ACT_HI_VARINST ahv where ahv.proc_inst_id_= art.proc_inst_id_ and ahv.text_='" + brand
					+ "'";
		} else {
			query = query + " where 1=1 ";
		}
		query = query
				+ " and art.proc_inst_id_=ari.proc_inst_id_ and ari.type_='starter' and art.delete_reason_ IS NULL ";
		if (tenantId != null && !"".equalsIgnoreCase(tenantId)) {
			query = query + " and art.tenant_id_='" + tenantId + "'";
		}
		if (processName != null && !"".equalsIgnoreCase(processName)) {
			if (tenantId != null && !"".equalsIgnoreCase(tenantId)) {
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ processName + "' and arp.tenant_id_='" + tenantId + "' ) ";
			} else
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ processName + "' ) ";
		}
		if (taskName != null && !"".equalsIgnoreCase(taskName)) {
			query = query + " and art.name_ like '%" + taskName + "'";
		}
		if (priority != null && !"".equalsIgnoreCase(priority)) {
			query = query + " and art.priority_ = '" + priority + "'";
		}
		if ((fromDate != null) && (toDate != null) && (!toDate.equals("")) && (!fromDate.equals(""))) {
			query = query + " and art.start_time_ between '" + fromDate + "' and '" + toDate + "' ";
		}
		query = query + " ) as z where z.age>0 order by z.age desc";
		if (size != null && size > 0)
			query = query + " LIMIT " + size;
		if (start != null && start > 0)
			query = query + " OFFSET " + start;
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
	public String generateCountOfTasksByDayQuery(String tenantId, String brand, String processName, String taskName,
			String priority, String fromDate, String toDate, boolean dateRequired, boolean nameRequired, Long size,
			Long start) {

		String query = " select  ";
		if (dateRequired) {
			query = query + " date_format(art.start_time_,'%Y-%m-%d') as day, ";
		}
		if (nameRequired) {
			query = query + " substring(art.name_, 0,position(' ' IN art.name_ )) as name, ";
		}

		query = query + " count(1) as count_of_tasks from ACT_HI_TASKINST art  ";
		if (brand != null && !"".equalsIgnoreCase(brand)) {
			query = query + " ,ACT_HI_VARINST ahv where ahv.proc_inst_id_= art.proc_inst_id_ and ahv.text_='" + brand
					+ "'";
		} else {
			query = query + " where 1=1 ";
		}
		if (tenantId != null && !"".equalsIgnoreCase(tenantId)) {
			query = query + " and art.tenant_id_='" + tenantId + "'";
		}
		if (processName != null && !"".equalsIgnoreCase(processName)) {
			if (tenantId != null && !"".equalsIgnoreCase(tenantId)) {
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ processName + "' and arp.tenant_id_='" + tenantId + "' ) ";
			} else
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ processName + "' ) ";
		}
		if (taskName != null && !"".equalsIgnoreCase(taskName)) {
			query = query + " and art.name_ like '%" + taskName + "'";
		}
		if (priority != null && !"".equalsIgnoreCase(priority)) {
			query = query + " and art.priority_ = '" + priority + "'";
		}
		if ((fromDate != null) && (toDate != null) && (!toDate.equals("")) && (!fromDate.equals(""))) {
			query = query + " and art.start_time_ between '" + fromDate + "' and '" + toDate + "' ";
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
		if (size != null && size > 0)
			query = query + " LIMIT " + size;
		if (start != null && start > 0)
			query = query + " OFFSET " + start;
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
	public String generateCountOfTasksByStatusQuery(String tenantId, String brand, String processName, String taskName,
			String priority, String fromDate, String toDate, boolean nameRequired, Long size, Long start) {

		String query = " select ";
		if (nameRequired) {
			query = query + " art.name_ as name, ";
		}
		query = query + " SUM(CASE WHEN art.delete_reason_ IS NULL THEN 1 ELSE 0 END)  count_of_opentasks, "
				+ " SUM(CASE WHEN art.delete_reason_ ='completed' THEN 1 ELSE 0 END)  count_of_closedtasks ";

		query = query + " from ACT_HI_TASKINST art ";
		if (brand != null && !"".equalsIgnoreCase(brand)) {
			query = query + " ,ACT_HI_VARINST ahv where ahv.proc_inst_id_= art.proc_inst_id_ and ahv.text_='" + brand
					+ "'";
		} else {
			query = query + " where 1=1 ";
		}
		if (tenantId != null && !"".equalsIgnoreCase(tenantId)) {
			query = query + " and art.tenant_id_='" + tenantId + "'";
		}
		if (processName != null && !"".equalsIgnoreCase(processName)) {
			if (tenantId != null && !"".equalsIgnoreCase(tenantId)) {
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ processName + "' and arp.tenant_id_='" + tenantId + "' ) ";
			} else
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ processName + "' ) ";
		}
		if (taskName != null && !"".equalsIgnoreCase(taskName)) {
			query = query + " and art.name_ like '%" + taskName + "'";
		}
		if (priority != null && !"".equalsIgnoreCase(priority)) {
			query = query + " and art.priority_ = '" + priority + "'";
		}
		if ((fromDate != null) && (toDate != null) && (!toDate.equals("")) && (!fromDate.equals(""))) {
			query = query + " and art.start_time_ between '" + fromDate + "' and '" + toDate + "' ";
		}

		if (nameRequired)
			query = query + " group by art.name_ order by art.name_ asc";
		if (size != null && size > 0)
			query = query + " LIMIT " + size;
		if (start != null && start > 0)
			query = query + " OFFSET " + start;
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
	public String generateCountOfTriageTasksQuery(String tenantId, String brand, String processName, String taskName,
			String priority, String fromDate, String toDate, boolean dateRequired, boolean nameRequired, Long size,
			Long start) {

		String query = " select  ";
		int len = tenantId.length() + brand.length() + 13;
		if (dateRequired) {
			query = query + " date_format(art.start_time_,'%Y-%m-%d') as day, ";
		}
		if (nameRequired) {
			query = query + " substring(ari.group_id_," + len + ")  as name, ";
		}

		query = query + " count(1) as count_of_tasks from ACT_HI_TASKINST art,ACT_HI_IDENTITYLINK ari ";
		if (brand != null && !"".equalsIgnoreCase(brand)) {
			query = query + " ,ACT_HI_VARINST ahv where ahv.proc_inst_id_= art.proc_inst_id_ and ahv.text_='" + brand
					+ "'";
		} else {
			query = query + " where 1=1 ";
		}
		query = query + " and ari.task_id_=art.id_ and substring(ari.group_id_," + len + ") !='' ";
		if (tenantId != null && !"".equalsIgnoreCase(tenantId)) {
			query = query + " and art.tenant_id_='" + tenantId + "'";
		}
		if (processName != null && !"".equalsIgnoreCase(processName)) {
			if (tenantId != null && !"".equalsIgnoreCase(tenantId)) {
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ processName + "' and arp.tenant_id_='" + tenantId + "' ) ";
			} else
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ processName + "' ) ";
		}
		if (taskName != null && !"".equalsIgnoreCase(taskName)) {
			query = query + " and art.name_ like '%" + taskName + "'";
		}
		if (priority != null && !"".equalsIgnoreCase(priority)) {
			query = query + " and art.priority_ = '" + priority + "'";
		}
		if ((fromDate != null) && (toDate != null) && (!toDate.equals("")) && (!fromDate.equals(""))) {
			query = query + " and art.start_time_ between '" + fromDate + "' and '" + toDate + "' ";
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
		if (size != null && size > 0)
			query = query + " LIMIT " + size;
		if (start != null && start > 0)
			query = query + " OFFSET " + start;
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
	public String generateCountOfTriageTasksByStatusQuery(String tenantId, String brand, String processName,
			String taskName, String priority, String fromDate, String toDate, boolean nameRequired, Long size,
			Long start) {
		String query = " select ";
		int len = tenantId.length() + brand.length() + 13;
		if (nameRequired) {
			query = query + " substring(ari.group_id_," + len + ") as name, ";
		}
		query = query + " SUM(CASE WHEN art.delete_reason_ IS NULL THEN 1 ELSE 0 END)  count_of_opentasks, "
				+ " SUM(CASE WHEN art.delete_reason_ ='completed' THEN 1 ELSE 0 END)  count_of_closedtasks ";

		query = query + " from ACT_HI_TASKINST art,ACT_HI_IDENTITYLINK ari  ";
		if (brand != null && !"".equalsIgnoreCase(brand)) {
			query = query + " ,ACT_HI_VARINST ahv where ahv.proc_inst_id_= art.proc_inst_id_ and ahv.text_='" + brand
					+ "'";
		} else {
			query = query + " where 1=1 ";
		}
		query = query + " and ari.task_id_=art.id_ and substring(ari.group_id_," + len + ") !='' ";
		if (tenantId != null && !"".equalsIgnoreCase(tenantId)) {
			query = query + " and art.tenant_id_='" + tenantId + "'";
		}
		if (processName != null && !"".equalsIgnoreCase(processName)) {
			if (tenantId != null && !"".equalsIgnoreCase(tenantId)) {
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ processName + "' and arp.tenant_id_='" + tenantId + "' ) ";
			} else
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ processName + "' ) ";
		}
		if (taskName != null && !"".equalsIgnoreCase(taskName)) {
			query = query + " and art.name_ like '%" + taskName + "'";
		}
		if (priority != null && !"".equalsIgnoreCase(priority)) {
			query = query + " and art.priority_ = '" + priority + "'";
		}
		if ((fromDate != null) && (toDate != null) && (!toDate.equals("")) && (!fromDate.equals(""))) {
			query = query + " and art.start_time_ between '" + fromDate + "' and '" + toDate + "' ";
		}

		if (nameRequired)
			query = query + " group by name order by name asc";
		if (size != null && size > 0)
			query = query + " LIMIT " + size;
		if (start != null && start > 0)
			query = query + " OFFSET " + start;
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
	public String generateAvgTaskAgeingByNameQuery(String tenantId, String brand, String processName, String taskName,
			String priority, String fromDate, String toDate, Long size, Long start) {
		int len = tenantId.length() + brand.length() + 13;
		String query = " select TaskAgeing.name,ROUND(CONVERT(AVG(TaskAgeing.days), UNSIGNED INTEGER),2) as avgdays from ("
				+ " select substring(ari.group_id_," + len + ") as name, "
				+ " CASE WHEN art.delete_reason_='completed' THEN " + " DATEDIFF( art.end_time_, art.start_time_) "
				+ " WHEN art.delete_reason_ IS NULL THEN  " + " DATEDIFF( current_timestamp, art.start_time_) "
				+ " ELSE 0 END " + " as days from ACT_HI_TASKINST art,ACT_HI_IDENTITYLINK ari ";

		if (brand != null && !"".equalsIgnoreCase(brand)) {
			query = query + " ,ACT_HI_VARINST ahv where ahv.proc_inst_id_= art.proc_inst_id_ and ahv.text_='" + brand
					+ "'";
		} else {
			query = query + " where 1=1 ";
		}
		query = query + " and ari.task_id_=art.id_ and substring(ari.group_id_," + len + ") !='' ";
		if (tenantId != null && !"".equalsIgnoreCase(tenantId)) {
			query = query + " and art.tenant_id_='" + tenantId + "'";
		}
		if (processName != null && !"".equalsIgnoreCase(processName)) {
			if (tenantId != null && !"".equalsIgnoreCase(tenantId)) {
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ processName + "' and arp.tenant_id_='" + tenantId + "' ) ";
			} else
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ processName + "' ) ";
		}
		if (taskName != null && !"".equalsIgnoreCase(taskName)) {
			query = query + " and art.name_ like '%" + taskName + "'";
		}
		if (priority != null && !"".equalsIgnoreCase(priority)) {
			query = query + " and art.priority_ = '" + priority + "'";
		}
		if ((fromDate != null) && (toDate != null) && (!toDate.equals("")) && (!fromDate.equals(""))) {
			query = query + " and art.start_time_ between '" + fromDate + "' and '" + toDate + "' ";
		}
		query = query + " ) as TaskAgeing group by TaskAgeing.name order by avgdays desc ";
		if (size != null && size > 0)
			query = query + " LIMIT " + size;
		if (start != null && start > 0)
			query = query + " OFFSET " + start;
		return query;
	}

	/**
	 * Returns the list of users with given string using like operator in sql query
	 * 
	 * @param emailLike
	 *            - Name of the process (optional)
	 * @return result of the query - {@link JSONArray} in {@link String} format
	 */
	public String generateUsersQuery(String tenantId, String emailLike) {

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
	public String generateTasksQuery(String tenantId, String brand, String processName, String taskName,
			String taskDescription, String priority, String fromDate, String toDate, String startedBy, String assignee,
			String status, String groupName, String isStuck, Long size, Long start) {

		String query = " select distinct art.id_ as task_id,art.description_ as description,substring(art.name_,position('on' IN art.name_)+6) as toIDpic,art.start_time_ as start_time,art.name_ as name, art.description_ as description,art.assignee_ as assignee,art.duration_ as duration,art.delete_reason_ as delete_reason,art.priority_ as priority from ACT_HI_TASKINST art,ACT_HI_IDENTITYLINK ari  ";
		if (brand != null && !"".equalsIgnoreCase(brand)) {
			query = query + " ,ACT_HI_VARINST ahv where ahv.proc_inst_id_= art.proc_inst_id_ and ahv.text_='" + brand
					+ "'";
		} else {
			query = query + " where 1=1 ";
		}
		if (tenantId != null && !"".equalsIgnoreCase(tenantId)) {
			query = query + " and art.tenant_id_='" + tenantId + "'";
		}
		if (processName != null && !"".equalsIgnoreCase(processName)) {
			if (tenantId != null && !"".equalsIgnoreCase(tenantId)) {
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ processName + "' and arp.tenant_id_='" + tenantId + "' ) ";
			} else
				query = query + " and art.proc_def_id_ in (select arp.id_ from ACT_RE_PROCDEF arp where arp.key_='"
						+ processName + "' ) ";
			query = query + " and (art.proc_inst_id_=ari.proc_inst_id_ or art.id_=ari.task_id_) ";
		}
		if (startedBy != null && !"".equalsIgnoreCase(startedBy)) {
			query = query + "  and (ari.type_='starter' or ari.type_='candidate') and ari.user_id_='" + startedBy + "'";
		}
		if (taskDescription != null && !"".equalsIgnoreCase(taskDescription)) {
			query = query + " and art.description_='" + taskDescription + "'";
		}
		if (status != null && "closed".equalsIgnoreCase(status)) {
			query = query + " and art.delete_reason_='completed' ";
		}
		if (status != null && "open".equalsIgnoreCase(status)) {
			query = query + " and art.delete_reason_ IS NULL ";
		}
		if (assignee != null && !"".equalsIgnoreCase(assignee)) {
			if ("all".equalsIgnoreCase(assignee))
				query = query + " and art.assignee_ IS NOT NULL";
			else
				query = query + " and art.assignee_='" + assignee + "'";
		}
		if (groupName != null && !"".equalsIgnoreCase(groupName)) {
			query = query + " and ari.group_id_='" + groupName + "'";
		}
		if (isStuck != null && "true".equalsIgnoreCase(isStuck)) {
			query = query + " and art.duration_ >0 ";
		}
		if (taskName != null && !"".equalsIgnoreCase(taskName)) {
			query = query + " and art.name_ like '%" + taskName + "'";
		}
		if (priority != null && !"".equalsIgnoreCase(priority)) {
			query = query + " and art.priority_ = '" + priority + "'";
		}
		if ((fromDate != null) && (toDate != null) && (!toDate.equals("")) && (!fromDate.equals(""))) {
			query = query + " and art.start_time_ between '" + fromDate + "' and '" + toDate + "' ";
		}
		query = query + "  order by art.start_time_ desc ";
		if (size != null && size > 0)
			query = query + " LIMIT " + size;
		if (start != null && start > 0)
			query = query + " OFFSET " + start;
		return query;
	}

	public String getTaskJourney(String taskID) {
		String result = null, taskJourneyQuery = "";
		Connection connection = null;
		if (taskID != null && !"".equalsIgnoreCase(taskID)) {

			String commentTableQuery = " select count(1) as cnt,user_id_ as users,time_ from ACT_HI_COMMENT where task_id_='"
					+ taskID
					+ "' and action_='AddUserLink' and substring(message_, position('|'  IN message_)+2)='assignee' group by user_id_,time_ order by time_ asc limit 1 ";
			String identityLinkQuery = " select * from ACT_HI_IDENTITYLINK where task_id_='" + taskID
					+ "' and group_id_ IS NOT NULL ";
			try {
				connection = activitiDataSource.getConnection();

				boolean isReassigned = false, isAssignedToGroup = false;
				ResultSet rs = connection.createStatement().executeQuery(commentTableQuery);
				if (rs.next())
					isReassigned = true;

				rs.close();
				ResultSet rs1 = connection.createStatement().executeQuery(identityLinkQuery);
				if (rs1.next())
					isAssignedToGroup = true;

				rs1.close();
				String admin = cxiqAdmin;
				taskJourneyQuery = " select taskjourney.fromID,taskjourney.toID,taskjourney.action,taskjourney.action_date,taskjourney.duration from  (";
				taskJourneyQuery += " select CASE WHEN ari.user_id_ IS NULL THEN '" + admin
						+ "' else ari.user_id_ end as fromID, CASE WHEN ari.user_id_ IS NULL THEN '" + admin
						+ "' else ari.user_id_ end as toID,'started' as action,art.start_time_ as action_date,0 as duration  from ACT_HI_TASKINST art,ACT_HI_IDENTITYLINK ari "
						+ "  where art.id_='" + taskID
						+ "' and   (ari.proc_inst_id_=art.proc_inst_id_ or ari.task_id_=art.id_) and (ari.type_='starter' or ari.type_='candidate') ";

				if (isAssignedToGroup) {
					taskJourneyQuery += " union ";
					taskJourneyQuery += "  select p.toID as fromID,CASE WHEN ari.user_id_ IS NULL THEN ari.group_id_ else art.assignee_ end as toID,'assigned' as action,art.start_time_ as action_date,0 as duration  from ACT_HI_TASKINST art,ACT_HI_IDENTITYLINK ari, (  select CASE WHEN ari.user_id_ IS NULL THEN '"
							+ admin + "' else ari.user_id_ end as fromID,CASE WHEN ari.user_id_ IS NULL THEN '" + admin
							+ "' else ari.user_id_ end as toID,'started' as action,art.start_time_ as action_date,'0' as duration  from ACT_HI_TASKINST art,ACT_HI_IDENTITYLINK ari where art.id_='"
							+ taskID
							+ "' and   (ari.proc_inst_id_=art.proc_inst_id_ or ari.task_id_=art.id_)  and (ari.type_='starter' or ari.type_='candidate' ) "
							+

							" ) as p where art.id_='" + taskID
							+ "' and   (ari.proc_inst_id_=art.proc_inst_id_ or ari.task_id_=art.id_)  and ( ari.type_='candidate')  ";
					if (isReassigned)
						taskJourneyQuery += " union "
								+ "  select y.fromID as fromID,x.fromID as toID,'claimed' as action,y.action_date as action_date,0 as duration from (select user_id_  as fromID, substring(message_, 0,position('|'  IN message_)-1) as toID,time_ from ACT_HI_COMMENT where task_id_='"
								+ taskID
								+ "'  and action_='AddUserLink'  group by user_id_,substring(message_, 0,position('|'  IN message_)-1),time_ order by time_ asc limit 1) as x, "
								+ " (  select  ari.group_id_ as fromID,art.assignee_ as toID,'claimed' as action,CASE WHEN art.claim_time_ IS NULL THEN art.start_time_ ELSE art.claim_time_ END as action_date,0 as duration  from ACT_HI_TASKINST art,ACT_HI_IDENTITYLINK ari where art.id_='"
								+ taskID
								+ "'  and art.claim_time_ IS NOT NULL and ari.task_id_=art.id_ and  (ari.type_='candidate' )  ) as y ";
					else
						taskJourneyQuery += " union "
								+ " ( select  ari.group_id_ as fromID,art.assignee_ as toID,'claimed' as action,CASE WHEN art.claim_time_ IS NULL THEN art.start_time_ ELSE art.claim_time_ END as action_date,0 as duration  from ACT_HI_TASKINST art,ACT_HI_IDENTITYLINK ari where art.id_='"
								+ taskID
								+ "'  and art.claim_time_ IS NOT NULL and ari.task_id_=art.id_ and  (ari.type_='candidate' ) ) ";
				}
				if (isReassigned) {

					if (!isAssignedToGroup)
						taskJourneyQuery += " union "
								+ "  select y.toID as fromID,x.fromID as toID,'assigned' as action,y.action_date as action_date,0 as duration from (select user_id_  as fromID, substring(message_, 0,position('|'  IN message_)-1) as toID,time_ from ACT_HI_COMMENT where task_id_='"
								+ taskID
								+ "'  and action_='AddUserLink'  group by user_id_,substring(message_, 0,position('|'  IN message_)-1),time_ order by time_ asc limit 1) as x, "
								+ " (  select CASE WHEN ari.user_id_ IS NULL THEN '" + admin
								+ "' else ari.user_id_ end as fromID,  CASE WHEN ari.user_id_ IS NULL THEN '" + admin
								+ "' else ari.user_id_ end as toID,'started' as action,art.start_time_ as action_date,0 as duration  from ACT_HI_TASKINST art,ACT_HI_IDENTITYLINK ari    where art.id_='"
								+ taskID
								+ "' and   ari.proc_inst_id_=art.proc_inst_id_ and ari.type_='starter'  ) as y ";

					taskJourneyQuery += " union "
							+ "   select  user_id_ as fromID,substring(message_, 0,position('|'  IN message_)-1) as toID,'reassigned' as action,time_ as action_date,'0' as duration from ACT_HI_COMMENT where task_id_='"
							+ taskID
							+ "' and action_='AddUserLink' and user_id_!=substring(message_, 0,position('|'  IN message_)-1) ";

				}
				if (!isReassigned && !isAssignedToGroup) {
					taskJourneyQuery += " union" + " select CASE WHEN ari.user_id_ IS NULL THEN '" + admin
							+ "' else ari.user_id_ end as fromID,art.assignee_ as toID,'assigned' as action,art.start_time_ as action_date,0 as duration  from ACT_HI_TASKINST art,ACT_HI_IDENTITYLINK ari where art.id_='"
							+ taskID
							+ "' and   (ari.proc_inst_id_=art.proc_inst_id_ or ari.task_id_=art.id_)  and (ari.type_='starter' ) ";
				}
				taskJourneyQuery += " union "
						+ "  select CASE WHEN art.end_time_ IS NOT NULL THEN art.assignee_ END as fromID,CASE WHEN art.end_time_ IS NOT NULL THEN art.assignee_ END as toID,'completed' as action,CASE WHEN art.end_time_ IS NOT NULL THEN art.end_time_  END as action_date,CASE WHEN art.end_time_ IS NOT NULL AND art.claim_time_ IS NOT NULL THEN DATEDIFF( art.end_time_, art.claim_time_)  "
						+ " WHEN art.end_time_ IS NOT NULL AND art.claim_time_ IS NULL THEN DATEDIFF( art.end_time_, art.start_time_) "
						+ " ELSE 0 END as duration  from ACT_HI_TASKINST art where art.id_='" + taskID
						+ "' and art.delete_reason_='completed' order by action_date asc "
						+ " ) as taskjourney order by action_date asc ,action desc ";

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Task Journey QUERY: \n " + taskJourneyQuery);
				}
				ResultSet rs3 = connection.createStatement().executeQuery(taskJourneyQuery);
				JSONArray jsonArray = new JSONArray();
				while (rs3.next()) {
					JSONObject obj = new JSONObject();
					String firstNameQuery = " ", lastNameQuery = " ";
					obj.put("from", rs3.getString("fromID"));
					obj.put("to", rs3.getString("toID"));
					obj.put("action", rs3.getString("action"));
					obj.put("action_date", rs3.getString("action_date"));
					obj.put("duration", rs3.getString("duration"));
					firstNameQuery = " select a.first_ as fromID_first,a.last_ as fromID_last,case when substring(a.first_,1,2) is null then substring(a.last_,1,2) else concat(substring(a.last_,1,1),substring(a.first_,1,1)) end as fromID_circle from ACT_ID_USER a where a.id_='"
							+ rs3.getString("fromID") + "' ";
					lastNameQuery = " select b.first_ as toID_first,b.last_ as toID_last,case when substring(b.first_,1,2) is null then substring(b.last_,1,2) else concat(substring(b.last_,1,1),substring(b.first_,1,1)) end as toID_circle from ACT_ID_USER b where b.id_='"
							+ rs3.getString("toID") + "' ";

					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("First Name QUERY: \n " + firstNameQuery);
					}
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Last Name QUERY: \n " + lastNameQuery);
					}
					ResultSet rs4 = connection.createStatement().executeQuery(firstNameQuery);

					ResultSet rs5 = connection.createStatement().executeQuery(lastNameQuery);

					if (rs4.next()) {
						obj.put("from_first", rs4.getString("fromID_first"));
						obj.put("from_last", rs4.getString("fromID_last"));
						obj.put("from_circle", rs4.getString("fromID_circle"));
					} else {
						obj.put("from_first", rs3.getString("fromID"));
						obj.put("from_last", "");
						obj.put("from_circle", rs3.getString("fromID").substring(0, 2));
					}
					if (rs5.next()) {
						obj.put("to_first", rs5.getString("toID_first"));
						obj.put("to_last", rs5.getString("toID_last"));
						obj.put("to_circle", rs5.getString("toID_circle"));
					} else {
						obj.put("to_first", rs3.getString("toID"));
						obj.put("to_last", "");
						obj.put("to_circle", rs3.getString("toID").substring(0, 2));
					}

					// Get the count of actions performed on task
					String fromCntActionQuery = "", toCntActionQuery = "";
					fromCntActionQuery = "select count(*) as no_of_actions from ACT_HI_COMMENT where task_id_='"
							+ taskID + "' and user_id_='" + rs3.getString("fromID")
							+ "' and action_ not in ('DeleteUserLink') ";
					toCntActionQuery = "select count(*) as no_of_actions from ACT_HI_COMMENT where task_id_='" + taskID
							+ "' and user_id_='" + rs3.getString("toID") + "' and action_ not in ('DeleteUserLink') ";
					ResultSet rs6 = connection.createStatement().executeQuery(fromCntActionQuery);
					ResultSet rs7 = connection.createStatement().executeQuery(toCntActionQuery);
					if (rs6.next() && (rs6.getString("no_of_actions") != null
							&& !"".equalsIgnoreCase(rs6.getString("no_of_actions"))))
						obj.put("from_cnt_actions", rs6.getString("no_of_actions"));
					else
						obj.put("from_cnt_actions", 0);
					if (rs7.next() && (rs7.getString("no_of_actions") != null
							&& !"".equalsIgnoreCase(rs7.getString("no_of_actions"))))
						obj.put("to_cnt_actions", rs7.getString("no_of_actions"));
					else
						obj.put("to_cnt_actions", 0);

					jsonArray.put(obj);
					rs4.close();
					rs5.close();
				}
				rs3.close();
				result = jsonArray.toString();
			} catch (Exception e) {
				LOGGER.error("getTaskJourney() Exception: " + e.getMessage());
			} finally {
				if (connection != null) {
					try {
						connection.close();
					} catch (SQLException e) {
						LOGGER.error(e);
					}
				}
			}
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Result is " + result);
		}
		return result;
	}

	public boolean reassignTask(String taskID, String fromUserID, String toUserID) {
		Connection connection = null;
		try {
			connection = activitiDataSource.getConnection();
			Statement stmt = null;

			Random rnd = new Random();
			int n = 100000 + rnd.nextInt(900000);
			String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.s").format(new Date());
			String query = " insert into ACT_HI_COMMENT (id_, type_, time_, user_id_, task_id_, proc_inst_id_, action_, message_, full_msg_)"
					+ "  values('" + n + "','event','" + timeStamp + "','" + fromUserID + "','" + taskID
					+ "',null,'AddUserLink','" + toUserID + "_|_',null)";
			stmt = connection.createStatement();
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Reassign Task QUERY: \n " + query);
			}
			int rs = stmt.executeUpdate(query);
			if (rs > 0)
				return true;
		} catch (Exception e) {
			LOGGER.error("In updateRecord method catch block due to " + e);
			return false;
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					LOGGER.error(e);
				}
			}
		}
		return true;
	}

	public String generateTriageTasksByContentIDQuery(String contentID) {
		String query = "";
		if (contentID != null && !"".equalsIgnoreCase(contentID)) {
			query = "  select art.id_ as task_id from ACT_HI_TASKINST art where art.name_ like '" + contentID
					+ " - %'   and art.proc_def_id_ in (select id_ from ACT_RE_PROCDEF where key_='TriageTaskProcess' ) order by START_TIME_ desc  limit 1";
		}
		return query;
	}

	public String generateHierarchiesQuery(String tenantId,String brand,String name,String value,String columnNames,String type) {

		String query = " select ";
		String columns = " ", emptyChecks = "";
		;
		String[] colNames = columnNames.split(",");
		int countOfColumns = colNames.length;
		if (countOfColumns == 0)
			query = query + name;
		for (int i = 0; i < countOfColumns; i++) {
			columns = columns + colNames[i];
			emptyChecks = emptyChecks + " and " + colNames[i] + " != '' and " + colNames[i] + " != 'N/A' and " + " "
					+ colNames[i] + " IS NOT NULL ";
			if (i < countOfColumns - 1) {
				columns = columns + ",";
			}
		}
		query = query + columns + " as name FROM " + tenantId + "." + type + "_" + brand + " where 1=1";
		if (name != null && !"".equalsIgnoreCase(name) && value != null && !"".equalsIgnoreCase(value)) {
			query = query + " and " + name + " in( '" + value + "')";
		}
		if (emptyChecks != null && !"".equalsIgnoreCase(emptyChecks))
			query = query + emptyChecks;
		query = query + " group by " + columns;
		query = query + " order by " + columns + " asc ";

		return query;
	}

	public String updateFilterForUser(String tenantId, String userId, String brand, String filterName,
			String filterData, String defaultFilter) {
		String result = "{\"nps\":\"false\"}";
		;
		Connection connection = null;
		Statement stmt = null;
		try {
			connection = cxiqDataSource.getConnection();
			stmt = connection.createStatement();
			String updateQuery = "";
			String updateField = "";
			String query1 = "select * from " + tenantId + ".user_filter_map where user_id='" + userId + "'";
			if (brand != null && !"".equalsIgnoreCase(brand))
				query1 = query1 + " and brand='" + brand + "'";
			if (filterName != null && !"".equalsIgnoreCase(filterName))
				query1 = query1 + " and filter_name='" + filterName + "'";
			ResultSet rs1 = stmt.executeQuery(query1);
			filterData = filterData.replaceAll("'", "''");

			if (!rs1.next()) {
				updateQuery = "insert into  " + tenantId + ".user_filter_map " + " values ('" + userId + "','" + brand
						+ "','" + filterName + "','" + filterData + "','" + defaultFilter + "' )";

			} else {

				if (brand != null && !"".equalsIgnoreCase(brand))
					updateField = updateField + " brand='" + brand + "' ";
				if (filterName != null && !"".equalsIgnoreCase(filterName)) {
					if (!"".equalsIgnoreCase(updateField))
						updateField = updateField + ",";
					updateField = updateField + " filter_name='" + filterName + "' ";
				}

				if (filterData != null && !"".equalsIgnoreCase(filterData)) {
					if (!"".equalsIgnoreCase(updateField))
						updateField = updateField + ",";

					updateField = updateField + " filter_data='" + filterData + "' ";
				}
				if (defaultFilter != null && !"".equalsIgnoreCase(defaultFilter)
						&& "1".equalsIgnoreCase(defaultFilter)) {
					int rs2 = stmt.executeUpdate("update " + tenantId
							+ ".user_filter_map  set default_filter='0' where user_id='" + userId + "' ");
					if (rs2 < 0)
						return result;
					if (!"".equalsIgnoreCase(updateField))
						updateField = updateField + ",";
					updateField = updateField + " default_filter='" + defaultFilter + "' ";
				}
				updateQuery = "update " + tenantId + ".user_filter_map  set " + updateField + " where user_id='"
						+ userId + "' ";
				if (filterName != null && !"".equalsIgnoreCase(filterName))
					updateQuery = updateQuery + " and filter_name='" + filterName + "'";
				if (brand != null && !"".equalsIgnoreCase(brand))
					updateQuery = updateQuery + " and brand='" + brand + "'";

			}

			int rs = -1;
			rs = stmt.executeUpdate(updateQuery);

			if (rs >= 0)
				result = "{\"nps\":\"true\"}";

		} catch (Exception e) {
			LOGGER.error("In updateLevelsForBrand method catch block due to " + e);
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					LOGGER.error(e);
				}
			}
		}
		return result;
	}

	public String deleteFilterForUser(String tenantId, String userId, String brand, String filterName) {

		String result = "{\"nps\":\"false\"}";
		;
		Connection connection = null;
		Statement stmt = null;
		int rs = -1;
		try {
			connection = cxiqDataSource.getConnection();
			stmt = connection.createStatement();
			String updateQuery = " delete from " + tenantId + ".user_filter_map where user_id='" + userId
					+ "' and brand='" + brand + "' and filter_name='" + filterName + "'";
			rs = stmt.executeUpdate(updateQuery);
			if (rs >= 0)
				result = "{\"nps\":\"true\"}";

		} catch (Exception e) {
			LOGGER.error("In deleteFilterForUser method catch block due to " + e);
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					LOGGER.error(e);
				}
			}
		}
		return result;
	}

}
