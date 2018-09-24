package com.cxiq.triage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.ResourceBundle;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.delegate.JavaDelegate;
import org.apache.log4j.Logger;

import com.cxiq.constants.CXIQConstants;
import com.cxiq.drools.CXIQDroolsService;
import com.cxiq.drools.Group;
import com.cxiq.service.utils.DBUtils;

public class UpdateTriageRecord implements JavaDelegate {

	CXIQDroolsService cxiqd;

	DBUtils dbUtils;

	static final Logger LOGGER = Logger.getLogger(UpdateTriageRecord.class.getName());

	private Expression statusVal;

	public void execute(DelegateExecution execution) throws Exception {
		@SuppressWarnings("unchecked")
		Map<String, String> idMap = (Map<String, String>) execution.getVariable("id");

		String tenantId = execution.getTenantId();
		execution.setVariable("tenantId", tenantId);
		String id = idMap.get("id");
		String content = idMap.get("content");
		String nps_score = idMap.get("nps_score");
		String status = (String) statusVal.getValue(execution);
		String brand = (String) execution.getVariable("brand");
		if (brand == null || "".equalsIgnoreCase(brand))
			brand = "decooda";
		boolean result = updateRecord(tenantId, brand, id, content, nps_score, status);

		// Fire drools and get the group
		if ("Processed".equalsIgnoreCase(status)) {
			String groupId = getGroupByContentDetails(tenantId, brand, idMap);
			idMap.put("groupId", groupId);
			String groupName = getGroupName(tenantId, brand, groupId);
			idMap.put("groupName", groupName);
		}
		execution.setVariable("result", result);
	}

	private String getGroupByContentDetails(String tenantId, String brand, Map<String, String> idMap) {
		String groupId = "";
		com.cxiq.drools.Group group = new Group();

		group.setAge(idMap.get("age"));
		group.setAuthor(idMap.get("author"));
		group.setAuthor_link(idMap.get("author_link"));
		group.setAuthor_location(idMap.get("author_location"));
		group.setContent(idMap.get("content"));
		group.setCountry(idMap.get("country"));
		group.setDate(idMap.get("date"));
		group.setDistrict(idMap.get("district"));
		group.setExtractor_name(idMap.get("extractor_name"));
		group.setLocation(idMap.get("location"));
		group.setNps_score(Integer.parseInt(idMap.get("nps_score")));
		group.setOverall_rating(idMap.get("overall_rating"));
		group.setOwnership(idMap.get("ownership"));
		group.setPage_no(idMap.get("page_no"));
		group.setProduct(idMap.get("product"));
		group.setRegion(idMap.get("region"));
		group.setReview(idMap.get("review"));
		group.setRoom_cleanliness_rating(idMap.get("room_cleanliness_rating"));
		group.setRoom_comfort_rating(idMap.get("room_comfort_rating"));
		group.setSku(idMap.get("sku"));
		group.setStaff_service_rating(idMap.get("staff_service_rating"));
		group.setState(idMap.get("state"));
		group.setTitle(idMap.get("title"));
		group.setTraveler_type(idMap.get("traveler_type"));
		group.setDate(idMap.get("date"));
		group.setUrl(idMap.get("url"));

		cxiqd = new CXIQDroolsService();
		groupId = cxiqd.getGroup(tenantId, brand, group);

		return groupId;
	}

	private String getGroupName(String tenantId, String brandName, String groupId) {
		dbUtils = new DBUtils();
		Connection connection = null;
		Statement stmt = null;
		String groupName = "";

		try {
			connection = getCXIQConnection();
			// String npsTable=DBUtils.getNPSTable(tenantId,brandName);
			stmt = connection.createStatement();

			if (stmt != null) {
				String query = " select * from " + tenantId + ".brand_groups_map where brand='" + brandName
						+ "' and group_id='" + groupId + "' ";
				ResultSet rs = stmt.executeQuery(query);
				if (rs.next())
					groupName = rs.getString("group_name");
			}
		} catch (Exception e) {
			LOGGER.error("In updateRecord method catch block due to " + e);
		} finally {
			dbUtils.closeStatement(stmt);
			dbUtils.closeConnection(connection);

		}
		return groupName;

	}

	/**
	 * Updates triage status for message in NPS table.
	 * 
	 * @param tenantId
	 * @param id
	 * @param content
	 * @param nps_score
	 * @param status
	 * @return {@link Boolean}
	 */
	public boolean updateRecord(String tenantId, String brandName, String id, String content, String nps_score,
			String status) {
		dbUtils = new DBUtils();
		Connection connection = null;
		Statement stmt = null;

		try {
			connection = getCXIQConnection();
			String npsTable = getCXIQTable(tenantId, brandName);
			stmt = connection.createStatement();

			if (stmt != null) {
				String query = " update " + npsTable + " set triage_status = '" + status + "'  where sourcedata_id='"
						+ id + "' ";
				stmt.executeUpdate(query);
			}
		} catch (Exception e) {
			LOGGER.error("In updateRecord method catch block due to " + e);
			return false;
		} finally {
			dbUtils.closeStatement(stmt);
			dbUtils.closeConnection(connection);

		}
		return true;
	}

	public static Connection getCXIQConnection() {
		ResourceBundle rb = ResourceBundle.getBundle("application");
		Connection connection = null;
		String driverClass = rb.getString("datasource.cxiq.driver-class-name");
		String dbUserName = rb.getString("datasource.cxiq.username");
		String dbPassword = rb.getString("datasource.cxiq.password");
		String dbUrl = rb.getString("datasource.cxiq.url");

		try {
			Class.forName(driverClass);
			connection = DriverManager.getConnection(dbUrl, dbUserName, dbPassword);
		} catch (Exception e) {
			LOGGER.error("In getCXIQConnection method catch block due to " + e);
			System.out.println("In getCXIQConnection method catch block due to " + e);
		}
		return connection;

	}

	public String getCXIQTable(String tenantId, String brand) {
		String DEFAULT_TABLE = "";
		String npsTable = "";
		Connection connection = null;
		if (brand != null && !"".equalsIgnoreCase(brand)) {
			DEFAULT_TABLE = brand;
		} else {
			String query = "select * from " + tenantId + ".user_brand_map where user_id= '" + tenantId + "'";
			try {
				connection = getCXIQConnection();
				ResultSet rs = connection.createStatement().executeQuery(query);

				if (rs.next()) {
					DEFAULT_TABLE = rs.getString("default_brand");
				}
				rs.close();
			} catch (SQLException e) {
				LOGGER.error("Error while creating preparedstatement: " + e.getMessage());
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
		if (tenantId == null || "".equalsIgnoreCase(tenantId)) {
			tenantId = "decooda";
		}
		npsTable = tenantId + "." + CXIQConstants.DECOODA_TABLE_PREFIX + DEFAULT_TABLE;

		return npsTable;
	}
}