package com.cxiq.service.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.cxiq.constants.CXIQConstants;

@Service
public class DBUtils {
	private static final Logger LOGGER = Logger.getLogger(DBUtils.class.getName());

	@Qualifier("cxiq")
	@Autowired
	DataSource cxiqDatasource;

	@Qualifier("activiti")
	@Autowired
	DataSource activitiDatasource;

	@Autowired
	Utils utils;

	public String getJSONResultOnQuery(String query, String db) {
		JSONArray response = new JSONArray();
		Connection conn = null;
		if (!query.isEmpty()) {
			try {
				if (db.equalsIgnoreCase("CXIQ")) {
					conn = cxiqDatasource.getConnection();
				} else {
					conn = activitiDatasource.getConnection();
				}
				ResultSet rs = conn.createStatement().executeQuery(query);
				ResultSetMetaData rsmd = rs.getMetaData();
				int columnCount = rsmd.getColumnCount();
				while (rs.next()) {
					response.put(utils.asJsonObject(columnCount, rsmd, rs));
				}
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			} finally {
				if (conn != null) {
					try {
						conn.close();
					} catch (SQLException e) {
						LOGGER.error(e);
					}
				}
			}
		}
		return response.toString();
	}

	public boolean executeSQLFile(HashMap<String, String> variables, String filePath, String dbConnection) {
		String s = new String();
		StringBuffer sb = new StringBuffer();
		Connection connection = null;
		boolean result = false;
		try {
			FileReader fr = new FileReader(new File(filePath));
			// be sure to not have line starting with ""--"" or ""/*"" or any other non
			// aplhabetical character

			BufferedReader br = new BufferedReader(fr);

			while ((s = br.readLine()) != null) {
				sb.append(s);
			}
			br.close();

			// here is our splitter ! We use "";"" as a delimiter for each request
			// then we are sure to have well formed statements
			if ("activiti".equalsIgnoreCase(dbConnection))
				connection = activitiDatasource.getConnection();
			else
				connection = cxiqDatasource.getConnection();
			Statement st = connection.createStatement();

			String[] queries = sb.toString().split("\\$\\$");

			for (int i = 0; i < queries.length; i++) {
				// we ensure that there is no spaces before or after the request string
				// in order to not execute empty statements
				String query = queries[i];
				for (String key : variables.keySet()) {
					query = query.replace(key, variables.get(key));
				}
				if (!query.trim().equals("")) {
					st.addBatch(query);
				}
			}
			st.executeBatch();
			result = true;
		} catch (SQLException e) {
			LOGGER.error("Error while executing script file - '" + filePath + "' : " + e.getMessage());
			result = false;
		} catch (FileNotFoundException e) {
			LOGGER.error("Unable to find script file - '" + filePath + "' : " + e.getMessage());
			result = false;
		} catch (IOException e) {
			LOGGER.error("Unable to read script file - '" + filePath + "' : " + e.getMessage());
			result = false;
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

	public String getCXIQTable(String tenantId, String brand) {
		String DEFAULT_TABLE = "";
		String npsTable = "";
		Connection connection = null;
		if (brand != null && !"".equalsIgnoreCase(brand)) {
			DEFAULT_TABLE = brand;
		} else {
			String query = "select * from user_brand_map where user_id= '" + tenantId + "'";
			try {
				connection = cxiqDatasource.getConnection();
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

	public void closeResultSet(ResultSet rs) {
		try {
			if (rs != null)
				rs.close();
		} catch (Exception ex) {
			LOGGER.error("In closeResultSet " + ex);
		}
	}

	public void closeStatement(Statement st) {
		try {
			if (st != null)
				st.close();
		} catch (Exception ex) {
			LOGGER.error("In closeStatement " + ex);
		}
	}

	public void closeConnection(Connection con) {
		try {
			if (con != null)
				con.close();
		} catch (Exception ex) {
			LOGGER.error("In closeConnection " + ex);
		}
	}
}
