package com.cxiq.service.cxiq;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cxiq.constants.CXIQConstants;
import com.cxiq.service.activiti.ActivitiRestClientService;
import com.cxiq.service.utils.DBUtils;

@Service
public class CXIQAdminService {

	static final Logger LOGGER = Logger.getLogger(CXIQAdminService.class.getName());

	@Qualifier("cxiq")
	@Autowired
	DataSource cxiqDatasource;

	@Qualifier("activiti")
	@Autowired
	DataSource activitiDatasource;

	@Autowired
	DBUtils dbUtils;

	@Autowired
	ActivitiRestClientService activitiClient;

	@Autowired
	AccountService accountService;

	@Value("${cxiq.admin}")
	String cxiqAdmin;

	@Value("${cxiq.admin.password}")
	String cxiqAdminPassword;

	public String createTenant(String tenantId, String tenantName) {
		String result = "false";
		if (tenantId == null || "".equalsIgnoreCase(tenantId))
			return "false";
		if (tenantName == null || "".equalsIgnoreCase(tenantName))
			tenantName = tenantId;

		try {

			boolean response = false;
			String dir = CXIQAdminService.class.getClassLoader().getResource("").getPath();
			HashMap<String, String> variables = new HashMap<String, String>();
			variables.put("$db", tenantId);
			response = dbUtils.executeSQLFile(variables, dir + CXIQConstants.CREATE_TENANT_SQL_FILE, "cxiq");
			if (response)
				response = dbUtils.executeSQLFile(variables, dir + CXIQConstants.IMPORT_TENANT_DATA_ACTIVITI_SQL_FILE,
						"activiti");
			if (response) {
				String filePath = dir + "//NPS.zip";
				if (activitiClient.deployProcesses(tenantId, filePath))
					result = "true";
				else
					return result;
			}
			if (!response || "false".equalsIgnoreCase(result)) {
				deleteTenant(tenantId);
			}
		} catch (Exception e) {
			result = "false";
		}
		return result;
	}

	public boolean checkIfTenantAlreadyExists(String tenantId) {
		boolean result = false;
		Connection connection = null;
		Statement stmt = null;
		String query = "";
		try {
			connection = activitiDatasource.getConnection();
			query = "select * from ACT_ID_GROUP where ID_='nps_tenant_" + tenantId + "'";
			stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			if (rs.next())
				result = true;
		} catch (Exception e) {
			LOGGER.error("In checkIfTenantExists method catch block due to " + e);
		} finally {
			dbUtils.closeStatement(stmt);
			dbUtils.closeConnection(connection);

		}
		return result;
	}

	public String deleteTenant(String tenantId) {
		String deleted = "false";
		Connection connection = null;
		Statement stmt = null;

		try {
			connection = activitiDatasource.getConnection();
			connection.setAutoCommit(false);
			stmt = connection.createStatement();
			String users = "";
			String getUserQuery = "select distinct USER_ID_ from ACT_ID_MEMBERSHIP where GROUP_ID_ like 'nps_tenant_"
					+ tenantId + "' or GROUP_ID_ like 'nps_role_" + tenantId + "_%' or GROUP_ID_ like 'nps_group_"
					+ tenantId + "_%' ";
			ResultSet rs = stmt.executeQuery(getUserQuery);
			while (rs.next()) {
				String user = rs.getString("USER_ID_");
				// cxiqAuth.removeUser(user);
				if ("".equalsIgnoreCase(users)) {
					users = users + "'" + user + "'";
				} else
					users = users + ",'" + user + "'";
			}
			String tenantSchemaQuery = " DROP DATABASE IF EXISTS " + tenantId;
			String userGroupQuery = "Delete from ACT_ID_MEMBERSHIP where GROUP_ID_ like 'nps_tenant_" + tenantId
					+ "' or GROUP_ID_ like 'nps_role_" + tenantId + "_%' or GROUP_ID_ like 'nps_group_" + tenantId
					+ "_%' ";
			String groupQuery = "Delete from ACT_ID_GROUP where ID_ like 'nps_tenant_" + tenantId
					+ "' or ID_ like 'nps_role_" + tenantId + "_%' or ID_ like 'nps_group_" + tenantId + "_%' ";

			stmt.addBatch(tenantSchemaQuery);
			stmt.addBatch(userGroupQuery);
			if (users != null && !"".equalsIgnoreCase(users)) {
				String userQuery = "Delete from ACT_ID_USER where ID_ IN (" + users + ") ";

				stmt.addBatch(userQuery);

			}

			stmt.addBatch(groupQuery);

			stmt.executeBatch();
			connection.commit();

			if (!deleteActivitiData(tenantId))
				return "false";
			if (!accountService.updateAccountDetails("", tenantId, "", "Deleted"))
				return "false";

			deleted = "true";
		} catch (Exception e) {
			deleted = "false";
			try {
				connection.rollback();
			} catch (SQLException e1) {
				LOGGER.error("In deleteBrand method catch block due to " + e);
			}
			LOGGER.error("In deleteBrand method catch block due to " + e);
		} finally {
			dbUtils.closeStatement(stmt);
			dbUtils.closeConnection(connection);
		}
		return deleted;

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

	public String updateTriageStatusForMessages(String tenantId, String brand, String status, int limit) {
		String result = "{\"nps\":\"false\"}";
		Connection connection = null;
		Statement stmt = null;
		try {
			connection = cxiqDatasource.getConnection();
			String cxiqTable = dbUtils.getCXIQTable(tenantId, brand);
			stmt = connection.createStatement();

			String updateQuery = " update " + cxiqTable;
			if (status == null || "".equalsIgnoreCase(status))
				updateQuery += " set triage_status = NULL ";
			else
				updateQuery += " set triage_status = '" + status + "' ";

			if (limit != 0)
				updateQuery += " limit " + limit;
			else
				updateQuery += " limit 20 ";
			int rs1 = stmt.executeUpdate(updateQuery);

			if (rs1 >= 0)
				result = "{\"nps\":\"true\"}";

		} catch (Exception e) {
			LOGGER.error("In updateTriageStatusForMessages method catch block due to " + e);
			result = "{\"nps\":\"false\"}";
		} finally {
			dbUtils.closeStatement(stmt);
			dbUtils.closeConnection(connection);

		}
		return result;
	}
}
