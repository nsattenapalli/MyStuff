package com.cxiq;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.cxiq.service.utils.DBUtils;

public class TriageProcess implements JavaDelegate {

	@Autowired
	DBUtils dbUtils;

	@Qualifier("cxiq")
	@Autowired
	DataSource cxiqDataSource;

	static final Logger LOGGER = Logger.getLogger(TriageProcess.class.getName());

	public void execute(DelegateExecution execution) throws Exception {
		String tenantId = execution.getTenantId();
		execution.setVariable("tenantId", tenantId);
		String brand = (String) execution.getVariable("brand");
		Map<String, Map<String, String>> idMap = new HashMap<String, Map<String, String>>();
		idMap = getCXIQData(tenantId, brand);
		List<Map<String, String>> idMapList = new ArrayList<Map<String, String>>(idMap.values());
		execution.setVariable("idMap", idMapList);
	}

	/**
	 * 
	 * @param tenantId
	 * @return Map with the list of messsages from CXIQ table
	 */
	public Map<String, Map<String, String>> getCXIQData(String tenantId, String brand) {

		Map<String, Map<String, String>> idMap = new HashMap<String, Map<String, String>>();

		Connection connection = null;
		Statement stmt = null;
		ResultSet rs = null;

		try {

			String cxiqTable = dbUtils.getCXIQTable(tenantId, brand);
			connection = cxiqDataSource.getConnection();
			stmt = connection.createStatement();

			if (stmt != null) {
				String query = "select * from " + cxiqTable + " where triage_status IS NULL ";

				// stmt.setMaxRows(100000);
				rs = stmt.executeQuery(query);
				int i = 1;
				while (rs.next()) {

					String contentID = rs.getString("sourcedata_id");
					// String primaryEmotionConf = rs.getString("primary_emotion_confidence");
					// String valenceConfidence = rs.getString("valence_confidence");
					// String valenceDirection = rs.getString("valence_direction");
					String npsScore = rs.getString("sourcedata_nps_score");
					String content = rs.getString("content");
					String userID = rs.getString("user_id");

					String extractorName = rs.getString("sourcedata_extractor_name");
					String ownership = rs.getString("sourcedata_ownership");
					String country = rs.getString("sourcedata_country");
					String region = rs.getString("sourcedata_region");
					String state = rs.getString("sourcedata_state");
					String district = rs.getString("sourcedata_district");
					String location = rs.getString("sourcedata_location");
					String product = rs.getString("sourcedata_product");
					String sku = rs.getString("sourcedata_sku");
					String url = rs.getString("sourcedata_url");
					String date = rs.getString("sourcedata_date");
					String overallRating = rs.getString("sourcedata_overall_rating");
					String staffServiceRating = rs.getString("sourcedata_staff_service_rating");
					String roomComfortRating = rs.getString("sourcedata_room_comfort_rating");
					String valueRating = rs.getString("sourcedata_value_rating");
					String roomCleanlinessRating = rs.getString("sourcedata_room_cleanliness_rating");
					String title = rs.getString("sourcedata_title");
					String review = rs.getString("sourcedata_review");
					String author = rs.getString("sourcedata_author");
					String authorLink = rs.getString("sourcedata_author_link");
					String authorLocation = rs.getString("sourcedata_author_location");
					String age = rs.getString("sourcedata_age");
					String travelerType = rs.getString("sourcedata_traveler_type");
					String pageNo = rs.getString("sourcedata_page_no");

					Map<String, String> variableMap = new HashMap<String, String>();
					if (contentID != null && !contentID.equals("")) {
						variableMap.put("id", contentID);
					}
					if (npsScore != null && !npsScore.equals(""))
						variableMap.put("nps_score", npsScore);
					if (content != null && !content.equals(""))
						variableMap.put("content", content);
					if (userID != null && !userID.equals(""))
						variableMap.put("user_id", userID);
					if (extractorName != null && !extractorName.equals(""))
						variableMap.put("extractor_name", extractorName);
					if (ownership != null && !ownership.equals(""))
						variableMap.put("ownership", ownership);
					if (country != null && !country.equals(""))
						variableMap.put("country", country);
					if (region != null && !region.equals(""))
						variableMap.put("region", region);
					if (state != null && !state.equals(""))
						variableMap.put("state", state);
					if (district != null && !district.equals(""))
						variableMap.put("district", district);
					if (location != null && !location.equals(""))
						variableMap.put("location", location);
					if (product != null && !product.equals(""))
						variableMap.put("product", product);
					if (sku != null && !sku.equals(""))
						variableMap.put("sku", sku);
					if (url != null && !url.equals(""))
						variableMap.put("url", url);
					if (date != null && !date.equals(""))
						variableMap.put("date", date);
					if (overallRating != null && !overallRating.equals(""))
						variableMap.put("overall_rating", overallRating);
					if (staffServiceRating != null && !staffServiceRating.equals(""))
						variableMap.put("staff_service_rating", staffServiceRating);
					if (roomComfortRating != null && !roomComfortRating.equals(""))
						variableMap.put("room_comfort_rating", roomComfortRating);
					if (valueRating != null && !valueRating.equals(""))
						variableMap.put("value_rating", valueRating);
					if (roomCleanlinessRating != null && !roomCleanlinessRating.equals(""))
						variableMap.put("room_cleanliness_rating", roomCleanlinessRating);
					if (title != null && !title.equals(""))
						variableMap.put("title", title);
					if (review != null && !review.equals(""))
						variableMap.put("review", review);
					if (author != null && !author.equals(""))
						variableMap.put("author", author);
					if (authorLink != null && !authorLink.equals(""))
						variableMap.put("author_link", authorLink);
					if (authorLocation != null && !authorLocation.equals(""))
						variableMap.put("author_location", authorLocation);
					if (age != null && !age.equals(""))
						variableMap.put("age", age);
					if (travelerType != null && !travelerType.equals(""))
						variableMap.put("traveler_type", travelerType);
					if (pageNo != null && !pageNo.equals(""))
						variableMap.put("page_no", pageNo);
					variableMap.put("group", "");
					idMap.put(Integer.toString(i), variableMap);
					++i;

				}
				rs.close();
				stmt.close();
			}
		} catch (Exception e) {
			LOGGER.error("In getNPSData method catch block due to " + e);
		} finally {

			if (connection != null)
				try {
					connection.close();
				} catch (SQLException e) {
					LOGGER.error(e);
				}

		}
		return idMap;
	}
}