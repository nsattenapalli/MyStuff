package com.cxiq.service.cxiq;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.sql.DataSource;

import org.activiti.engine.impl.util.json.JSONArray;
import org.activiti.engine.impl.util.json.JSONObject;
import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.cxiq.service.utils.DBUtils;

@Service
public class BrandService {

	static final Logger LOGGER = Logger.getLogger(BrandService.class.getName());

	@Qualifier("cxiq")
	@Autowired
	DataSource cxiqDataSource;

	@Autowired
	DBUtils dbUtils;

	public String importHierarchy(String tenantId, String brand, InputStream fileInputStream, String type) {
		String result = "{\"nps\":\"false\"}";
		try {
			Workbook workbook = new XSSFWorkbook(fileInputStream);
			Sheet datatypeSheet = workbook.getSheetAt(0);
			Iterator<Row> iterator = datatypeSheet.iterator();
			String tableName = type + "_" + brand;
			Connection connection = null;
			Statement stmt = null;
			try {
				connection = cxiqDataSource.getConnection();
				stmt = connection.createStatement();
				ArrayList<String> header = new ArrayList<String>();
				while (iterator.hasNext()) {
					Row currentRow = iterator.next();
					Iterator<Cell> cellIterator = currentRow.iterator();
					String tableCols = "";

					HashMap<String, String> values = new HashMap<String, String>();
					String colValues = "";
					while (cellIterator.hasNext()) {
						Cell currentCell = cellIterator.next();
						String columnData = currentCell.getStringCellValue();

						if (currentRow.getRowNum() == 0) {
							header.add(columnData);
						} else {
							values.put(header.get(currentCell.getColumnIndex()), columnData);
							if ("".equalsIgnoreCase(colValues))
								colValues = colValues + "'" + columnData + "'";
							else
								colValues = colValues + " , " + "'" + columnData + "'";
						}
					}
					if (currentRow.getRowNum() == 0) {
						if (!checkIfTableExists(tenantId, tableName)) {

							for (Iterator<String> iterator2 = header.iterator(); iterator2.hasNext();) {
								String headerVal = (String) iterator2.next();
								if ("".equalsIgnoreCase(tableCols))
									tableCols = tableCols + headerVal + " varchar(255) NOT NULL ";
								else
									tableCols = tableCols + " , " + headerVal + " varchar(255) NOT NULL ";
							}
							String createTableQuery = "create table " + tenantId + "." + tableName + " ( " + tableCols
									+ ") ";
							int rs1 = stmt.executeUpdate(createTableQuery);
							if (rs1 < 0)
								return "{\"nps\":\"false\"}";
						} else {
							if (!checkForColumns(tenantId, brand, type, header))
								return "{\"nps\":\"Columns doesn't match\"}";
						}
					} else {
						String status = updateData(tenantId, brand, type, null, values);
						if (status.contains("false"))
							return result;
					}
				}

			} catch (Exception e) {

				LOGGER.error("In importHierarchy method catch block due to " + e);
				return result;
			} finally {
				workbook.close();
				dbUtils.closeStatement(stmt);
				dbUtils.closeConnection(connection);

			}
		} catch (FileNotFoundException e) {
			LOGGER.error("In importHierarchy method catch block due to " + e);
			return result;
		} catch (IOException e) {
			LOGGER.error("In importHierarchy method catch block due to " + e);
			return result;
		}
		result = "{\"nps\":\"true\"}";
		return result;
	}

	private boolean checkForColumns(String tenantId, String brand, String type, ArrayList<String> header) {
		boolean result = false;
		ArrayList<String> columns = getColumns(tenantId, brand, type);
		for (Iterator<String> iterator2 = header.iterator(); iterator2.hasNext();) {
			if (!columns.contains(iterator2.next())) {
				result = false;
				break;
			} else
				result = true;
		}
		return result;
	}

	public ArrayList<String> getColumns(String tenantId, String brand, String type) {
		String query = " select COLUMN_NAME from INFORMATION_SCHEMA.COLUMNS  where  TABLE_SCHEMA =  '" + tenantId
				+ "' and TABLE_NAME='" + brand + "_" + type + "'";
		Connection connection = null;
		Statement stmt = null;
		ArrayList<String> columns = new ArrayList<String>();
		try {
			connection = cxiqDataSource.getConnection();
			stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery(query);

			while (rs.next())
				columns.add(rs.getString("COLUMN_NAME"));
		} catch (Exception e) {
			LOGGER.error("In importData method catch block due to " + e);
		} finally {
			dbUtils.closeStatement(stmt);
			dbUtils.closeConnection(connection);

		}
		return columns;
	}

