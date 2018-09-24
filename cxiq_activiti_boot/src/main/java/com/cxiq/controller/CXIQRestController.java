package com.cxiq.controller;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.cxiq.constants.CXIQConstants;
import com.cxiq.drools.CXIQDroolsService;
import com.cxiq.drools.Group;
import com.cxiq.service.cxiq.BrandService;
import com.cxiq.service.cxiq.CXIQAdminService;
import com.cxiq.service.cxiq.CXIQFilterService;
import com.cxiq.service.cxiq.ChatMessageService;
import com.cxiq.service.cxiq.TopicGroupService;
import com.cxiq.service.utils.DBUtils;
import com.cxiq.service.utils.QueryGeneratorService;
import com.cxiq.service.utils.Utils;
import com.cxiq.service.utils.ValueSortedMap;

@RestController
@RequestMapping(value = "/service")
public class CXIQRestController {
	static final Logger LOGGER = Logger.getLogger(CXIQRestController.class.getName());

	static ConcurrentHashMap<String, List<String>> columnsMap = new ConcurrentHashMap<String, List<String>>();

	private static final HashSet<String> POSITIVE_EMOTIONS = new HashSet<String>(
			Arrays.asList("emotion_happiness", "emotion_gratitude", "emotion_excitement", "emotion_crave"));
	private static final HashSet<String> NEGATIVE_EMOTIONS = new HashSet<String>(
			Arrays.asList("emotion_anger", "emotion_frustration", "emotion_not_happy", "emotion_disappointment"));

	@Qualifier("cxiq")
	@Autowired
	DataSource cxiqDataSource;

	@Autowired
	CXIQAdminService cxiqAdmin;

	@Autowired
	QueryGeneratorService qg;

	@Autowired
	Utils utils;

	@Autowired
	DBUtils dbUtils;

	@Autowired
	CXIQDroolsService cxiqDrools;

	@Autowired
	ChatMessageService chatMessages;

	@Autowired
	TopicGroupService topicGroups;

	@Autowired
	CXIQFilterService cxiqFilter;

	@Autowired
	BrandService brandService;

	@Value("${version}")
	String version;

