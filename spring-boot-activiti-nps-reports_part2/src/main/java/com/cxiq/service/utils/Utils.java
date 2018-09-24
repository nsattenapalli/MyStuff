package com.cxiq.service.utils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class Utils {

	private static final Logger LOGGER = Logger.getLogger(Utils.class.getName());

	public JSONObject asJsonObject(int numColumns, ResultSetMetaData rsmd, ResultSet rs) throws JSONException {
		JSONObject obj = new JSONObject();
		for (int i = 1; i < numColumns + 1; i++) {
			try {
				String colName = rsmd.getColumnLabel(i);
				String colValue = rs.getString(i);
				if (colValue != null) {
					obj.put(colName, colValue.trim());
				} else {
					obj.put(colName, 0);
				}
			} catch (SQLException e) {

			}
		}
		return obj;
	}

	public String getCurrentDateTime() {
		java.util.Date dt = new java.util.Date();
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String currentDateTime = sdf.format(dt);
		return currentDateTime;
	}

	public long getNoOfDaysFromDate(String date) {

		long days = 0;
		Date date1 = null;
		try {
			date1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(date);
			Date currentDateTime = new Date();
			days = (currentDateTime.getTime() - date1.getTime()) / (24 * 60 * 60 * 1000);
		} catch (ParseException e) {
			LOGGER.error("Unable to get the date diff - '" + date + "' : " + e);
		}
		return days;
	}
}