	public String getHierarchyData(String tenantId, String brand, String type, String pageSize, String offset,
			String like) {
		String result = null;
		String likeQuery = "";
		String query = " select * from  " + tenantId + "." + type + "_" + brand;

		if (like != null && !"".equalsIgnoreCase(like)) {
			ArrayList<String> cols = getColumns(tenantId, brand, type);
			for (Iterator<String> iterator = cols.iterator(); iterator.hasNext();) {
				String col = (String) iterator.next();
				if ("".equalsIgnoreCase(likeQuery))
					likeQuery = likeQuery + col + " like '%" + like + "%'";
				else
					likeQuery = likeQuery + " or " + col + " like '%" + like + "%'";
			}
			if (!"".equalsIgnoreCase(likeQuery))
				likeQuery = " where " + likeQuery;
		}
		query = query + likeQuery;
		if ((offset == null) || (offset.equals(""))) {
			offset = "0";
		}
		if ((pageSize == null) || (pageSize.equals(""))) {
			pageSize = "10";
		}
		query = query + " limit " + pageSize + " offset " + offset;
		result = dbUtils.getJSONResultOnQuery(query, "CXIQ");
		return result;
	}

	public String updateHierarchyData(String tenantId, String brand, String type, String body) {
		String result = "{\"nps\":\"false\"}";
		if (body != null && !"".equalsIgnoreCase(body)) {
			JSONArray a = new JSONArray(body);
			for (int i = 0; i < a.length(); i++) {
				HashMap<String, String> oldValues = new HashMap<String, String>();
				HashMap<String, String> newValues = new HashMap<String, String>();

				JSONObject recordsObj = new JSONObject();
				recordsObj = (JSONObject) a.get(i);
				JSONObject recordObj = recordsObj.getJSONObject("record");
				JSONObject oldObj = recordObj.getJSONObject("oldValue");
				JSONObject newObj = recordObj.getJSONObject("newValue");

				for (Iterator iterator = newObj.keys(); iterator.hasNext();) {
					String header = (String) iterator.next();
					newValues.put(header, (String) newObj.get(header));
				}
				for (Iterator iterator = oldObj.keys(); iterator.hasNext();) {
					String header = (String) iterator.next();
					oldValues.put(header, (String) oldObj.get(header));
				}
				// update record
				if ((oldValues != null && oldValues.size() > 0) && (newValues != null && newValues.size() > 0))
					result = updateData(tenantId, brand, type, oldValues, newValues);
				// delete record
				if ((oldValues != null && oldValues.size() > 0) && (newValues == null || newValues.size() == 0))
					result = deleteHierarchyData(tenantId, brand, type, oldValues, false);
				// insert record
				if ((newValues != null && newValues.size() > 0) && (oldValues == null || oldValues.size() == 0))
					result = updateData(tenantId, brand, type, oldValues, newValues);
				if ((newValues == null || newValues.size() == 0) && (oldValues == null || oldValues.size() == 0))
					return result;
			}
		}
		return result;
	}

	public String updateData(String tenantId, String brand, String type, HashMap<String, String> oldValues,
			HashMap<String, String> newValues) {
		String result = "{\"nps\":\"false\"}";
		String whereCondition = "", colValues = "", updateColVals = "";

		HashMap<String, String> objMap = new HashMap<String, String>();
		if (oldValues != null && oldValues.size() > 0)
			objMap = oldValues;
		else
			objMap = newValues;
		String colNames = "";
		for (Map.Entry<String, String> entry : objMap.entrySet()) {
			String header = entry.getKey();
			if ("".equalsIgnoreCase(whereCondition)) {
				if (oldValues != null && oldValues.size() > 0)
					whereCondition = whereCondition + header + "= '" + oldValues.get(header) + "'";
				else
					whereCondition = whereCondition + header + "= '" + newValues.get(header) + "'";

				colNames = colNames + header;
				colValues = colValues + "'" + newValues.get(header) + "'";
				updateColVals = updateColVals + header + "= '" + newValues.get(header) + "'";
			} else {
				if (oldValues != null && oldValues.size() > 0)
					whereCondition = whereCondition + " and " + header + "= '" + oldValues.get(header) + "'";
				else
					whereCondition = whereCondition + " and " + header + "= '" + newValues.get(header) + "'";
				colNames = colNames + "," + header;
				colValues = colValues + " , " + "'" + newValues.get(header) + "'";
				updateColVals = updateColVals + " , " + header + "= '" + newValues.get(header) + "'";
			}
		}

		String query = " select * from  " + tenantId + "." + type + "_" + brand + " where " + whereCondition;
		Connection connection = null;
		Statement stmt = null;
		try {
			connection = cxiqDataSource.getConnection();
			stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			if (rs.next()) {
				if (oldValues != null && oldValues.size() > 0) {
					String insertQuery = "update  " + tenantId + "." + type + "_" + brand + " set  " + updateColVals
							+ " where " + whereCondition;
					int rs1 = stmt.executeUpdate(insertQuery);
					if (rs1 > 0)
						return "{\"nps\":\"true\"}";
				} else
					return "{\"nps\":\"true\"}";
			} else {
				String insertQuery = "insert into " + tenantId + "." + type + "_" + brand + "( " + colNames
						+ ") values ( " + colValues + " )";
				int rs1 = stmt.executeUpdate(insertQuery);
				if (rs1 > 0)
					return "{\"nps\":\"true\"}";

			}
		} catch (Exception e) {
			LOGGER.error("In updateData method catch block due to " + e);
		} finally {
			dbUtils.closeStatement(stmt);
			dbUtils.closeConnection(connection);
		}
		return result;
	}