	@RequestMapping(value = "/nps", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getNPS(@RequestParam("from") String fromDate, @RequestParam("to") String toDate,
			@RequestParam("tenantId") String tenantId, @RequestParam("brand") String brand,
			@RequestBody(required = false) String filter) {
		String npsTable = dbUtils.getCXIQTable(tenantId, brand);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Getting NPS value fromDate = " + fromDate + ", toDate = " + toDate);
		}

		String query = "select round(avg(sourcedata_nps_score),2) NPS from " + npsTable + " where 1=1 ";

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
		String result = null;
		result = dbUtils.getJSONResultOnQuery(query, "CXIQ");
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("NPS value is " + result + " fromDate = " + fromDate + ", toDate = " + toDate);
		}
		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		}
		return result;
	}

	@RequestMapping(value = "/nps/daywise", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getNPSByDay(@RequestParam("from") String fromDate, @RequestParam("to") String toDate,
			@RequestParam("tenantId") String tenantId, @RequestParam("brand") String brand,
			@RequestBody(required = false) String filter) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Getting NPS-daywise value fromDate = " + fromDate + ", toDate = " + toDate);
		}
		String npsTable = dbUtils.getCXIQTable(tenantId, brand);

		String query = "select content_published_date \"day\", round(avg(sourcedata_nps_score),2) \"value\", "
				+ "SUM(CASE WHEN ( sourcedata_nps_score > 8 ) THEN 1 ELSE 0 END) promoters, "
				+ "SUM(CASE WHEN ( sourcedata_nps_score < 7) THEN 1 ELSE 0 END) detractors,"
				+ "SUM(CASE WHEN ( sourcedata_nps_score = 7 or sourcedata_nps_score = 8) THEN 1 ELSE 0 END) passives, count(*) total "
				+ "from " + npsTable + " where 1=1 ";

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
		query = query + " group by content_published_date order by content_published_date asc";

		String result = dbUtils.getJSONResultOnQuery(query, "CXIQ");
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("NPS-daywise value is " + result + " fromDate = " + fromDate + ", toDate = " + toDate);
		}
		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		}

		return result;
	}

	@RequestMapping(value = "/topics", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getTopics(@RequestParam("tenantId") String tenantId, @RequestParam("brand") String brand) {

		if (LOGGER.isDebugEnabled())
			LOGGER.debug(
					"Get Topics with following parameters \n " + ", tenantId = " + tenantId + ", brand = " + brand);

		String npsTable = dbUtils.getCXIQTable(tenantId, brand);

		String result = "{TOPIC:0}";
		String topicPrefix = "topic~";
		String topicID = "", topicName = "";

		List<String> columns = getColumns(npsTable);

		JSONArray topicsArray = new JSONArray();
		for (String name : columns) {
			if (name.startsWith(topicPrefix)) {
				String topic[] = name.split("~");
				topicID = name;
				topicName = topic[1];
				JSONObject topicsObject = new JSONObject();
				try {
					topicsObject.put("id", topicID);
					topicsObject.put("topic", topicName);
				} catch (JSONException e) {
					LOGGER.error(e);
					return null;
				}

				topicsArray.put(topicsObject);
			}
		}
		result = topicsArray.toString();
		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"TOPICS\":0}";
		}
		return result;
	}

	@RequestMapping(value = "/topics/volume", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getTopicsVolume(@RequestParam("from") String fromDate, @RequestParam("to") String toDate,
			@RequestParam(value = "audience", required = false) String audience,
			@RequestParam("filter") String topicsFilter, @RequestParam("tenantId") String tenantId,
			@RequestParam("brand") String brand, @RequestBody(required = false) String filter) {

		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Get Topics Volume with following parameters \n " + ", fromDate = " + fromDate + ", toDate = "
					+ toDate);

		String npsTable = dbUtils.getCXIQTable(tenantId, brand);

		String result = "{TOPIC:0}";
		Connection connection = null;

		try {
			List<String> columns = getColumns(npsTable);
			String latestCountQuery = " select count(*) as cnt from " + npsTable;
			connection = cxiqDataSource.getConnection();
			Statement stmt2 = connection.createStatement();
			ResultSet rs2 = stmt2.executeQuery(latestCountQuery);
			String countOfLatestScore = "";
			if (rs2.next())
				countOfLatestScore = rs2.getString("cnt");
			int latestCnt = Integer.parseInt(countOfLatestScore) / 10;
			rs2.close();
			String npsScoreQuery = "SELECT ";
			String percentQuery = "SELECT ";
			String topicPrefix = "topic~";
			int prefixLen = topicPrefix.length();
			int numTopics = 0;

			for (String name : columns) {
				if (name.startsWith(topicPrefix) && !name.startsWith(topicPrefix + "filter_")
						&& !name.startsWith(topicPrefix + "standard_")
						&& !name.startsWith(topicPrefix + "linguistic_")) {
					if (numTopics != 0) {
						npsScoreQuery += ",";
						percentQuery += ",";
					}
					percentQuery += "ROUND (SUM(CASE WHEN `" + name + "` != 0 THEN 1 ELSE 0 END)*100.0/count(*),2) AS `"
							+ name.substring(prefixLen) + "`";
					npsScoreQuery += "ROUND (AVG(CASE WHEN `" + name + "` != 0 THEN sourcedata_nps_score END),2) AS `"
							+ name.substring(prefixLen) + "`";
					numTopics++;
				}
			}

			String filterQuery = "";

			if (fromDate == null || toDate == null || fromDate.equals("") || toDate.equals("")) {
				filterQuery = "";
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
				String topicsQueryFilter = cxiqFilter.getTopicsQueryFilter(columns, topicsFilter);
				if (topicsQueryFilter != null) {
					filterQuery += ((filterQuery.equals("")) ? " WHERE " : " AND ") + topicsQueryFilter;
				}

			}
			if (filter != null && !filter.equals("")) {
				String hierarchyFilter = cxiqFilter.getHierarchyFilter(filter);
				if (hierarchyFilter != null && !"".equalsIgnoreCase(hierarchyFilter)) {
					filterQuery += ((filterQuery.equals("")) ? " WHERE " : " AND  ") + hierarchyFilter;

				}
				String categoryFilter = cxiqFilter.getCategoryFilter(filter);
				if (categoryFilter != null && !"".equalsIgnoreCase(categoryFilter)) {
					filterQuery += ((filterQuery.equals("")) ? " WHERE " : " AND ") + categoryFilter;
				}
			}

			String latestScoreQuery = npsScoreQuery + " FROM (SELECT * FROM " + npsTable + filterQuery
					+ " ORDER BY content_published_date DESC LIMIT " + latestCnt + " ) latest_talk";

			npsScoreQuery += " FROM " + npsTable + filterQuery;
			percentQuery += " FROM " + npsTable + filterQuery;

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("TOPICS by NPS SCORE QUERY: \n " + npsScoreQuery);
				LOGGER.debug("TOPICS by PERCENTAGE QUERY: \n " + percentQuery);
				LOGGER.debug("TOPICS by LATEST NPS SCORE QUERY (last 10%): \n " + latestScoreQuery);
			}

			Statement stmt = connection.createStatement();
			stmt.setFetchSize(2000);
			long millisStart1 = System.currentTimeMillis();
			ResultSet rs = stmt.executeQuery(percentQuery);
			long millisEnd1 = System.currentTimeMillis();
			if (LOGGER.isTraceEnabled())
				LOGGER.trace("Volume Time1:- " + (millisEnd1 - millisStart1));
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnCount = rsmd.getColumnCount();

			TopicValueComparator comparator = new TopicValueComparator("desc");
			ValueSortedMap<String, Float> sortedTopics = new ValueSortedMap<String, Float>(comparator);
			JSONObject jsonObj = null;

			if (rs.next()) {
				HashMap<String, String> colCommentMap = getAllColumnComments(connection, npsTable);
				sortedTopics.clear();

				for (int i = 1; i <= columnCount; i++) {
					sortedTopics.put(rsmd.getColumnName(i), rs.getFloat(i));
				}
				rs.close();
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("TOPICS by percent retrieved \n " + sortedTopics);
				}

				// Topics with nps score value
				Map<String, Float> topicsWithNPSScore = new HashMap<String, Float>();
				long millisStart2 = System.currentTimeMillis();
				rs = stmt.executeQuery(npsScoreQuery);
				long millisEnd2 = System.currentTimeMillis();
				if (LOGGER.isTraceEnabled())
					LOGGER.trace("Volume Time2:- " + (millisEnd2 - millisStart2));
				rsmd = rs.getMetaData();
				columnCount = rsmd.getColumnCount();
				if (rs.next()) {
					for (int i = 1; i <= columnCount; i++) {
						topicsWithNPSScore.put(rsmd.getColumnName(i), rs.getFloat(i));
					}
				}
				rs.close();
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("TOPICS by score retrieved \n " + topicsWithNPSScore);
				}
				Map<String, Float> topicsWithLatestScore = new HashMap<String, Float>();
				long millisStart3 = System.currentTimeMillis();
				rs = stmt.executeQuery(latestScoreQuery);
				long millisEnd3 = System.currentTimeMillis();
				if (LOGGER.isTraceEnabled())
					LOGGER.trace("Volume Time3:- " + (millisEnd3 - millisStart3));
				rsmd = rs.getMetaData();
				columnCount = rsmd.getColumnCount();
				if (rs.next()) {
					for (int i = 1; i <= columnCount; i++) {
						topicsWithLatestScore.put(rsmd.getColumnName(i), rs.getFloat(i));
					}
				}
				rs.close();

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("TOPICS by latest score \n " + topicsWithLatestScore);
				}

				JSONArray jsonArray = new JSONArray();
				for (Entry<String, Float> entry : sortedTopics.entrySet()) {
					String key = entry.getKey();
					Object value = entry.getValue();
					jsonObj = new JSONObject();
					jsonObj.put("id", "topic~" + key);
					jsonObj.put("percentage", value);
					jsonObj.put("nps_score", topicsWithNPSScore.get(key));
					jsonObj.put("latest_score", topicsWithLatestScore.get(key));
					String description = colCommentMap.get("topic~" + key);
					if (description != null && !"".equalsIgnoreCase(description))
						jsonObj.put("topic", description);
					jsonArray.put(jsonObj);
				}

				result = jsonArray.toString();
				stmt.close();

			}
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

	@RequestMapping(value = "/topic/analysis", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getTopicAnalysis(@RequestParam("topic") String topicName, @RequestParam("tenantId") String tenantId,
			@RequestParam("brand") String brand) {
		String npsTable = dbUtils.getCXIQTable(tenantId, brand);
		Connection connection = null;
		String query = "Select ";
		String selectValues = "";

		List<String> columns = getColumns(npsTable);

		for (String name : columns) {
			if (name.startsWith("performance_") || name.startsWith("persona_") || name.startsWith("purchase_path")
					|| name.startsWith("emotion_")) {
				selectValues += " SUM(CASE WHEN `" + name + "` != 0 THEN 1 ELSE 0 END) as `" + name + "`,";
			}
			if (name.startsWith("topic~")) {
				selectValues += "SUM(`" + name + "`) `" + name + "`,";
			}
		}
		query += selectValues.substring(0, (selectValues.length() - 1));
		query += " from " + npsTable + " where `" + topicName + "` != 0";

		if (LOGGER.isDebugEnabled())
			LOGGER.debug("TOPICS ANALYSIS QUERY = " + query);

		JSONObject result = new JSONObject();
		JSONArray mainArray = new JSONArray();
		try {
			connection = cxiqDataSource.getConnection();
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnCount = rsmd.getColumnCount();
			while (rs.next()) {
				String colName, colValue;

				JSONArray personaArray = new JSONArray();
				JSONArray emotionArray = new JSONArray();
				JSONArray purchasePathArray = new JSONArray();
				JSONArray purchIntentArray = new JSONArray();
				JSONArray performanceArray = new JSONArray();
				JSONArray connectedTopicArray = new JSONArray();
				for (int i = 1; i < columnCount + 1; i++) {
					try {
						JSONObject obj = new JSONObject();
						colName = rsmd.getColumnName(i);
						colValue = rs.getString(i);
						if (colValue != null) {
							obj.put("name",
									colName.replace("persona_", "").replace("emotion_", "")
											.replace("purchase_path_purch_intent_", "").replace("purchase_path_", "")
											.replace("performance_", "").replace("_", ""));
							obj.put("value", colValue.trim());
						} else {
							obj.put("name",
									colName.replace("persona_", "").replace("emotion_", "")
											.replace("purchase_path_purch_intent_", "").replace("purchase_path_", "")
											.replace("performance_", "").replace("_", ""));
							obj.put("value", 0);
						}
						if (colName.startsWith("persona_"))
							personaArray.put(obj);
						else if (colName.startsWith("emotion_"))
							emotionArray.put(obj);
						else if (colName.startsWith("purchase_path_purch_intent_"))
							purchIntentArray.put(obj);
						else if (colName.startsWith("purchase_path_")
								&& !colName.startsWith("purchase_path_purch_intent_"))
							purchasePathArray.put(obj);
						else if (colName.startsWith("performance_"))
							performanceArray.put(obj);
						else if (colName.startsWith("topic~")) {
							if (Integer.parseInt(colValue) > 0) {
								connectedTopicArray.put(obj);
							}
						}
					} catch (SQLException e) {
						LOGGER.error("Error while converting DB record as JSONObject");
						LOGGER.error(e);
					}
				}

				JSONObject personaObj = new JSONObject();
				personaObj.put("name", "Persona");
				personaObj.put("children", personaArray);
				JSONObject emotionObj = new JSONObject();
				emotionObj.put("name", "Emotion");
				emotionObj.put("children", emotionArray);
				JSONObject purchIntentObj = new JSONObject();
				purchIntentObj.put("name", "PI");
				purchIntentObj.put("children", purchIntentArray);
				JSONObject purchasePathObj = new JSONObject();
				purchasePathObj.put("name", "PurchasePath");
				purchasePathObj.put("children", purchasePathArray);
				JSONObject performanceObj = new JSONObject();
				performanceObj.put("name", "Performance");
				performanceObj.put("children", performanceArray);
				JSONObject topicsObj = new JSONObject();
				topicsObj.put("name", "Topics");
				topicsObj.put("children", connectedTopicArray);

				mainArray.put(personaObj);
				mainArray.put(emotionObj);
				mainArray.put(purchIntentObj);
				mainArray.put(purchasePathObj);
				mainArray.put(performanceObj);
				mainArray.put(topicsObj);
			}

			result.put("name", topicName);
			result.put("children", mainArray);

			rs.close();
			stmt.close();

		} catch (Exception e) {
			LOGGER.error(e);
		} finally {
			if (connection != null)
				try {
					connection.close();
				} catch (SQLException e) {
					LOGGER.error(e);
				}
		}
		return result.toString();
	}

	@RequestMapping(value = "/emotionstrend", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getEmotionsTrend(@RequestParam("from") String fromDate, @RequestParam("to") String toDate,
			@RequestParam("tenantId") String tenantId, @RequestParam("brand") String brand,
			@RequestBody(required = false) String filter) {

		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Emotions Trend : fromDate=" + fromDate + ",toDate=" + toDate);
		String npsTable = dbUtils.getCXIQTable(tenantId, brand);
		List<String> columns = getColumns(npsTable);

		// PREPARE query for fetching topics in descending order of given filter
		StringBuffer mQuery = new StringBuffer("SELECT content_published_date as day,");
		String emotionPrefix = "emotion_";

		for (String name : columns) {
			if (name.startsWith(emotionPrefix)
					&& (POSITIVE_EMOTIONS.contains(name) || NEGATIVE_EMOTIONS.contains(name))) {

				mQuery.append("ROUND( (SUM(CASE WHEN ( " + name + " != 0 ) THEN 1 ELSE 0 END)) *100.0/count(*),2) "
						+ name.replace("emotion_", "") + " ,");

			}
		}
		String query = mQuery.substring(0, mQuery.length() - 1) + " FROM " + npsTable + " where 1=1 ";
		if (fromDate != null && toDate != null && !toDate.equals("") && !fromDate.equals("")) {
			query += " and content_published_date between '" + fromDate + "' and '" + toDate + "' ";
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
		query += " group by content_published_date order by content_published_date asc";
		String result = dbUtils.getJSONResultOnQuery(query, "CXIQ");

		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Emotion value is " + result + " fromDate = " + fromDate + ", toDate = " + toDate);

		if (result == null || result.trim().length() == 0) {
			result = "[{\"day\":\"" + fromDate + "\",\"emotion\":\"0\"}]";
		}
		return result;
	}

	@RequestMapping(value = "/emotion", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getEmotion(@RequestParam("trend") String trend, @RequestParam("from") String fromDate,
			@RequestParam("to") String toDate, @RequestParam("tenantId") String tenantId,
			@RequestParam("brand") String brand, @RequestBody(required = false) String filter) {
		boolean isPositiveTrend = true;
		String npsTable = dbUtils.getCXIQTable(tenantId, brand);

		if ((trend == null) || (trend.equals(""))) {
			return "{ERROR:No Trend parameter}";
		}
		if (trend.equals("negative")) {
			isPositiveTrend = false;
		} else if (!trend.equals("positive")) {
			return "{ERROR:Trend can have 'positive' or 'negative' values only }";
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Getting Emotion trend fromDate = " + fromDate + ", toDate = " + toDate + ", trend =" + trend);
		}
		String result = null;
		String query = getEmotionTrendQuery(npsTable, isPositiveTrend ? POSITIVE_EMOTIONS : NEGATIVE_EMOTIONS)
				+ " where 1=1 ";
		if (fromDate != null && toDate != null && !toDate.equals("") && !fromDate.equals("")) {
			query += " and content_published_date between '" + fromDate + "' and '" + toDate + "' ";
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
		query = query + " group by content_published_date order by content_published_date asc";
		result = dbUtils.getJSONResultOnQuery(query, "CXIQ");
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Emotion value is " + result + " fromDate = " + fromDate + ", toDate = " + toDate
					+ ", trend = " + trend);
		}
		if ((result == null) || (result.trim().length() == 0)) {
			result = "[{\"day\":\"" + fromDate + "\",\"emotion\":\"0\"}]";
		}
		return result;
	}

	@RequestMapping(value = "/impact", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getImpact(@RequestParam("from") String fromDate, @RequestParam("to") String toDate,
			@RequestParam(value = "audience", required = false) String audience,
			@RequestParam("filter") String topicsFilter, @RequestParam("tenantId") String tenantId,
			@RequestParam("brand") String brand, @RequestBody(required = false) String filter) {

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Get Topics with following parameters \n , fromDate = " + fromDate + ", toDate = " + toDate);
		}
		String result = "{TOPIC:0}";

		String npsTable = dbUtils.getCXIQTable(tenantId, brand);

		String impactScoreQuery = "SELECT ";
		String percentQuery = "SELECT ";
		String topicPrefix = "topic~";
		int prefixLen = topicPrefix.length();
		int numTopics = 0;
		Connection connection = null;
		try {
			connection = cxiqDataSource.getConnection();
			List<String> columns = getColumns(npsTable);
			String latestCountQuery = " select round(count(*)/10) as cnt from " + npsTable;
			Statement stmt2 = connection.createStatement();
			ResultSet rs2 = stmt2.executeQuery(latestCountQuery);
			String countOfLatestScore = "";
			if (rs2.next())
				countOfLatestScore = rs2.getString("cnt");
			rs2.close();
			for (String name : columns) {
				if ((name.startsWith(topicPrefix)) && (!name.startsWith(topicPrefix + "filter_"))
						&& (!name.startsWith(topicPrefix + "standard_"))
						&& (!name.startsWith(topicPrefix + "linguistic_"))) {
					if (numTopics != 0) {
						impactScoreQuery = impactScoreQuery + ",";
						percentQuery += ",";
					}
					percentQuery += "ROUND (SUM(CASE WHEN `" + name + "` != 0 THEN 1 ELSE 0 END)*100.0/count(*),2) AS `"
							+ name.substring(prefixLen) + "`";
					impactScoreQuery = impactScoreQuery + "ROUND (AVG(CASE WHEN `" + name + "` <> 0 THEN `" + name
							+ "`  ELSE NULL END),2) AS `" + name.substring(prefixLen) + "`";
					impactScoreQuery += " , ROUND (AVG(CASE WHEN `" + name
							+ "` != 0 THEN sourcedata_nps_score END),2) AS `" + name.substring(prefixLen)
							+ "_likertscore` ";
					numTopics++;
				}
			}

			String filterQuery = "";
			if ((fromDate == null) || (toDate == null) || (fromDate.equals("")) || (toDate.equals(""))) {
				filterQuery = "";
			} else {
				filterQuery = filterQuery + (filterQuery.equals("") ? " WHERE " : " AND ")
						+ " ( content_published_date between '" + fromDate + "' and '" + toDate + "') ";
			}
			if ((audience != null) && (!audience.equals(""))) {
				String audienceFilter = cxiqFilter.getAudienceFilter(audience);
				if (audienceFilter != null) {
					filterQuery = filterQuery + (filterQuery.equals("") ? " WHERE " : " AND ") + audienceFilter;
				}
			}
			if ((topicsFilter != null) && (!topicsFilter.equals(""))) {
				String topicsQueryFilter = cxiqFilter.getTopicsQueryFilter(columns, topicsFilter);
				if (topicsQueryFilter != null) {
					filterQuery = filterQuery + (filterQuery.equals("") ? " WHERE " : " AND ") + topicsQueryFilter;
				}
			}
			if (filter != null && !filter.equals("")) {
				String hierarchyFilter = cxiqFilter.getHierarchyFilter(filter);
				if (hierarchyFilter != null && !"".equalsIgnoreCase(hierarchyFilter)) {
					filterQuery += (filterQuery.equals("") ? " WHERE " : " AND ") + hierarchyFilter;

				}
				String categoryFilter = cxiqFilter.getCategoryFilter(filter);
				if (categoryFilter != null && !"".equalsIgnoreCase(categoryFilter)) {
					filterQuery += (filterQuery.equals("") ? " WHERE " : " AND ") + categoryFilter;
				}
			}
			// last 10% score results
			String latestScoreQuery = impactScoreQuery + " FROM (SELECT * FROM " + npsTable + filterQuery
					+ " ORDER BY content_published_date DESC LIMIT " + countOfLatestScore + " ) latest_talk";

			impactScoreQuery = impactScoreQuery + " FROM " + npsTable + filterQuery;
			percentQuery = percentQuery + " FROM " + npsTable + filterQuery;

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("TOPICS IMPACT QUERY: \n " + impactScoreQuery);
			}

			Statement stamt = connection.createStatement();
			ResultSet rs = stamt.executeQuery(impactScoreQuery);
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnCount = rsmd.getColumnCount();

			TopicValueComparator comparator = new TopicValueComparator("desc");
			ValueSortedMap<String, Float> sortedTopics = new ValueSortedMap<String, Float>(comparator);
			HashMap<String, Float> likertScoreMap = new HashMap<String, Float>();
			JSONObject jsonObj = null;
			if (rs.next()) {
				HashMap<String, String> colCommentMap = getAllColumnComments(connection, npsTable);
				sortedTopics.clear();
				for (int i = 1; i <= columnCount; i++) {
					String colName = rsmd.getColumnName(i);
					Float colvalue = Float.valueOf(rs.getFloat(i));
					if (!colName.endsWith("likertscore"))
						sortedTopics.put(colName, colvalue);
					else
						likertScoreMap.put(colName, colvalue);
				}
				rs.close();

				// Topics with latest impact score (last 10% messages)
				Map<String, Float> topicsWithLatestScore = new HashMap<String, Float>();

				rs = stamt.executeQuery(latestScoreQuery);
				rsmd = rs.getMetaData();
				columnCount = rsmd.getColumnCount();
				if (rs.next()) {
					for (int i = 1; i <= columnCount; i++) {
						topicsWithLatestScore.put(rsmd.getColumnName(i), rs.getFloat(i));
					}
				}
				rs.close();

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("TOPICS by latest impact score \n " + topicsWithLatestScore);
				}

				// Topics with percentage value
				Map<String, Float> topicsWithPercent = new HashMap<String, Float>();

				rs = stamt.executeQuery(percentQuery);
				rsmd = rs.getMetaData();
				columnCount = rsmd.getColumnCount();
				if (rs.next()) {
					for (int i = 1; i <= columnCount; i++) {
						topicsWithPercent.put(rsmd.getColumnName(i), rs.getFloat(i));
					}
				}
				rs.close();
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("TOPICS by percent retrieved \n " + topicsWithPercent);
				}

				JSONArray jsonArray = new JSONArray();
				for (Map.Entry<String, Float> entry : sortedTopics.entrySet()) {
					String key = (String) entry.getKey();
					Object value = entry.getValue();
					jsonObj = new JSONObject();
					jsonObj.put("id", "topic~" + key);
					jsonObj.put("impact_score", value);
					jsonObj.put("likert_score", likertScoreMap.get(key + "_likertscore"));
					jsonObj.put("latest_score", topicsWithLatestScore.get(key));
					jsonObj.put("percentage", topicsWithPercent.get(key));
					String description = colCommentMap.get("topic~" + key);
					if (description != null && !"".equalsIgnoreCase(description))
						jsonObj.put("topic", description);
					jsonArray.put(jsonObj);
				}
				result = jsonArray.toString();
			}
			stamt.close();
		} catch (Exception e) {
			LOGGER.error(e);
			return null;
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

	@RequestMapping(value = "/messages/summary", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getMessagesSummary(@RequestParam("from") String fromDate, @RequestParam("to") String toDate,
			@RequestParam(value = "audience", required = false) String audience,
			@RequestParam("filter") String topicsFilter, @RequestParam("tenantId") String tenantId,
			@RequestParam("brand") String brand, @RequestBody(required = false) String filter) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Get messages/summary with following parameters \n , fromDate = " + fromDate + ", toDate = "
					+ toDate);
		}
		String npsTable = dbUtils.getCXIQTable(tenantId, brand);
		List<String> columns = getColumns(npsTable);
		String query = "";
		String result = "{\"Posts\":0}";

		String filterQuery = "";
		if ((fromDate == null) || (toDate == null) || (fromDate.equals("")) || (toDate.equals(""))) {
			filterQuery = "";
		} else {
			filterQuery = filterQuery + (filterQuery.equals("") ? " WHERE " : " AND ")
					+ " ( content_published_date between '" + fromDate + "' and '" + toDate + "') ";
		}
		if ((audience != null) && (!audience.equals(""))) {
			String audienceFilter = cxiqFilter.getAudienceFilter(audience);
			if (audienceFilter != null) {
				filterQuery = filterQuery + (filterQuery.equals("") ? " WHERE " : " AND ") + audienceFilter;
			}
		}
		if ((topicsFilter != null) && (!topicsFilter.equals(""))) {
			String topicsQueryFilter = cxiqFilter.getTopicsQueryFilter(columns, topicsFilter);
			if (topicsQueryFilter != null) {
				filterQuery = filterQuery + (filterQuery.equals("") ? " WHERE " : " AND ") + topicsQueryFilter;
			}
		}
		if (filter != null && !filter.equals("")) {
			String hierarchyFilter = cxiqFilter.getHierarchyFilter(filter);
			if (hierarchyFilter != null && !"".equalsIgnoreCase(hierarchyFilter)) {
				filterQuery = filterQuery + (filterQuery.equals("") ? " WHERE " : " AND ") + hierarchyFilter;

			}
			String categoryFilter = cxiqFilter.getCategoryFilter(filter);
			if (categoryFilter != null && !"".equalsIgnoreCase(categoryFilter)) {
				filterQuery = filterQuery + (filterQuery.equals("") ? " WHERE " : " AND ") + categoryFilter;
			}
		}
		query = "select  CASE WHEN sourcedata_nps_score > 8 THEN 'Promoter' WHEN sourcedata_nps_score < 7 THEN 'Detractor'ELSE 'Passive' END AS emotion,count(sourcedata_nps_score) as count "
				+ "FROM " + npsTable;
		query = query + filterQuery + " group by emotion";
		result = dbUtils.getJSONResultOnQuery(query, "CXIQ");
		return result;
	}

	@RequestMapping(value = "/messages", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getMessages(@RequestParam("from") String fromDate, @RequestParam("to") String toDate,
			@RequestParam(value = "audience", required = false) String audience,
			@RequestParam("filter") String topicsFilter,
			@RequestParam(value = "pageSize", required = false) String pageSize,
			@RequestParam(value = "start", required = false) String offset, @RequestParam("tenantId") String tenantId,
			@RequestParam("brand") String brand, @RequestBody(required = false) String filter) {
		String query = "";
		if ((offset == null) || (offset.equals(""))) {
			offset = "0";
		}
		if ((pageSize == null) || (pageSize.equals(""))) {
			pageSize = "10";
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(
					"Get poststream with following parameters \n , fromDate = " + fromDate + ", toDate = " + toDate);
		}
		String result = "{\"Posts\":0}";

		String npsTable = dbUtils.getCXIQTable(tenantId, brand);
		List<String> columns = getColumns(npsTable);

		String filterQuery = "";
		if ((fromDate == null) || (toDate == null) || (fromDate.equals("")) || (toDate.equals(""))) {
			filterQuery = "";
		} else {
			filterQuery = filterQuery + (filterQuery.equals("") ? " WHERE " : " AND ")
					+ " ( content_published_date between '" + fromDate + "' and '" + toDate + "') ";
		}
		if ((audience != null) && (!audience.equals(""))) {
			String audienceFilter = cxiqFilter.getAudienceFilter(audience);
			if (audienceFilter != null) {
				filterQuery = filterQuery + (filterQuery.equals("") ? " WHERE " : " AND ") + audienceFilter;
			}
		}
		if ((topicsFilter != null) && (!topicsFilter.equals(""))) {
			String topicsQueryFilter = cxiqFilter.getTopicsQueryFilter(columns, topicsFilter);
			if (topicsQueryFilter != null) {
				filterQuery = filterQuery + (filterQuery.equals("") ? " WHERE " : " AND ") + topicsQueryFilter;
			}
		}
		if (filter != null && !filter.equals("")) {
			String hierarchyFilter = cxiqFilter.getHierarchyFilter(filter);
			if (hierarchyFilter != null && !"".equalsIgnoreCase(hierarchyFilter)) {
				filterQuery = filterQuery + (filterQuery.equals("") ? " WHERE " : " AND ") + hierarchyFilter;

			}
			String categoryFilter = cxiqFilter.getCategoryFilter(filter);
			if (categoryFilter != null && !"".equalsIgnoreCase(categoryFilter)) {
				filterQuery = filterQuery + (filterQuery.equals("") ? " WHERE " : " AND ") + categoryFilter;
			}
		}
		query = "select decooda_content_id,sourcedata_id,sourcedata_author,content, CASE WHEN sourcedata_nps_score > 8 THEN 'Promoter' WHEN sourcedata_nps_score < 7 THEN 'Detractor'ELSE 'Passive' END AS emotion "
				+ "FROM " + npsTable;

		query = query + filterQuery;
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("POSTSTream QUERY: \n " + query);
		}
		query = query + " limit " + pageSize + " offset " + offset;

		result = dbUtils.getJSONResultOnQuery(query, "CXIQ");

		return result;
	}

	@RequestMapping(value = "/message/details", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getMessageDetails(@RequestParam("id") String id, @RequestParam("tenantId") String tenantId,
			@RequestParam("brand") String brand) {
		String npsTable = dbUtils.getCXIQTable(tenantId, brand);
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Message Details : id= " + id);
		String query = "select decooda_content_id,concat( CASE WHEN `performance~the_best~pos~excitement` !=0 THEN 'Best' ELSE '' END,' ',CASE WHEN `performance~recommend_pos~pos~happiness` !=0 THEN 'RecommendPositive' ELSE '' END,' ',   CASE WHEN `performance~better_than~pos~happiness` !=0 THEN 'BetterThan' ELSE '' END,' ',   CASE WHEN `performance~needs_met~pos~happiness` !=0 THEN 'NeedsMet' ELSE '' END,' ', CASE WHEN `performance~trust_pos~pos~happiness` !=0 THEN 'TrustPositive' ELSE '' END,' ',   CASE WHEN `performance~recommend_neg~neg~disappointment` !=0 THEN 'RecommendNegative' ELSE '' END,' ',      CASE WHEN `performance~value_neg~neg~disappointment` !=0 THEN 'ValueNegative' ELSE '' END,' ',      CASE WHEN `performance~value_pos~pos~happiness` !=0 THEN 'ValuePositive' ELSE '' END)As performance,           concat( CASE WHEN `persona~advocate~pos~happiness` !=0 THEN 'Advocate' ELSE '' END,' ',          CASE WHEN `persona~lover~pos~happiness` !=0 THEN 'Lover' ELSE '' END,' ',  CASE WHEN `persona~hater~neg~anger` !=0 THEN 'Hater' ELSE '' END) As persona,    concat(CASE WHEN `purchase_path~churn_definate~neg~anger` !=0 THEN 'ChurnDefinate' ELSE '' END) As purchasePath,   concat( CASE WHEN emotion_happiness !=0 THEN 'Happiness' ELSE '' END,' ',   CASE WHEN emotion_disappointment !=0 THEN 'Disappointment' ELSE '' END,' ',  CASE WHEN emotion_excitement !=0 THEN 'Excitement' ELSE '' END,' ', CASE WHEN emotion_anger !=0 THEN 'Anger' ELSE '' END,' ', CASE WHEN emotion_frustration !=0 THEN 'Frustration' ELSE '' END,' ', CASE WHEN emotion_gratitude !=0 THEN 'Gratitude' ELSE '' END) As emotion,     concat(CASE WHEN `purchase_path~purch_intent_pos_v2~pos~happiness` !=0 THEN 'Positive' ELSE '' END,' ',  CASE WHEN `purchase_path~purch_intent_neg_v2~neg~frustration` !=0 THEN 'Negative' ELSE '' END) As pi  "
				+ "from " + npsTable + " where decooda_content_id='" + id + "'";
		String result = null;
		result = dbUtils.getJSONResultOnQuery(query, "CXIQ");
		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		}
		return result;
	}

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

	@RequestMapping(value = "/version", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getNPSVersion() {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Get NPS Version ");
		String result = "{version:0}";

		try {
			JSONArray jsonArray = new JSONArray();
			JSONObject obj = new JSONObject();
			obj.put("version", version);
			result = jsonArray.put(obj).toString();
			if (LOGGER.isDebugEnabled())
				LOGGER.debug("NPS Version = " + result);
		} catch (JSONException e) {
			LOGGER.error("Exception in retrieving NPS Version : " + e);
		}
		return result;

	}

	@RequestMapping(value = "/hierarchies", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getHierarchies(@RequestParam("tenantId") String tenantId, @RequestParam("brand") String brand,
			@RequestParam("hierarchyName") String hierarchyName, @RequestParam("hierarchyValue") String hierarchyValue,
			@RequestParam("levels") String levels) {

		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Get hierarchies : hierarchyName=" + hierarchyName + ",hierarchyValue=" + hierarchyValue);
		String npsTable = dbUtils.getCXIQTable(tenantId, brand);
		String result = null;
		String query = qg.generateHierarchiesOrCategoriesQuery(npsTable, hierarchyName, hierarchyValue, levels);
		result = dbUtils.getJSONResultOnQuery(query, "CXIQ");

		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		}
		return result;

	}

	@RequestMapping(value = "/categories", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getCategories(@RequestParam("tenantId") String tenantId, @RequestParam("brand") String brand,
			@RequestParam("categoryName") String categoryName, @RequestParam("categoryValue") String categoryValue,
			@RequestParam("levels") String levels) {

		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Get categories : categoryName=" + categoryName + ",categoryValue=" + categoryValue);
		String npsTable = dbUtils.getCXIQTable(tenantId, brand);
		String result = null;
		String query = qg.generateHierarchiesOrCategoriesQuery(npsTable, categoryName, categoryValue, levels);
		result = dbUtils.getJSONResultOnQuery(query, "CXIQ");

		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		}
		return result;
	}

	@RequestMapping(value = "/tenants/{tenantId}/brands", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getBrands(@PathVariable("tenantId") String tenantId) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Get Brands : tenantId=" + tenantId);
		String result = null;
		String query = " SELECT SUBSTRING(TABLE_NAME,25) AS BRAND FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = '"
				+ tenantId + "' and  TABLE_NAME like '" + CXIQConstants.DECOODA_TABLE_PREFIX
				+ "%' and TABLE_NAME NOT like '" + CXIQConstants.DECOODA_TABLE_PREFIX + "%_analysis'";
		result = dbUtils.getJSONResultOnQuery(query, "CXIQ");

		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		}
		return result;
	}

	@RequestMapping(value = "/tenants/{tenantId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
	public String deleteTenant(@PathVariable("tenantId") String tenantId) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Delete Tenant : tenantId=" + tenantId);
		String result = null;
		result = cxiqAdmin.deleteTenant(tenantId);

		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":\"false\"}";
		} else {
			JSONObject obj = new JSONObject();
			JSONArray array = new JSONArray();
			try {
				obj.put("result", result);
				array.put(obj);
				result = array.toString();
			} catch (JSONException e) {
				LOGGER.error("deleteTenant() Exception: " + e.getMessage());
			}
		}
		return result;
	}

	@RequestMapping(value = "/tenants/{tenantId}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
	public String createTenant(@PathVariable("tenantId") String tenantId,
			@RequestParam("tenantName") String tenantName) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Create Tenant : tenantId=" + tenantId);
		String result = null;
		result = cxiqAdmin.createTenant(tenantId, tenantName);

		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":\"false\"}";
		} else {
			JSONObject obj = new JSONObject();
			JSONArray array = new JSONArray();
			try {
				obj.put("result", result);
				array.put(obj);
				result = array.toString();
			} catch (JSONException e) {
				LOGGER.error("createTenant() Exception: " + e.getMessage());
			}
		}
		return result;
	}

	@RequestMapping(value = "/users/{userId}/brands", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getBrandsForUser(@RequestParam("tenantId") String tenantId, @PathVariable("userId") String userId) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Get Brands : tenantId=" + tenantId + ",userId=" + userId);
		String result = null;
		String query = " SELECT * from  " + tenantId + ".user_brand_map where user_id='" + userId + "'";
		result = dbUtils.getJSONResultOnQuery(query, "CXIQ");

		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":\"false\"}";
		}
		return result;
	}

	@RequestMapping(value = "/users/{userId}/brands", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String assignBrandsToUser(@RequestParam("tenantId") String tenantId, @PathVariable("userId") String userId,
			@RequestParam("brands") String brands, @RequestParam("defaultBrand") String defaultBrand) {

		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Assign Brand For User : tenantId=" + tenantId + ",userId=" + userId + ",brands=" + brands
					+ ",default_brand=" + defaultBrand);

		String result = cxiqAdmin.assignBrandsToUser(tenantId, userId, brands, defaultBrand);
		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		} else {
			JSONObject obj = new JSONObject();
			JSONArray array = new JSONArray();
			try {
				obj.put("result", result);
				array.put(obj);
				result = array.toString();
			} catch (JSONException e) {
				LOGGER.error("assignBrandsToUser() Exception: " + e.getMessage());
			}
		}
		return result;
	}

	@RequestMapping(value = "/tenants/{tenantId}/brand", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String assignDefaultBrandToTenant(@PathVariable("tenantId") String tenantId,
			@RequestParam("defaultBrand") String defaultBrand) {

		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Assign Default Brand To Tenant : tenantId=" + tenantId + ",default_brand=" + defaultBrand);

		String result = cxiqAdmin.assignBrandsToUser(tenantId, tenantId, "", defaultBrand);
		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		} else {
			JSONObject obj = new JSONObject();
			JSONArray array = new JSONArray();
			try {
				obj.put("result", result);
				array.put(obj);
				result = array.toString();
			} catch (JSONException e) {
				LOGGER.error("assignBrandsToUser() Exception: " + e.getMessage());
			}
		}
		return result;
	}

	@RequestMapping(value = "/users/{userId}/brands/{brands}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
	public String deleteBrandsForUser(@RequestParam("tenantId") String tenantId, @PathVariable("userId") String userId,
			@PathVariable("brands") String brands) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Delete Brand For User: tenantId=" + tenantId + ",userId=" + userId + ",brands=" + brands);

		String result = cxiqAdmin.deleteBrandsForUser(tenantId, userId, brands);
		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"result\":\"false\"}";
		} else {
			JSONObject obj = new JSONObject();
			JSONArray array = new JSONArray();
			try {
				obj.put("result", result);
				array.put(obj);
				result = array.toString();
			} catch (JSONException e) {
				LOGGER.error("deleteBrandsForUser() Exception: " + e.getMessage());
			}
		}
		return result;
	}

	@RequestMapping(value = "/brands/{brand}/levels", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getLevelsForBrand(@RequestParam("tenantId") String tenantId, @PathVariable("brand") String brand) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Get Levels for Brand : tenantId=" + tenantId + ",brand=" + brand);
		String result = null;
		String query = " SELECT brand,hierarchies,categories,item from  " + tenantId + ".brand_levels_map where brand='"
				+ brand + "'";
		result = dbUtils.getJSONResultOnQuery(query, "CXIQ");

		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		}
		return result;
	}

	@RequestMapping(value = "/brands/{brand}/levels", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String updateLevelsForBrand(@RequestParam("tenantId") String tenantId, @PathVariable("brand") String brand,
			@RequestParam("hierarchies") String hierarchies, @RequestParam("categories") String categories,
			@RequestParam("item") String item) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Update Levels for Brand : tenantId=" + tenantId + ",brand=" + brand + ",hierarchies="
					+ hierarchies + ",categories=" + categories + ",item=" + item);
		String result = "";
		result = cxiqAdmin.updateLevelsForBrand(tenantId, brand, hierarchies, categories, item);
		return result;
	}

	@RequestMapping(value = "/tenants", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getTeanants() {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Get All Tenants ");
		String result = null;
		result = cxiqAdmin.getTenants();
		return result;
	}

	@RequestMapping(value = "/brands/{brand}/columns", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getColumnsForBrand(@RequestParam("tenantId") String tenantId, @PathVariable("brand") String brand) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Get table columns for brand, tenantId=" + tenantId + ",brand=" + brand);
		String result = null;
		String query = " SELECT SUBSTRING(`COLUMN_NAME`,12) as COLUMNS,DATA_TYPE FROM `INFORMATION_SCHEMA`.`COLUMNS` "
				+ " WHERE `TABLE_SCHEMA`='" + tenantId + "' and `TABLE_NAME`='" + CXIQConstants.DECOODA_TABLE_PREFIX
				+ brand + "' " + " AND   `COLUMN_NAME` LIKE 'sourcedata_%'";
		result = dbUtils.getJSONResultOnQuery(query, "CXIQ");

		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		}
		return result;
	}

	@RequestMapping(value = "/users/{userId}/filters", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getFiltersForUser(@RequestParam("tenantId") String tenantId, @PathVariable("userId") String userId,
			@RequestParam("brand") String brand,
			@RequestParam(value = "filterName", required = false) String filterName) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Get Filters for User : tenantId=" + tenantId + ",userId=" + userId + ",brand=" + brand
					+ ",filterName=" + filterName);
		String result = null;
		String query = " SELECT * from  " + tenantId + ".user_filter_map where user_id='" + userId + "'";
		if (brand != null && !"".equalsIgnoreCase(brand))
			query = query + " and brand='" + brand + "'";
		if (filterName != null && !"".equalsIgnoreCase(filterName))
			query = query + " and filter_name='" + filterName + "'";
		query = query + " order by filter_name asc";
		result = dbUtils.getJSONResultOnQuery(query, "CXIQ");

		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		}
		return result;
	}

	@RequestMapping(value = "/users/{userId}/filter", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String updateFilterForUser(@RequestParam("tenantId") String tenantId, @PathVariable("userId") String userId,
			@RequestParam("brand") String brand,
			@RequestParam(value = "filterName", required = false) String filterName,
			@RequestBody(required = false) String filterData,
			@RequestParam(value = "defaultFilter", required = false) String defaultFilter) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Update Filter for User : tenantId=" + tenantId + ",userId=" + userId + ",brand=" + brand
					+ ",filterName=" + filterName);
		return qg.updateFilterForUser(tenantId, userId, brand, filterName, filterData, defaultFilter);
	}

	@RequestMapping(value = "/users/{userId}/filter", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
	public String deleteFilterForUser(@RequestParam("tenantId") String tenantId, @PathVariable("userId") String userId,
			@RequestParam("brand") String brand,
			@RequestParam(value = "filterName", required = false) String filterName) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Update Filter for User : tenantId=" + tenantId + ",userId=" + userId + ",brand=" + brand
					+ ",filterName=" + filterName);
		return qg.deleteFilterForUser(tenantId, userId, brand, filterName);
	}

	@RequestMapping(value = "/users/status", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getLoginStatusOfUser(@RequestParam("tenantId") String tenantId, @RequestParam("brand") String brand) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Get Login Status of User : tenantId=" + tenantId + ",brand=" + brand);
		String result = null;
		String query = " select user_id,status from  " + tenantId + ".user_login_status where 1=1 ";
		if (brand != null && !"".equalsIgnoreCase(brand))
			query = query + " and brand='" + brand + "'";
		result = dbUtils.getJSONResultOnQuery(query, "CXIQ");
		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		}
		return result;
	}

	@RequestMapping(value = "/users/{userId}/status", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String updateFilterForUser(@RequestParam("tenantId") String tenantId, @PathVariable("userId") String userId,
			@RequestParam("brand") String brand, @RequestParam("status") String status) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Update Filter for User : tenantId=" + tenantId + ",userId=" + userId + ",brand=" + brand
					+ ",status=" + status);
		String result = "";
		result = cxiqAdmin.updateLoginStatusOfUser(tenantId, userId, brand, status);
		return result;
	}

	@RequestMapping(value = "/user", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
	public String deleteUser(@RequestParam("tenantId") String tenantId, @RequestParam("userId") String userId) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Deleting User : tenantId=" + tenantId + ",userId=" + userId);
		String result = "{\"nps\":\"false\"}";
		boolean isDeleted = cxiqAdmin.deleteUser(tenantId, userId);
		if (isDeleted)
			result = "{\"nps\":\"true\"}";
		return result;
	}

	@RequestMapping(value = "/brands/{brand}/image/upload", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public String uploadFileForBrand(@RequestParam("tenantId") String tenantId, @PathVariable("brand") String brand,
			@FormDataParam("file") InputStream fileInputStream,
			@FormDataParam("file") FormDataContentDisposition fileInputDetails) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Upload file for brand : tenantId=" + tenantId + ",brand=" + brand);
		String result = "";
		result = cxiqAdmin.uploadFileForBrand(tenantId, brand, fileInputStream, fileInputDetails);
		return result;
	}

	@RequestMapping(value = "/brands/{brand}/image/download", method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public @ResponseBody byte[] getImageForBrand(@RequestParam("tenantId") String tenantId,
			@PathVariable("brand") String brand) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Get Image for Brand : tenantId=" + tenantId + ",brand=" + brand);
		// String result=null;

		return cxiqAdmin.downloadImageForBrand(tenantId, brand);
	}

	@RequestMapping(value = "/groups/{groupId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String updateGroupInBrand(@RequestParam("tenantId") String tenantId, @RequestParam("brand") String brand,
			@PathVariable("groupId") String groupId, @RequestParam("groupName") String groupName,
			@RequestBody String rule) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Update group in brand : tenantId=" + tenantId + ",brand=" + brand + ",groupId=" + groupId
					+ ",groupName=" + groupName + ",rule=" + rule);
		String result = "";
		result = cxiqAdmin.updateGroupInBrand(tenantId, brand, groupId, groupName, rule);
		return result;
	}

	@RequestMapping(value = "/groups/{groupId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getGroupDetailsInBrand(@RequestParam("tenantId") String tenantId, @RequestParam("brand") String brand,
			@PathVariable("groupId") String groupId) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(
					" Get group details for brand : tenantId=" + tenantId + ",brand=" + brand + ",groupId=" + groupId);
		String result = null;
		String query = " select * from  " + tenantId + ".brand_groups_map where 1=1 ";
		if (brand != null && !"".equalsIgnoreCase(brand))
			query = query + " and brand='" + brand + "'";
		if (groupId != null && !"".equalsIgnoreCase(groupId))
			query = query + " and group_id='" + groupId + "'";
		result = dbUtils.getJSONResultOnQuery(query, "CXIQ");
		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		}
		return result;
	}

	@RequestMapping(value = "/groups/{groupId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
	public String deleteGroupInBrand(@RequestParam("tenantId") String tenantId, @RequestParam("brand") String brand,
			@PathVariable("groupId") String groupId) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Delete group in brand : tenantId=" + tenantId + ",brand=" + brand + ",groupId=" + groupId);
		String result = "";
		result = cxiqAdmin.deleteGroupInBrand(tenantId, brand, groupId);
		return result;
	}

	@RequestMapping(value = "/brands/groups", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getGroupsForBrand(@RequestParam("tenantId") String tenantId, @RequestParam("brands") String brands) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Get groups for brand : tenantId=" + tenantId + ",brands=" + brands);
		String result = null;
		String query = " select * from  " + tenantId + ".brand_groups_map where 1=1 ";
		if (brands != null && !"".equalsIgnoreCase(brands)) {
			String[] brandArray = brands.split(",");
			String brand = "";
			for (int i = 0; i < brandArray.length; i++) {
				if (i != 0)
					brand = brand + ",";
				brand = brand + "\'" + brandArray[i] + "\'";
			}
			query = query + " and brand in (" + brand + ")";
		}
		result = dbUtils.getJSONResultOnQuery(query, "CXIQ");
		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		}
		return result;
	}

	@RequestMapping(value = "/drools/group", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getGroup(@RequestParam("tenantId") String tenantId, @RequestParam("brand") String brand,
			@RequestParam("npsScore") int npsScore, @RequestParam("primaryEmotion") String primary_emotion,
			@RequestParam("content") String content, @RequestParam("emotionHappiness") boolean emotion_happiness) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Get group from drools : tenantId=" + tenantId + ",brand=" + brand);

		String result = null;

		Group group = new Group();
		group.setNps_score(npsScore);
		// group.setPrimary_emotion(primary_emotion);
		group.setContent(content);
		// group.setEmotion_happiness(emotion_happiness);
		result = cxiqDrools.getGroup(tenantId, brand, group);
		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"GroupId\":0}";
		} else {
			if (LOGGER.isDebugEnabled())
				LOGGER.debug(" Group identified - " + result);
			JSONObject obj = new JSONObject();
			try {
				obj.put("GroupId", result);
				result = obj.toString();
			} catch (JSONException e) {
				LOGGER.error(e.getMessage());
			}
		}
		return result;
	}

	@RequestMapping(value = "/tasks/{taskId}/chat/messages", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String updateChatMessage(@PathVariable("taskId") String taskId, String fullMsg) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Update chat message : taskId=" + taskId);
		String result = "";
		result = chatMessages.updateChatMessage(taskId, fullMsg);
		return result;
	}

	@RequestMapping(value = "/tasks/{taskId}/chat/messages", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getChatMessages(@PathVariable("taskId") String taskId, @RequestParam("fromUserId") String fromUserId,
			@RequestParam("toUserId") String toUserId, @RequestParam("groupName") String groupName) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Get chat messages : taskId=" + taskId + ",fromUserId=" + fromUserId + ",toUserId=" + toUserId
					+ ",groupName=" + groupName);
		String result = "";
		result = chatMessages.getChatMessages(taskId, fromUserId, toUserId, groupName);
		return result;
	}

	@RequestMapping(value = "/tasks/{taskId}/chat/groups", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getChatGroupsForTask(@PathVariable("taskId") String taskId) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Get chat messages : taskId=" + taskId);
		String result = "";
		result = chatMessages.getChatGroupsForTask(taskId);
		return result;
	}

	@RequestMapping(value = "/messages/triage/status", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String updateTriageStatusForMessages(@RequestParam("tenantId") String tenantId,
			@RequestParam("brand") String brand, @RequestParam("status") String status,
			@RequestParam("limit") int limit) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Update triage status for messages ");
		String result = "";
		result = cxiqAdmin.updateTriageStatusForMessages(tenantId, brand, status, limit);
		return result;
	}

	@RequestMapping(value = "/logout", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String logout() {
		return "";
	}

	@RequestMapping(value = "/brand/topicgroups", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getTopicGroupsForBrand(@RequestParam("tenantId") String tenantId,
			@RequestParam("brand") String brand) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Get TopicGroups : tenantId=" + tenantId + ",brand=" + brand);
		String result = null;
		String query = " SELECT * from  " + tenantId + ".brand_topicgroup_map where brand='" + brand + "'";
		result = dbUtils.getJSONResultOnQuery(query, "CXIQ");

		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":\"false\"}";
		}
		return result;
	}

	@RequestMapping(value = "/topicgroup/topics", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getTopicsForTopicGroup(@RequestParam("tenantId") String tenantId, @RequestParam("brand") String brand,
			@RequestParam("topicGroup") String topicGroup) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Get TopicGroups : tenantId=" + tenantId + ",brand=" + brand + ",topicGroup=" + topicGroup);
		String result = null;
		String query = " SELECT * from  " + tenantId + ".brand_topicgroup_map where brand='" + brand
				+ "' and topic_group='" + topicGroup + "'";
		result = dbUtils.getJSONResultOnQuery(query, "CXIQ");

		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":\"false\"}";
		}
		return result;
	}

	@RequestMapping(value = "/brand/topicgroup", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String updateTopicGroupForBrand(@RequestParam("tenantId") String tenantId,
			@RequestParam("brand") String brand, @RequestParam("topicGroup") String topicGroup,
			@RequestParam("topics") String topics) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Update Topic Group for Brand : tenantId=" + tenantId + ",brand=" + brand + ",topicGroup="
					+ topicGroup + ",topics=" + topics);
		String result = "";
		result = topicGroups.updateTopicGroupForBrand(tenantId, brand, topicGroup, topics);
		return result;
	}

	@RequestMapping(value = "/brand/topicgroup", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
	public String deleteTopicGroupForBrand(@RequestParam("tenantId") String tenantId,
			@RequestParam("brand") String brand, @RequestParam("topicGroup") String topicGroup) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Delete Topic Group for Brand : tenantId=" + tenantId + ",brand=" + brand + ",topicGroup="
					+ topicGroup);
		String result = "";
		result = topicGroups.deleteTopicGroupForBrand(tenantId, brand, topicGroup);
		return result;
	}

	@RequestMapping(value = "/topics/zscore", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getTopicsZScore(@RequestParam(value = "topicGroup", required = false) String topicGroup,
			@RequestParam(value = "topic", required = false) String topic, @RequestParam("from") String fromDate,
			@RequestParam("to") String toDate, @RequestParam("tenantId") String tenantId,
			@RequestParam("brand") String brand, String filter) {

		if (LOGGER.isDebugEnabled())
			LOGGER.debug("Get Topic  Z score with following parameters \n " + ", fromDate = " + fromDate + ", toDate = "
					+ toDate);
		String result = "";
		result = topicGroups.getZScoreForTopics(tenantId, brand, topicGroup, topic, fromDate, toDate, filter);
		return result;
	}

	@RequestMapping(value = "/topicgroup/messages", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getTopicGroupMessages(@RequestParam("from") String fromDate, @RequestParam("to") String toDate,
			@RequestParam("audience") String audience, @RequestParam("filter") String topicsFilter,
			@RequestParam("pagesize") String pageSize, @RequestParam("start") String offset,
			@RequestParam("tenantId") String tenantId, @RequestParam("brand") String brand, String filter) {
		String query = "";
		if ((offset == null) || (offset.equals(""))) {
			offset = "0";
		}
		if ((pageSize == null) || (pageSize.equals(""))) {
			pageSize = "10";
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(
					"Get poststream with following parameters \n , fromDate = " + fromDate + ", toDate = " + toDate);
		}
		String result = "{\"Posts\":0}";

		String npsTable = dbUtils.getCXIQTable(tenantId, brand);
		List<String> columns = getColumns(npsTable);

		String filterQuery = "";
		if ((fromDate == null) || (toDate == null) || (fromDate.equals("")) || (toDate.equals(""))) {
			filterQuery = "";
		} else {
			filterQuery = filterQuery + (filterQuery.equals("") ? " WHERE " : " AND ")
					+ " ( content_published_date between '" + fromDate + "' and '" + toDate + "') ";
		}
		if ((audience != null) && (!audience.equals(""))) {
			String audienceFilter = cxiqFilter.getAudienceFilter(audience);
			if (audienceFilter != null) {
				filterQuery = filterQuery + (filterQuery.equals("") ? " WHERE " : " AND ") + audienceFilter;
			}
		}
		if ((topicsFilter != null) && (!topicsFilter.equals(""))) {
			String topicsQueryFilter = cxiqFilter.getTopicsQueryFilter(columns, topicsFilter);
			if (topicsQueryFilter != null) {
				topicsQueryFilter = topicsQueryFilter.replace("AND", "OR");
				filterQuery = filterQuery + (filterQuery.equals("") ? " WHERE " : " AND ") + topicsQueryFilter;
			}
		}
		if (filter != null && !filter.equals("")) {
			String hierarchyFilter = cxiqFilter.getHierarchyFilter(filter);
			if (hierarchyFilter != null && !"".equalsIgnoreCase(hierarchyFilter)) {
				filterQuery = filterQuery + (filterQuery.equals("") ? " WHERE " : " AND ") + hierarchyFilter;

			}
			String categoryFilter = cxiqFilter.getCategoryFilter(filter);
			if (categoryFilter != null && !"".equalsIgnoreCase(categoryFilter)) {
				filterQuery = filterQuery + (filterQuery.equals("") ? " WHERE " : " AND ") + categoryFilter;
			}
		}
		query = "select decooda_content_id,sourcedata_id,sourcedata_author,content, CASE WHEN sourcedata_nps_score > 8 THEN 'Promoter' WHEN sourcedata_nps_score < 7 THEN 'Detractor'ELSE 'Passive' END AS emotion "
				+ "FROM " + npsTable;

		query = query + filterQuery;
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("POSTSTream QUERY: \n " + query);
		}
		query = query + " limit " + pageSize + " offset " + offset;

		result = dbUtils.getJSONResultOnQuery(query, "CXIQ");

		return result;
	}

	@RequestMapping(value = "/topicgroup/messages/summary", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getTopicGroupMessagesSummary(@RequestParam("from") String fromDate, @RequestParam("to") String toDate,
			@RequestParam("audience") String audience, @RequestParam("filter") String topicsFilter,
			@RequestParam("tenantId") String tenantId, @RequestParam("brand") String brand, String filter) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Get messages/summary with following parameters \n , fromDate = " + fromDate + ", toDate = "
					+ toDate);
		}
		String npsTable = dbUtils.getCXIQTable(tenantId, brand);
		List<String> columns = getColumns(npsTable);
		String query = "";
		String result = "{\"Posts\":0}";

		String filterQuery = "";
		if ((fromDate == null) || (toDate == null) || (fromDate.equals("")) || (toDate.equals(""))) {
			filterQuery = "";
		} else {
			filterQuery = filterQuery + (filterQuery.equals("") ? " WHERE " : " AND ")
					+ " ( content_published_date between '" + fromDate + "' and '" + toDate + "') ";
		}
		if ((audience != null) && (!audience.equals(""))) {
			String audienceFilter = cxiqFilter.getAudienceFilter(audience);
			if (audienceFilter != null) {
				filterQuery = filterQuery + (filterQuery.equals("") ? " WHERE " : " AND ") + audienceFilter;
			}
		}
		if ((topicsFilter != null) && (!topicsFilter.equals(""))) {
			String topicsQueryFilter = cxiqFilter.getTopicsQueryFilter(columns, topicsFilter);
			if (topicsQueryFilter != null) {
				topicsQueryFilter = topicsQueryFilter.replace("AND", "OR");
				filterQuery = filterQuery + (filterQuery.equals("") ? " WHERE " : " AND ") + topicsQueryFilter;
			}
		}
		if (filter != null && !filter.equals("")) {
			String hierarchyFilter = cxiqFilter.getHierarchyFilter(filter);
			if (hierarchyFilter != null && !"".equalsIgnoreCase(hierarchyFilter)) {
				filterQuery = filterQuery + (filterQuery.equals("") ? " WHERE " : " AND ") + hierarchyFilter;

			}
			String categoryFilter = cxiqFilter.getCategoryFilter(filter);
			if (categoryFilter != null && !"".equalsIgnoreCase(categoryFilter)) {
				filterQuery = filterQuery + (filterQuery.equals("") ? " WHERE " : " AND ") + categoryFilter;
			}
		}
		query = "select  CASE WHEN sourcedata_nps_score > 8 THEN 'Promoter' WHEN sourcedata_nps_score < 7 THEN 'Detractor'ELSE 'Passive' END AS emotion,count(sourcedata_nps_score) as count "
				+ "FROM " + npsTable;
		query = query + filterQuery + " group by emotion";
		result = dbUtils.getJSONResultOnQuery(query, "CXIQ");
		return result;
	}

	@RequestMapping(value = "/brand/data", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public String importBrandData(@RequestParam("tenantId") String tenantId, @RequestParam("brand") String brand,
			@RequestParam("type") String type, @FormDataParam("file") InputStream fileInputStream,
			@FormDataParam("file") FormDataContentDisposition fileInputDetails) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Import data for brand : tenantId=" + tenantId + ",brand=" + brand + ",type=" + type);
		String result = "{\"nps\":\"false\"}";
		result = brandService.importData(tenantId, brand, fileInputStream, type);
		return result;
	}

	@RequestMapping(value = "/brand/data", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getBrandData(@RequestParam("tenantId") String tenantId, @RequestParam("brand") String brand,
			@RequestParam("type") String type) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Get data for brand : tenantId=" + tenantId + ",brand=" + brand + ",type=" + type);
		String result = null;
		result = brandService.getData(tenantId, brand, type);
		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		}
		return result;
	}

	private String getEmotionTrendQuery(String npsTable, Set<String> emotions) {
		List<String> columns = getColumns(npsTable);

		String query = "SELECT content_published_date as 'day',	ROUND( (SUM(CASE WHEN (";
		String emotionPrefix = "emotion_";
		int numTopics = 0;
		for (String name : columns) {
			if (name.startsWith(emotionPrefix) && emotions.contains(name)) {
				if (numTopics != 0) {
					query += " OR ";
				}
				query += "`" + name + "` != 0 ";
				numTopics++;
			}
		}

		query += ") THEN 1 ELSE 0 END)) *100.0/count(*),2) emotion FROM " + npsTable;
		return query;
	}

	public class TopicValueComparator implements Comparator<Float> {

		private String sortOrder;

		TopicValueComparator(String sortOrder) {
			this.sortOrder = sortOrder;
		}

		@Override
		public int compare(Float arg0, Float arg1) {
			if ("asc".equalsIgnoreCase(sortOrder))
				return arg0.compareTo(arg1);
			else
				return arg0.compareTo(arg1) * -1;

		}

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

	private HashMap<String, String> getAllColumnComments(Connection connection, String npsTable) {
		String query = "SHOW FULL COLUMNS FROM " + npsTable + " where field like 'topic~%' ";
		String column = "", comment = "";
		HashMap<String, String> colCommentMap = new HashMap<String, String>();
		try {
			Statement stmt = connection.createStatement();
			ResultSet rs1 = stmt.executeQuery(query);

			while (rs1.next()) {
				column = rs1.getString("field");
				comment = rs1.getString("comment");
				if (comment == null || "".equalsIgnoreCase(comment)) {
					String topic[] = column.split("~");
					colCommentMap.put(column, topic[1]);
				} else
					colCommentMap.put(column, comment);
			}

			rs1.close();
			stmt.close();
		} catch (Exception e) {
			LOGGER.error("getResultForQuery() Exception: " + e.getMessage());
		}
		return colCommentMap;
	}

	// -----Services for Activiti Reports-----

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

	@RequestMapping(value = "/users", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getUsers(@RequestParam("tenantId") String tenantId, @RequestParam("brand") String brand,
			@RequestParam("emailLike") String emailLike) {
		String reportQuery = "";
		reportQuery = qg.generateUsersQuery(tenantId, emailLike);
		String result = null;
		result = dbUtils.getJSONResultOnQuery(reportQuery, "ACTIVITI");
		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		}
		return result;
	}

	@RequestMapping(value = "/reports/tasks", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getTasks(@RequestParam("tenantId") String tenantId, @RequestParam("brand") String brand,
			@RequestParam("processName") String processName, @RequestParam("taskDescription") String taskDescription,
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

	@RequestMapping(value = "/tasks/{taskId}/reassign", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String reassignTask(@PathVariable("taskId") String taskId, @RequestParam("fromUserID") String fromUserID,
			@RequestParam("toUserID") String toUserID) {

		if (LOGGER.isDebugEnabled())
			LOGGER.debug("ReAssign Task : taskId= " + taskId + ",fromUserID=" + fromUserID + ",toUserID=" + toUserID);
		String result = "{\"nps\":0}";
		boolean result1 = qg.reassignTask(taskId, fromUserID, toUserID);
		if (result1)
			result = "{\"nps\":1}";
		return result;

	}
}
