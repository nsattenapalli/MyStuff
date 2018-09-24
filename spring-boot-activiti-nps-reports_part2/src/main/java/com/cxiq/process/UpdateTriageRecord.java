package com.cxiq.process;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;

import javax.sql.DataSource;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.delegate.JavaDelegate;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.cxiq.drools.CXIQDroolsService;
import com.cxiq.drools.Group;
import com.cxiq.service.utils.DBUtils;

public class UpdateTriageRecord implements JavaDelegate {

	@Qualifier("cxiq")
	@Autowired
	DataSource cxiqDataSource;

	@Autowired
	DBUtils dbUtils;

	@Autowired
	CXIQDroolsService npsd;

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

		groupId = npsd.getGroup(tenantId, brand, group);

		return groupId;
	}

	private String getGroupName(String tenantId, String brandName, String groupId) {
		Connection connection = null;
		Statement stmt = null;
		String groupName = "";

		try {
			connection = cxiqDataSource.getConnection();
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

		Connection connection = null;
		Statement stmt = null;

		try {
			connection = cxiqDataSource.getConnection();
			String npsTable = dbUtils.getCXIQTable(tenantId, brandName);
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
}