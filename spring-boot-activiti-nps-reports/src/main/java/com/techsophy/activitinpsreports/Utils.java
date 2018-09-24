package com.techsophy.activitinpsreports;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class Utils {

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
}
