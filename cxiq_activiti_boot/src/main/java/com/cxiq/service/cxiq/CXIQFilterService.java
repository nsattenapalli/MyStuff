package com.cxiq.service.cxiq;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.cxiq.service.utils.DBUtils;

@Service
public class CXIQFilterService {
	static final Logger LOGGER = Logger.getLogger(CXIQFilterService.class.getName());

	@Qualifier("cxiq")
	@Autowired
	DataSource cxiqDataSource;

	@Autowired
	DBUtils dbUtils;

	public String getHierarchiesOrCategoriesQuery(String cxiqTable, String name, String value, String columnNames) {

		String query = " select ";
		String columns = " ", emptyChecks = "";
		;
		String[] colNames = columnNames.split(",");
		int countOfColumns = colNames.length;
		if (countOfColumns == 0)
			query = query + " sourcedata_" + name;
		for (int i = 0; i < countOfColumns; i++) {
			columns = columns + " sourcedata_" + colNames[i];
			emptyChecks = emptyChecks + " and sourcedata_" + colNames[i] + " != '' and sourcedata_" + colNames[i]
					+ " != 'N/A' and " + " sourcedata_" + colNames[i] + " IS NOT NULL ";
			if (i < countOfColumns - 1) {
				columns = columns + ",";

			}
		}
		query = query + columns + " as name FROM " + cxiqTable + " where 1=1";
		if (name != null && !"".equalsIgnoreCase(name) && value != null && !"".equalsIgnoreCase(value)) {
			query = query + " and sourcedata_" + name + " in( " + value + ")";
		}
		if (emptyChecks != null && !"".equalsIgnoreCase(emptyChecks))
			query = query + emptyChecks;
		query = query + " group by " + columns;
		query = query + " order by " + columns + " asc ";

		return query;
	}

	public String getAllHierarchiesQuery(String cxiqTable) {

		String query = " select sourcedata_country,sourcedata_region,sourcedata_state,sourcedata_district,sourcedata_location, "
				+ " sourcedata_store,sourcedata_hotel from " + cxiqTable
				+ " group by sourcedata_country,sourcedata_region,sourcedata_state,sourcedata_district,sourcedata_location, "
				+ " sourcedata_store,sourcedata_hotel ";
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
			dbUtils.closeStatement(stmt);
			dbUtils.closeConnection(connection);

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
			dbUtils.closeStatement(stmt);
			dbUtils.closeConnection(connection);

		}
		return result;
	}

	public String getHierarchyFilter(String hierarchies) {
		String query = "";
		JSONObject object = null;
		try {
			object = new JSONObject(hierarchies);

			JSONArray jArray = object.getJSONArray("hierarchies");
			for (int i = 0; i < jArray.length(); i++) {
				JSONObject object3 = jArray.getJSONObject(i);
				if (!"".equalsIgnoreCase(query))
					query = query + " and ";
				if (object3.has("name") && object3.has("values")) {
					String columnName = (String) object3.get("name");
					String columnValues = (String) object3.get("values");
					query = query + " sourcedata_" + columnName + " in (" + columnValues + ")";
				}
			}

		} catch (JSONException e) {
			LOGGER.error("Error while converting to JSONObject" + e.getMessage());

		}
		return query;
	}

	public String getCategoryFilter(String categories) {
		String query = "";
		JSONObject object = null;
		try {
			object = new JSONObject(categories);

			JSONArray jArray = object.getJSONArray("categories");
			for (int i = 0; i < jArray.length(); i++) {
				JSONObject object3 = jArray.getJSONObject(i);
				if (!"".equalsIgnoreCase(query))
					query = query + " and ";
				if (object3.has("name") && object3.has("values")) {
					String columnName = (String) object3.get("name");
					String columnValues = (String) object3.get("values");
					query = query + " sourcedata_" + columnName + " in (" + columnValues + ")";
				}
			}

		} catch (JSONException e) {
			LOGGER.error("Error while converting to JSONObject" + e.getMessage());
		}
		return query;
	}

	public String getAudienceFilter(String audienceValue) {
		String query = " ";
		if (audienceValue.equalsIgnoreCase("detractor")) {
			query = query + " sourcedata_nps_score < 7 ";
		} else if (audienceValue.equalsIgnoreCase("promoter")) {
			query = query + " sourcedata_nps_score > 8 ";
		} else if (audienceValue.equalsIgnoreCase("passive")) {
			query = query + " (sourcedata_nps_score = 7 or sourcedata_nps_score = 8) ";
		}
		return query;
	}

	public String getTopicsQueryFilter(List<String> columns, String filters) {

		String queryFilter = "";

		int numConditions = 0;
		for (String prefix : filters.split(",")) {
			if ((prefix != null) && (prefix.trim().length() != 0)) {
				if (numConditions == 0) {
					queryFilter = queryFilter + " ( ";
				} else {
					queryFilter = queryFilter + " AND ( ";
				}
				int numColumns = 0;
				for (String name : columns) {
					if (name.startsWith(prefix)) {
						if ((!prefix.equals("purchase_path")) || (!name.startsWith("purchase_path_purch_intent"))) {
							if (numColumns != 0) {
								queryFilter = queryFilter + " OR ";
							}
							queryFilter = queryFilter + "`" + name + "` != 0 ";
							numColumns++;
						}
					}
				}
				queryFilter = queryFilter + ") ";
				numConditions++;
			}
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Topics filter query is " + queryFilter);
		}
		return queryFilter;
	}

}