	public boolean checkIfTableExists(String tenantId, String tableName) {
		boolean result = false;
		Connection connection = null;
		Statement stmt = null;
		try {
			connection = cxiqDataSource.getConnection();
			stmt = connection.createStatement();
			String query = "SHOW TABLES FROM " + tenantId + " LIKE '" + tableName + "';";
			ResultSet rs = stmt.executeQuery(query);
			if (rs.next())
				result = true;
			else
				result = false;
		} catch (Exception e) {
			LOGGER.error("In checkIfTableExists method catch block due to " + e);
			return false;
		} finally {
			dbUtils.closeStatement(stmt);
			dbUtils.closeConnection(connection);

		}
		return result;
	}

	public String deleteHierarchyData(String tenantId, String brand, String type, HashMap<String, String> oldValues,
			boolean fullTable) {
		String result = "{\"nps\":\"false\"}";
		String query = "";
		if (fullTable) {
			query = " drop table  " + tenantId + "." + type + "_" + brand;
		} else {
			query = " delete from " + tenantId + "." + type + "_" + brand;
			String whereCondition = "";
			for (Map.Entry<String, String> entry : oldValues.entrySet()) {
				String header = entry.getKey();
				String value = oldValues.get(header);
				if ("".equalsIgnoreCase(whereCondition))
					whereCondition = whereCondition + header + "= '" + value + "'";
				else
					whereCondition = whereCondition + " and " + header + "= '" + value + "'";
			}
			query = query + " where " + whereCondition;
		}

		Connection connection = null;
		Statement stmt = null;
		try {
			connection = cxiqDataSource.getConnection();
			stmt = connection.createStatement();
			int rs = stmt.executeUpdate(query);
			if (rs >= 0)
				return "{\"nps\":\"true\"}";

		} catch (Exception e) {
			LOGGER.error("In deleteHierarchyData method catch block due to " + e);
		} finally {
			dbUtils.closeStatement(stmt);
			dbUtils.closeConnection(connection);
		}
		return result;
	}

	public String getCountOfHierarchyData(String tenantId, String brand, String type, String like) {
		String result = null;
		String query = " select count(*) as count from  " + tenantId + "." + type + "_" + brand;
		String likeQuery = "";

		if (like != null && !"".equalsIgnoreCase(like)) {
			ArrayList<String> cols = getColumns(tenantId, brand, type);
			for (Iterator<String> iterator = cols.iterator(); iterator.hasNext();) {
				String col = (String) iterator.next();
				if ("".equalsIgnoreCase(likeQuery))
					likeQuery = likeQuery + col + " like '%" + like + "%'";
				else
					likeQuery = likeQuery + " or " + col + " like '%" + like + "%'";
			}
			if (!"".equalsIgnoreCase(likeQuery))
				likeQuery = " where " + likeQuery;
		}
		query = query + likeQuery;
		result = dbUtils.getJSONResultOnQuery(query, "CXIQ");
		return result;
	}

	public String getLevelsForBrand(String tenantId, String brand) {
		String result = null;

		String hierarchyLevels = "", categoryLevels = "";
		JSONArray jArray = new JSONArray();

		JSONObject brandObj = new JSONObject();
		brandObj.put("brand", brand);
		jArray.put(brandObj);

		ArrayList<String> hiearchies = getColumns(tenantId, brand, "hierarchies");
		for (Iterator<String> iterator = hiearchies.iterator(); iterator.hasNext();) {
			String col = (String) iterator.next();
			if (!"".equalsIgnoreCase(hierarchyLevels))
				hierarchyLevels = hierarchyLevels + "," + col;
			else
				hierarchyLevels = hierarchyLevels + col;
		}
		JSONObject hierarchiesObj = new JSONObject();
		hierarchiesObj.put("hierarchies", hierarchyLevels);
		jArray.put(hierarchiesObj);

		ArrayList<String> categories = getColumns(tenantId, brand, "categories");
		for (Iterator<String> iterator = categories.iterator(); iterator.hasNext();) {
			String col = (String) iterator.next();
			if (!"".equalsIgnoreCase(categoryLevels))
				categoryLevels = categoryLevels + "," + col;
			else
				categoryLevels = categoryLevels + col;
		}
		JSONObject categoriesObj = new JSONObject();
		categoriesObj.put("categories", categoryLevels);
		jArray.put(categoriesObj);

		result = jArray.toString();
		return result;
	}

}