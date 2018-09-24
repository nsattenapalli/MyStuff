package com.cxiq.service.cxiq;

import java.sql.Connection;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.cxiq.service.utils.DBUtils;

@Service
public class CXIQAdminService {

	static final Logger LOGGER = Logger.getLogger(CXIQAdminService.class.getName());

	@Qualifier("cxiq")
	@Autowired
	DataSource cxiqDatasource;

	@Autowired
	DBUtils dbUtils;

	public String updateTriageStatusForMessages(String tenantId, String brand, String status, int limit) {
		String result = "{\"nps\":\"false\"}";
		Connection connection = null;
		Statement stmt = null;
		try {
			connection = cxiqDatasource.getConnection();
			String cxiqTable = dbUtils.getCXIQTable(tenantId, brand);
			stmt = connection.createStatement();

			String updateQuery = " update " + cxiqTable;
			if (status == null || "".equalsIgnoreCase(status))
				updateQuery += " set triage_status = NULL ";
			else
				updateQuery += " set triage_status = '" + status + "' ";

			if (limit != 0)
				updateQuery += " limit " + limit;
			else
				updateQuery += " limit 20 ";
			int rs1 = stmt.executeUpdate(updateQuery);

			if (rs1 >= 0)
				result = "{\"nps\":\"true\"}";

		} catch (Exception e) {
			LOGGER.error("In updateTriageStatusForMessages method catch block due to " + e);
			result = "{\"nps\":\"false\"}";
		} finally {
			dbUtils.closeStatement(stmt);
			dbUtils.closeConnection(connection);

		}
		return result;
	}
}
