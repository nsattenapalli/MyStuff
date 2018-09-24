package com.cxiq.service.cxiq;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.cxiq.service.utils.DBUtils;

@Service
public class TopicGroupService {
	static final Logger LOGGER = Logger.getLogger(TopicGroupService.class.getName());

	@Qualifier("cxiq")
	@Autowired
	DataSource cxiqDatasource;

	@Autowired
	DBUtils dbUtils;

	@Autowired
	CXIQFilterService cxiqFilter;

	public String updateTopicGroupForBrand(String tenantId, String brand, String topicGroup, String topics) {
		String result = "{\"nps\":\"false\"}";
		;
		Connection connection = null;
		Statement stmt = null;
		try {
			connection = cxiqDatasource.getConnection();
			stmt = connection.createStatement();
			String updateQuery = "";
			String updateField = "";

			if (tenantId == null || "".equalsIgnoreCase(tenantId) || brand == null && "".equalsIgnoreCase(brand)
					|| topicGroup == null && "".equalsIgnoreCase(topicGroup))
				return result;

			if (topics != null && !"".equalsIgnoreCase(topics))
				updateField = updateField + " topics='" + topics + "' ";
			else
				updateField = updateField + " topics='' ";

			String query1 = "select topic_group from " + tenantId + ".brand_topicgroup_map where brand='" + brand
					+ "' and topic_group='" + topicGroup + "' ";
			ResultSet rs1 = stmt.executeQuery(query1);
			if (!rs1.next()) {
				updateQuery = "insert into  " + tenantId + ".brand_topicgroup_map " + " values ( " + "'" + brand + "','"
						+ topicGroup + "','" + topics + "')";
			} else {

				if (!"".equalsIgnoreCase(updateField)) {
					updateQuery = "update " + tenantId + ".brand_topicgroup_map " + " set " + updateField
							+ " where brand='" + brand + "' and topic_group='" + topicGroup + "'";

				}

			}
			int rs = stmt.executeUpdate(updateQuery);
			if (rs >= 0)
				result = "{\"nps\":\"true\"}";

		} catch (Exception e) {
			LOGGER.error("In updateTopicGroupForBrand method catch block due to " + e);
		} finally {
			dbUtils.closeStatement(stmt);
			dbUtils.closeConnection(connection);

		}
		return result;
	}

	public String deleteTopicGroupForBrand(String tenantId, String brand, String topicGroup) {
		String result = "{\"nps\":\"false\"}";
		;
		Connection connection = null;
		Statement stmt = null;
		try {
			connection = cxiqDatasource.getConnection();
			stmt = connection.createStatement();

			if (tenantId == null || "".equalsIgnoreCase(tenantId) || brand == null && "".equalsIgnoreCase(brand)
					|| topicGroup == null && "".equalsIgnoreCase(topicGroup))
				return result;

			String query1 = "delete from " + tenantId + ".brand_topicgroup_map where brand='" + brand
					+ "' and topic_group='" + topicGroup + "' ";
			int rs1 = stmt.executeUpdate(query1);
			if (rs1 >= 0)
				result = "{\"nps\":\"true\"}";

		} catch (Exception e) {
			LOGGER.error("In deleteTopicGroupForBrand method catch block due to " + e);
		} finally {
			dbUtils.closeStatement(stmt);
			dbUtils.closeConnection(connection);

		}
		return result;
	}

	public String getZScoreForTopics(String tenantId, String brand, String topicGroup, String topic, String fromDate,
			String toDate, String filter) {
		String result = "{\"nps\":\"false\"}";
		String topicQuery = "";
		Connection connection = null;
		Statement stmt = null;
		try {
			if (topicGroup != null && !"".equalsIgnoreCase(topicGroup)) {
				String topicGroupQuery = " SELECT * from  " + tenantId + ".brand_topicgroup_map where brand='" + brand
						+ "' and topic_group='" + topicGroup + "'";

				connection = cxiqDatasource.getConnection();
				stmt = connection.createStatement();
				ResultSet rs = stmt.executeQuery(topicGroupQuery);
				if (rs.next()) {
					String topicsCol = rs.getString("topics");
					if (topicsCol != null && !"".equalsIgnoreCase(topicsCol)) {
						String[] topicsArray = topicsCol.split(",");
						for (int i = 0; i < topicsArray.length; i++) {
							if ("".equalsIgnoreCase(topicQuery))
								topicQuery = topicQuery + " `" + topicsArray[i] + "` !=0 ";
							else
								topicQuery = topicQuery + " or `" + topicsArray[i] + "` !=0 ";
						}
					}

				}
			} else if (topic != null && !"".equalsIgnoreCase(topic)) {
				topicQuery = topicQuery + " `" + topic + "` !=0 ";
			} else
				return result;
			String cxiqTable = dbUtils.getCXIQTable(tenantId, brand);

			String filterQuery = "";
			filterQuery = " WHERE " + topicQuery;

			if (fromDate == null || toDate == null || fromDate.equals("") || toDate.equals("")) {
			} else {
				filterQuery += ((filterQuery.equals("")) ? " WHERE " : " AND ") + " ( content_published_date between '"
						+ fromDate + "' and '" + toDate + "') ";
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

			String query = " SELECT content_published_date day,ROUND((AVG(sourcedata_nps_score)-avgnps_table.tot_avg_nps)/avgnps_table.tot_std_nps,2) as z_score, "
					+ " ROUND((9-avgnps_table.tot_avg_nps)/avgnps_table.tot_std_nps,2) as base_z_score " + " FROM "
					+ cxiqTable + " , "
					+ " ( SELECT   ROUND(AVG(avg_nps),2) AS tot_avg_nps,ROUND(STD(avg_nps),2) AS tot_std_nps from "
					+ " ( SELECT   content_published_date,AVG(sourcedata_nps_score) AS avg_nps FROM " + cxiqTable
					+ filterQuery + " group by content_published_date order by content_published_date asc) as P "
					+ " ) as avgnps_table " + filterQuery
					+ " group by content_published_date,avgnps_table.tot_avg_nps,avgnps_table.tot_std_nps order by content_published_date asc ";
			result = dbUtils.getJSONResultOnQuery(query, "CXIQ");

		} catch (Exception e) {
			LOGGER.error("In getTopicsForTopicGroupAsList method catch block due to " + e);
		} finally {
			dbUtils.closeStatement(stmt);
			dbUtils.closeConnection(connection);

		}
		return result;
	}
}
