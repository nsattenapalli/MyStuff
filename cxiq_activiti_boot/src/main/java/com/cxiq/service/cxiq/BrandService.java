package com.cxiq.service.cxiq;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;

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

	public String importData(String tenantId, String brand, InputStream fileInputStream, String type) {
		String result = "{\"nps\":\"false\"}";
		try {
			Workbook workbook = new XSSFWorkbook(fileInputStream);
			Sheet datatypeSheet = workbook.getSheetAt(0);
			Iterator<Row> iterator = datatypeSheet.iterator();
			String tableName = brand + "_" + type;
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

					ArrayList<String> values = new ArrayList<String>();
					String colValues = "";
					while (cellIterator.hasNext()) {
						Cell currentCell = cellIterator.next();
						String columnData = currentCell.getStringCellValue();

						if (currentRow.getRowNum() == 0) {
							header.add(columnData);
						} else {
							values.add(columnData);
							if ("".equalsIgnoreCase(colValues))
								colValues = colValues + "'" + columnData + "'";
							else
								colValues = colValues + " , " + "'" + columnData + "'";
						}
					}
					if (currentRow.getRowNum() == 0) {
						if (!checkIfTableExists(tableName)) {

							for (Iterator<String> iterator2 = header.iterator(); iterator2.hasNext();) {
								String headerVal = (String) iterator2.next();
								if ("".equalsIgnoreCase(tableCols))
									tableCols = tableCols + headerVal + " varchar(255) NOT NULL ";
								else
									tableCols = tableCols + " , " + headerVal + " varchar(255) NOT NULL ";
							}
							String createTableQuery = "create table " + tableName + " ( " + tableCols + ") ";
							// stmt.addBatch(createTableQuery);
							int rs1 = stmt.executeUpdate(createTableQuery);
							if (rs1 <= 0)
								return "{\"nps\":\"false\"}";
						} else {
							if (!checkForColumns(tenantId, brand, type, header))
								return "{\"nps\":\"Columns doesn't match\"}";
						}
					} else {
						String status = updateData(tenantId, brand, type, header, null, values);
						if (status.contains("false"))
							return result;
						// String insertDataQuery="insert into "+tableName+" values ( "+colValues+" )";
						// stmt.addBatch(insertDataQuery);
					}
				}

			} catch (Exception e) {

				LOGGER.error("In importData method catch block due to " + e);
				return result;
			} finally {
				workbook.close();
				dbUtils.closeStatement(stmt);
				dbUtils.closeConnection(connection);

			}
		} catch (FileNotFoundException e) {
			LOGGER.error("In importData method catch block due to " + e);
			return result;
		} catch (IOException e) {
			LOGGER.error("In importData method catch block due to " + e);
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

	public String getData(String tenantId, String brand, String type) {
		String result = null;
		String query = " select * from  " + tenantId + "." + brand + "_" + type;
		result = dbUtils.getJSONResultOnQuery(query, "CXIQ");
		return result;
	}

	public String updateRecords(String tenantId, String brand, String type, String body) {
		String result = "{\"nps\":\"false\"}";
		if (body != null && !"".equalsIgnoreCase(body)) {
			JSONArray a = new JSONArray(body);
			ArrayList<String> headers = new ArrayList<String>();
			ArrayList<String> newValues = new ArrayList<String>();
			ArrayList<String> oldValues = new ArrayList<String>();
			for (int i = 0; i < a.length(); i++) {
				JSONObject recordsObj = new JSONObject();
				recordsObj = (JSONObject) a.get(i);
				JSONObject recordObj = recordsObj.getJSONObject("record");
				JSONObject oldObj = recordObj.getJSONObject("oldValue");
				JSONObject newObj = recordObj.getJSONObject("newValue");
				for (Iterator iterator = newObj.keys(); iterator.hasNext();) {
					String header = (String) iterator.next();
					headers.add(header);
					newValues.add((String) newObj.get(header));
				}
				for (Iterator iterator = oldObj.keys(); iterator.hasNext();) {
					oldValues.add((String) oldObj.get((String) iterator.next()));
				}
				updateData(tenantId, brand, type, headers, oldValues, newValues);
			}
		}
		return result;
	}

	public String updateData(String tenantId, String brand, String type, ArrayList<String> headers,
			ArrayList<String> oldValues, ArrayList<String> newValues) {
		String result = "{\"nps\":\"false\"}";
		String whereCondition = "", colValues = "", updateColVals = "";
		for (int i = 0; i < headers.size(); i++) {
			String header = headers.get(i);
			if ("".equalsIgnoreCase(whereCondition)) {
				if (oldValues.size() > 0)
					whereCondition = whereCondition + header + "= '" + oldValues.get(i) + "'";
				else
					whereCondition = whereCondition + header + "= '" + newValues.get(i) + "'";

				colValues = colValues + "'" + newValues.get(i) + "'";
				updateColVals = updateColVals + header + "= '" + newValues.get(i) + "'";
			} else {
				if (oldValues.size() > 0)
					whereCondition = whereCondition + " and " + header + "= '" + oldValues.get(i) + "'";
				else
					whereCondition = whereCondition + " and " + header + "= '" + newValues.get(i) + "'";

				colValues = colValues + " , " + "'" + newValues.get(i) + "'";
				updateColVals = updateColVals + " , " + header + "= '" + newValues.get(i) + "'";
			}
		}

		String query = " select * from  " + tenantId + "." + brand + "_" + type + " where " + whereCondition;
		Connection connection = null;
		Statement stmt = null;
		try {
			connection = cxiqDataSource.getConnection();
			stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			if (rs.next()) {
				if (oldValues.size() > 0) {
					String insertQuery = "update  " + tenantId + "." + brand + "_" + type + " set  " + updateColVals
							+ " where " + whereCondition;
					int rs1 = stmt.executeUpdate(insertQuery);
					if (rs1 > 0)
						return "{\"nps\":\"true\"}";
				} else
					return "{\"nps\":\"true\"}";
			} else {
				String insertQuery = "insert into " + tenantId + "." + brand + "_" + type + " values ( " + colValues
						+ " )";
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

	public boolean checkIfTableExists(String tableName) {
		boolean result = false;
		Connection connection = null;
		Statement stmt = null;
		try {
			connection = cxiqDataSource.getConnection();
			stmt = connection.createStatement();
			String query = "SHOW TABLES LIKE '" + tableName + "';";
			ResultSet rs = stmt.executeQuery(query);
			if (rs.next())
				result = true;
			else
				result = false;
		} catch (Exception e) {
			LOGGER.error("In createTable method catch block due to " + e);
			return false;
		} finally {
			dbUtils.closeStatement(stmt);
			dbUtils.closeConnection(connection);

		}
		return result;
	}

}