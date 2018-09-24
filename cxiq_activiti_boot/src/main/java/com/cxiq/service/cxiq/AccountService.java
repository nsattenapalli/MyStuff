package com.cxiq.service.cxiq;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cxiq.service.utils.Utils;

@Service
public class AccountService {

	static final Logger LOGGER = Logger.getLogger(AccountService.class.getName());

	@Autowired
	Utils utils;

	@Qualifier("cxiq")
	@Autowired
	DataSource cxiqDatasource;

	@Value("${registration.email.invalidate.days}")
	long emailInvalidateDays;

	@Value("${registration.trial.account.days}")
	long trailAccountDays;

	public String getAccounts() {
		String result = "";
		String query = " select * from  cxiq.account ";
		result = getJSONResultOnQuery(query);
		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		}
		return result;
	}

	public String getAccountDetails(String userId) {
		String result = "";
		String query = " select * from  cxiq.account where user_id= '" + userId + "' ";
		result = getJSONResultOnQuery(query);
		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		}
		return result;
	}

	public boolean deleteAccount(String userId) {
		boolean result = false;
		Connection connection = null;
		Statement stmt = null;
		String query = " delete from  cxiq.account where user_id= '" + userId + "' ";
		try {

			connection = cxiqDatasource.getConnection();
			stmt = connection.createStatement();
			int rs1 = stmt.executeUpdate(query);
			if (rs1 >= 0)
				result = true;

		} catch (Exception e) {
			LOGGER.error("In deleteAccount method catch block due to " + e);
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

	public boolean isAccountValidByURI(String userId, String tenantId, String uri) {

		boolean result = false;

		String query = " select * from  cxiq.account where user_id= '" + userId + "'";
		if (tenantId != null && !"".equalsIgnoreCase(tenantId))
			query += " or tenant_id='" + tenantId + "'";
		Connection connection = null;
		Statement stmt = null;
		try {
			if (uri.startsWith("/nps-rest"))
				connection = cxiqDatasource.getConnection();
			else
				connection = cxiqDatasource.getConnection();
			stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			if (rs.next()) {
				String createdDate = rs.getString("created_date");
				String isActive = rs.getString("is_active");
				String accountType = rs.getString("account_type");
				String status = rs.getString("status");
				String registeredDate = rs.getString("registered_date");
				if (Integer.parseInt(isActive) == 0) {
					LOGGER.error("Account is set to invalid for - '" + userId + "' in tenant - '" + tenantId + "'");
					return result;
				}
				if ("Trial".equalsIgnoreCase(accountType)) {
					if (!"Registered".equalsIgnoreCase(status)
							&& utils.getNoOfDaysFromDate(createdDate) >= emailInvalidateDays) {
						LOGGER.error("Trial account exceeded email validation days for - '" + userId + "' in tenant - '"
								+ tenantId + "'");
						return result;
					}
					if ("Registered".equalsIgnoreCase(status)
							&& utils.getNoOfDaysFromDate(registeredDate) >= trailAccountDays) {
						LOGGER.error("Trial account exceeded validation days for - '" + userId + "' in tenant - '"
								+ tenantId + "'");
						return result;
					}
				}

				result = true;
			} else
				return false;
		} catch (Exception e) {
			LOGGER.error("In createBrand method catch block due to " + e);
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

	public boolean isAccountValid(String userId, String tenantId) {
		boolean result = false;
		result = isAccountValidByURI(userId, tenantId, "/nps-rest");
		return result;

	}

	public String getCountOfAccountsByStatus() {
		String result = "";
		String query = " select status, count(*) count from cxiq.account group by status order by status asc";
		result = getJSONResultOnQuery(query);
		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		}
		return result;
	}

	public boolean createAccount(String userId, String fName, String lName, String pwd, String tenantId, String phoneNo,
			String status, String accountType) {
		boolean result = false;
		Connection connection = null;
		Statement stmt = null;
		String query = "";
		try {

			connection = cxiqDatasource.getConnection();
			stmt = connection.createStatement();
			String currentDateTime = utils.getCurrentDateTime();
			query = "insert into cxiq.account values ( " + "'" + userId + "','" + fName + "','" + lName + "','" + pwd
					+ "' ,'" + tenantId + "','" + phoneNo + "','" + status + "','" + currentDateTime + "',NULL,'"
					+ accountType + "','" + currentDateTime + "','1')";

			int rs1 = stmt.executeUpdate(query);
			if (rs1 >= 0)
				result = true;

		} catch (Exception e) {
			LOGGER.error("In createAccount method catch block due to " + e);
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

	public boolean updateAccountDetails(String userId, String tenantId, String password, String status) {

		boolean result = false;
		if ((password == null || "".equalsIgnoreCase(password)) && (status == null || "".equalsIgnoreCase(status)))
			return result;
		if ((userId == null || "".equalsIgnoreCase(userId)) && (tenantId == null || "".equalsIgnoreCase(tenantId)))
			return result;

		Connection connection = null;
		Statement stmt = null;
		String query = "";
		try {

			connection = cxiqDatasource.getConnection();
			stmt = connection.createStatement();
			String currentDateTime = utils.getCurrentDateTime();
			query = "update cxiq.account set lastmodified_date='" + currentDateTime + "'";
			if (password != null && !"".equalsIgnoreCase(password))
				query += ", pwd = '" + password + "'";
			if (status != null && !"".equalsIgnoreCase(status)) {
				if ("InActive".equalsIgnoreCase(status) || "Deleted".equalsIgnoreCase(status)
						|| "Rejected".equalsIgnoreCase(status))
					query += ", is_active = '0'";
				else
					query += ", is_active = '1'";
				if (!"InActive".equalsIgnoreCase(status) && !"Active".equalsIgnoreCase(status))
					query += ", status = '" + status + "'";

				if ("Registered".equalsIgnoreCase(status))
					query += ", registered_date= '" + currentDateTime + "'";

			}

			query += " where 1=1 ";

			if (userId != null && !"".equalsIgnoreCase(userId))
				query += " and user_id= '" + userId + "'";
			if (tenantId != null && !"".equalsIgnoreCase(tenantId))
				query += " and tenant_id= '" + tenantId + "'";

			int rs1 = stmt.executeUpdate(query);
			if (rs1 >= 0)
				result = true;

		} catch (Exception e) {
			LOGGER.error("In createAccount method catch block due to " + e);
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

	public String getTenantIdForAccount(String userId) {
		Connection connection = null;
		Statement stmt = null;
		String query = "", tenantId = "";
		try {
			connection = cxiqDatasource.getConnection();
			query = "select tenant_id from cxiq.account where user_id='" + userId + "'";
			stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			if (rs.next()) {
				tenantId = rs.getString("tenant_id");
			}
		} catch (Exception e) {
			LOGGER.error("In getTenantIdForAccount method catch block due to " + e);
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					LOGGER.error(e);
				}
			}
		}
		return tenantId;
	}

	public boolean checkIfTenantAlreadyExists(String tenantId) {
		boolean result = false;
		Connection connection = null;
		Statement stmt = null;
		String query = "";
		try {
			connection = cxiqDatasource.getConnection();
			query = "select * from cxiq.account where tenant_id='" + tenantId + "'";
			stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			if (rs.next())
				result = true;
		} catch (Exception e) {
			LOGGER.error("In checkIfTenantExists method catch block due to " + e);
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

	public boolean checkIfUserAlreadyExists(String userId) {
		boolean result = false;
		Connection connection = null;
		Statement stmt = null;
		String query = "";
		try {
			connection = cxiqDatasource.getConnection();
			query = "select * from cxiq.account where user_id='" + userId + "'";
			stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			if (rs.next())
				result = true;
		} catch (Exception e) {
			LOGGER.error("In checkIfUserAlreadyExists method catch block due to " + e);
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

	public String getJSONResultOnQuery(String query) {
		JSONArray response = new JSONArray();
		Connection connection = null;
		if (!query.isEmpty()) {
			try {
				connection = cxiqDatasource.getConnection();
				ResultSet rs = connection.createStatement().executeQuery(query);
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
				if (connection != null) {
					try {
						connection.close();
					} catch (SQLException e) {
						LOGGER.error(e);
					}
				}
			}
		}
		return response.toString();
	}

}
