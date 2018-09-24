package com.cxiq.service.cxiq;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cxiq.constants.CXIQConstants;
import com.cxiq.drools.CXIQDroolsService;
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

	@Autowired
	CXIQDroolsService cxiqDrools;

	@Value("${cxiq.admin}")
	String cxiqAdmin;

	@Value("${cxiq.admin.password}")
	String cxiqAdminPassword;

	public String assignBrandsToUser(String tenantId, String userId, String brands, String defaultBrand) {
		String result = "";
		Connection connection = null;
		Statement stmt = null;
		if (userId == null && "".equalsIgnoreCase(userId))
			return "false";

		try {
			connection = cxiqDatasource.getConnection();
			stmt = connection.createStatement();
			String brandList = "", query = "";
			String query1 = "select * from " + tenantId + ".user_brand_map where user_id='" + userId + "'";
			ResultSet rs = stmt.executeQuery(query1);
			if (rs.next()) {
				brandList = rs.getString("brands");
				if (brandList == null)
					brandList = "";
				if (brands != null && !brandList.contains(brands)) {
					if (brandList != null && !"".equalsIgnoreCase(brandList)) {
						brandList = brandList + "," + brands;
						query = "update " + tenantId + ".user_brand_map set brands='" + brandList + "' where user_id='"
								+ userId + "'";
					} else
						query = "update " + tenantId + ".user_brand_map set brands='" + brands + "' where user_id='"
								+ userId + "'";
				} else
					return "false";
			} else {
				brandList = brands;
				query = " Insert into " + tenantId + ".user_brand_map values('" + userId + "','" + brandList + "','"
						+ brandList + "')";
			}

			int r = stmt.executeUpdate(query);
			if (r <= 0)
				return "false";
			else
				result = "true";
			if (defaultBrand != null && !"".equalsIgnoreCase(defaultBrand)) {
				query = "update " + tenantId + ".user_brand_map set default_brand='" + defaultBrand
						+ "' where user_id='" + userId + "'";
				int r1 = stmt.executeUpdate(query);
				if (r1 <= 0)
					result = "false";
			}

		} catch (Exception e) {
			LOGGER.error("In createBrand method catch block due to " + e);
			return "false";
		} finally {
			dbUtils.closeStatement(stmt);
			dbUtils.closeConnection(connection);

		}
		return result;
	}

	public String deleteBrandsForUser(String tenantId, String userId, String brands) {
		String result = "";
		Connection connection = null;
		Statement stmt = null;
		if (userId == null && "".equalsIgnoreCase(userId))
			return "false";
		try {
			connection = cxiqDatasource.getConnection();
			stmt = connection.createStatement();
			String brandList = "", query = "";
			String query1 = "select * from " + tenantId + ".user_brand_map where user_id='" + userId + "'";
			ResultSet rs = stmt.executeQuery(query1);
			if (rs.next())
				brandList = rs.getString("brands");
			if (brandList.contains(brands)) {
				if (brandList.contains(brands + ","))
					brandList = brandList.replace(brands + ",", "");
				else if (brandList.contains("," + brands))
					brandList = brandList.replace("," + brands, "");
				else
					brandList = brandList.replace(brands, "");
				query = "update " + tenantId + ".user_brand_map set brands='" + brandList + "' where user_id='" + userId
						+ "'";
				int r1 = stmt.executeUpdate(query);
				if (r1 <= 0)
					result = "false";
				else
					result = "true";
			}
		} catch (Exception e) {
			LOGGER.error("In deleteBrand method catch block due to " + e);
			return "false";
		} finally {
			dbUtils.closeStatement(stmt);
			dbUtils.closeConnection(connection);

		}
		return result;
	}

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
				//cxiqAuth.removeUser(user);
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

	public String getTenants() {
		Connection connection = null;
		Statement stmt = null, stmt2 = null;
		String result = "{\"nps\":0}";
		try {
			connection = activitiDatasource.getConnection();
			stmt = connection.createStatement();
			String tenantsQuery = "select SUBSTRING(ID_,12) as tenant,NAME_ from ACT_ID_GROUP where ID_ like 'nps_tenant_%'";
			JSONArray array = new JSONArray();

			ResultSet rs = stmt.executeQuery(tenantsQuery);
			while (rs.next()) {
				String tenantId = rs.getString("tenant");
				String tenantName = rs.getString("NAME_");
				String adminUsers = "";
				String adminsQuery = "SELECT GROUP_ID_,GROUP_CONCAT(USER_ID_) as ADMIN"
						+ " FROM ACT_ID_MEMBERSHIP where GROUP_ID_='nps_role_" + tenantId + "_admin' "
						+ " GROUP BY GROUP_ID_";
				stmt2 = connection.createStatement();
				ResultSet rs2 = stmt2.executeQuery(adminsQuery);
				if (rs2.next())
					adminUsers = rs2.getString("ADMIN");
				rs2.close();
				String defBrandQuery = "select * from " + tenantId + ".user_brand_map where user_id='" + tenantId + "'";
				String defaultBrand = "";
				ResultSet rs3 = stmt2.executeQuery(defBrandQuery);
				if (rs3.next())
					defaultBrand = rs3.getString("default_brand");
				rs3.close();
				String brandsQuery = " SELECT GROUP_CONCAT(SUBSTRING(TABLE_NAME,25)) AS BRANDS FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = '"
						+ tenantId + "' and  TABLE_NAME like '" + CXIQConstants.DECOODA_TABLE_PREFIX + "%' and "
						+ " TABLE_NAME NOT like '" + CXIQConstants.DECOODA_TABLE_PREFIX + "%_analysis'";
				String brands = "";
				ResultSet rs4 = stmt2.executeQuery(brandsQuery);
				if (rs4.next())
					brands = rs4.getString("BRANDS");
				rs4.close();
				JSONObject obj = new JSONObject();
				obj.put("tenant_id", tenantId);
				obj.put("tenant_name", tenantName);
				if (adminUsers == null)
					adminUsers = "";
				obj.put("admin_users", adminUsers);
				if (defaultBrand == null)
					defaultBrand = "";
				obj.put("default_brand", defaultBrand);
				if (brands == null)
					brands = "";
				obj.put("brands", brands);
				array.put(obj);
			}
			result = array.toString();

		} catch (Exception e) {
			LOGGER.error("In createBrand method catch block due to " + e);
		} finally {
			dbUtils.closeStatement(stmt2);
			dbUtils.closeStatement(stmt);

			dbUtils.closeConnection(connection);

		}
		return result;
	}

	public String updateLoginStatusOfUser(String tenantId, String userId, String brand, String status) {
		String result = "{\"nps\":\"false\"}";
		;
		Connection connection = null;
		Statement stmt = null;
		String updateQuery = "", query = "";
		try {
			connection = cxiqDatasource.getConnection();
			stmt = connection.createStatement();
			query = " select user_id from  " + tenantId + ".user_login_status where user_id='" + userId + "'";
			if (brand != null && !"".equalsIgnoreCase(brand))
				query = query + " and brand='" + brand + "'";

			ResultSet rs = stmt.executeQuery(query);
			if (!rs.next()) {
				updateQuery = "insert into  " + tenantId + ".user_login_status " + " values ( " + "'" + userId + "','"
						+ brand + "','" + status + "' )";
			} else {
				updateQuery = "update " + tenantId + ".user_login_status " + " set status='" + status
						+ "' where user_id='" + userId + "' and brand='" + brand + "'";
			}
			int rs1 = stmt.executeUpdate(updateQuery);
			if (rs1 >= 0)
				result = "{\"nps\":\"true\"}";

		} catch (Exception e) {
			LOGGER.error("In updateLoginStatusOfUser method catch block due to " + e);
		} finally {
			dbUtils.closeStatement(stmt);
			dbUtils.closeConnection(connection);

		}
		return result;
	}

	public String uploadFileForBrand(String tenantId, String brand, InputStream fileInputStream,
			FormDataContentDisposition fileInputDetails) {
		String result = "{\"nps\":\"false\"}";
		Connection connection = null;
		PreparedStatement stmt = null;
		Statement stmt1 = null;
		String query = "", query1 = "";
		try {
			connection = cxiqDatasource.getConnection();
			query1 = "select brand from " + tenantId + ".brand_image_map where brand='" + brand + "'";
			stmt1 = connection.createStatement();
			ResultSet rs = stmt1.executeQuery(query1);
			if (rs.next()) {
				query = "update  " + tenantId + ".brand_image_map  set filename= ?" + " ,content=? where brand=?";
				stmt = connection.prepareStatement(query);
				stmt.setString(1, fileInputDetails.getFileName());
				stmt.setBinaryStream(2, fileInputStream);
				stmt.setString(3, brand);
			} else {
				query = "insert into  " + tenantId + ".brand_image_map  values(?,?,?,?,?,?)";
				stmt = connection.prepareStatement(query);
				stmt.setString(1, brand);
				stmt.setString(2, "");
				stmt.setString(3, "");
				stmt.setString(4, "");
				stmt.setString(5, fileInputDetails.getFileName());
				stmt.setBinaryStream(6, fileInputStream);

			}
			int rs1 = stmt.executeUpdate();
			if (rs1 >= 0)
				result = "{\"nps\":\"true\"}";
		} catch (Exception e) {
			LOGGER.error("In uploadFileForBrand method catch block due to " + e);
		} finally {
			dbUtils.closeStatement(stmt);
			dbUtils.closeConnection(connection);

		}
		return result;
	}

	public byte[] downloadImageForBrand(String tenantId, String brand) {

		Connection connection = null;
		Statement stmt = null;
		String query = "";

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			connection = cxiqDatasource.getConnection();
			stmt = connection.createStatement();
			query = " select content from  " + tenantId + ".brand_image_map where brand='" + brand + "'";
			ResultSet rs1 = stmt.executeQuery(query);
			if (rs1.next()) {
				final InputStream in = rs1.getBinaryStream("content");

				int data = in.read();
				while (data >= 0) {
					out.write((char) data);
					data = in.read();
				}
				out.flush();
			}
		} catch (Exception e) {
			LOGGER.error("In downloadImageForBrand method catch block due to " + e);
		} finally {
			dbUtils.closeStatement(stmt);
			dbUtils.closeConnection(connection);

		}
		return out.toByteArray();
	}

	public String updateGroupInBrand(String tenantId, String brand, String groupId, String groupName, String rule) {
		String result = "{\"nps\":\"false\"}";
		;
		Connection connection = null;
		Statement stmt = null;
		String updateQuery = "", query = "";
		try {

			String drl = cxiqDrools.createDRL(tenantId, brand, rule, groupId);
			// Create entry in NPS
			connection = activitiDatasource.getConnection();
			stmt = connection.createStatement();
			query = " select * from  " + tenantId + ".brand_groups_map where brand='" + brand + "' and group_id='"
					+ groupId + "'";
			ResultSet rs = stmt.executeQuery(query);

			if (!rs.next()) {
				updateQuery = "insert into  " + tenantId + ".brand_groups_map " + " values ( " + "'" + brand + "','"
						+ groupId + "','" + groupName + "','" + rule + "' ,'" + drl + "')";
			} else {
				updateQuery = "update " + tenantId + ".brand_groups_map " + " set rule='" + rule + "' , drl='" + drl
						+ "' ";
				if (groupName != null && !"".equalsIgnoreCase(groupName))
					updateQuery = updateQuery + " , group_name='" + groupName + "' ";
				updateQuery = updateQuery + " where brand='" + brand + "' and group_id='" + groupId + "'";
			}
			int rs1 = stmt.executeUpdate(updateQuery);
			if (rs1 >= 0) {
				result = "{\"nps\":\"true\"}";
				String actGroupId = "nps_group_" + tenantId + "_" + brand + "_" + groupId;
				String groupQuery = " select * from ACT_ID_GROUP where ID_='" + actGroupId + "'";
				String updateGroupQuery = "";
				ResultSet rs2 = stmt.executeQuery(groupQuery);
				if (!rs2.next()) {
					updateGroupQuery = " insert into ACT_ID_GROUP values('" + actGroupId + "','1','" + groupName
							+ "','assignment') ";
				} else {
					if (groupName != null && !"".equalsIgnoreCase(groupName))
						updateGroupQuery = "update ACT_ID_GROUP set NAME_='" + groupName + "' where ID_='" + actGroupId
								+ "'";
				}
				if (!"".equalsIgnoreCase(updateGroupQuery)) {
					int rs4 = stmt.executeUpdate(updateGroupQuery);
					if (rs4 > 0)
						result = "{\"nps\":\"true\"}";
					else
						result = "{\"nps\":\"false\"}";
				}

			}

		} catch (Exception e) {
			LOGGER.error("In updateGroupForBrand method catch block due to " + e);
			result = "{\"nps\":\"false\"}";
		} finally {
			dbUtils.closeStatement(stmt);
			dbUtils.closeConnection(connection);

		}
		return result;
	}

	public String deleteGroupInBrand(String tenantId, String brand, String groupId) {
		String result = "{\"nps\":\"false\"}";
		;
		Connection connection = null;
		Statement stmt = null;
		try {

			connection = activitiDatasource.getConnection();
			stmt = connection.createStatement();
			String groupUsersQuery = " delete from ACT_ID_MEMBERSHIP where GROUP_ID_='nps_group_" + tenantId + "_"
					+ brand + "_" + groupId + "'";
			stmt.executeUpdate(groupUsersQuery);
			String groupQuery = " delete from ACT_ID_GROUP where ID_='nps_group_" + tenantId + "_" + brand + "_"
					+ groupId + "'";
			int rs1 = stmt.executeUpdate(groupQuery);

			if (rs1 >= 0) {
				String deleteQuery = " delete from " + tenantId + ".brand_groups_map where brand='" + brand
						+ "' and group_id='" + groupId + "'";
				int rs4 = stmt.executeUpdate(deleteQuery);
				if (rs4 > 0)
					result = "{\"nps\":\"true\"}";
			}

		} catch (Exception e) {
			LOGGER.error("In deleteGroupInBrand method catch block due to " + e);
			result = "{\"nps\":\"false\"}";
		} finally {
			dbUtils.closeStatement(stmt);
			dbUtils.closeConnection(connection);

		}
		return result;
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

	public boolean deleteUser(String tenantId, String userId) {
		boolean isDeleted = false;
		String response = "";

		response = activitiClient.invokeRestCall("/identity/users/" + userId, "", cxiqAdmin, cxiqAdminPassword,
				"DELETE");
		if (response.contains("204"))
			isDeleted = true;
		if (isDeleted) {
			Connection connection = null;
			Statement stmt = null;
			String query = "";
			try {
				connection = cxiqDatasource.getConnection();
				connection.setAutoCommit(false);
				stmt = connection.createStatement();
				query = "delete from " + tenantId + ".user_brand_map where user_id='" + userId + "'";
				stmt.addBatch(query);
				query = "delete from " + tenantId + ".user_filter_map where user_id='" + userId + "'";
				stmt.addBatch(query);
				query = "delete from " + tenantId + ".user_login_status where user_id='" + userId + "'";
				stmt.addBatch(query);

				stmt.executeBatch();
				connection.commit();

				// remove user from cache
				/*if (isDeleted)
					cxiqAuth.removeUser(userId);*/

			} catch (Exception e) {
				isDeleted = false;
				try {
					connection.rollback();
				} catch (SQLException e1) {
					LOGGER.error("In deleteUser method catch block due to " + e1);
				}
				LOGGER.error("In deleteUser method catch block due to " + e);
			} finally {
				dbUtils.closeStatement(stmt);
				dbUtils.closeConnection(connection);

			}
		}
		return isDeleted;
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

		response = activitiClient.invokeRestCall("repository/process-definitions?deploymentId=" + deploymentID,
				"", cxiqAdmin, cxiqAdminPassword, "GET");
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
					response = activitiClient.invokeRestCall("repository/deployments/" + deploymentID, "",
							cxiqAdmin, cxiqAdminPassword, "DELETE");
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

		response = activitiClient.invokeRestCall("runtime/process-instances?processDefinitionId=" + procDefID,
				"", cxiqAdmin, cxiqAdminPassword, "GET");
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

			response = activitiClient.invokeRestCall("history/historic-process-instances/" + procInstID, "",
					cxiqAdmin, cxiqAdminPassword, "DELETE");
			if (response.indexOf("204") > 0) {
				if (LOGGER.isDebugEnabled())
					LOGGER.debug("Deleted history process instance - " + procInstID);

				isDeleted = true;
			}
		}
		return isDeleted;
	}
}
