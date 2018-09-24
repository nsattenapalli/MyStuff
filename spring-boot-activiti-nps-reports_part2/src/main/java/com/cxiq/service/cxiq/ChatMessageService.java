package com.cxiq.service.cxiq;

import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.UUID;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.cxiq.service.utils.DBUtils;
import com.cxiq.service.utils.Utils;

@Service
public class ChatMessageService {

	@Qualifier("activiti")
	@Autowired
	DataSource activitiDatasource;

	@Autowired
	Utils utils;

	@Autowired
	DBUtils dbUtils;

	static final Logger LOGGER = Logger.getLogger(ChatMessageService.class.getName());

	public String updateChatMessage(String taskId, String fullMsg) {

		String result = "{\"nps\":\"false\"}";
		Connection connection = null;
		PreparedStatement stmt = null;
		Statement stmt1 = null;
		String query = "", query1 = "", filterQuery = "";
		try {
			JSONObject obj = new JSONObject(fullMsg);
			String groupName = obj.get("groupName").toString();
			String fromUserId = obj.get("fromUserId").toString();
			String toUserId = obj.get("toUserId").toString();
			String chatWindow1 = "", chatWindow2 = "";
			if (groupName != null && !"".equalsIgnoreCase(groupName)) {
				filterQuery = filterQuery + " and MESSAGE_='" + groupName + "'";
			} else if (fromUserId != null && !"".equalsIgnoreCase(fromUserId) && toUserId != null
					&& !"".equalsIgnoreCase(toUserId)) {
				chatWindow1 = fromUserId + "|" + toUserId;
				chatWindow2 = toUserId + "|" + fromUserId;
				filterQuery = filterQuery + " and (MESSAGE_='" + chatWindow1 + "' or MESSAGE_='" + chatWindow2 + "')";
			} else
				return result;
			query1 = "select * from ACT_HI_COMMENT where TASK_ID_='" + taskId + "' and TYPE_='Chat' ";
			if (filterQuery != null && !"".equalsIgnoreCase(filterQuery))
				query1 = query1 + filterQuery;

			connection = activitiDatasource.getConnection();
			stmt1 = connection.createStatement();
			ResultSet rs = stmt1.executeQuery(query1);

			if (rs.next()) {
				query = "update  ACT_HI_COMMENT  set FULL_MSG_=? " + " ,TIME_=?  where TASK_ID_=? and TYPE_='Chat' ";

				if (filterQuery != null && !"".equalsIgnoreCase(filterQuery))
					query = query + filterQuery;
				stmt = connection.prepareStatement(query);

				byte[] byteContent = fullMsg.getBytes();
				Blob blob = connection.createBlob();
				blob.setBytes(1, byteContent);
				// Calendar cal = Calendar.getInstance();
				stmt.setBlob(1, blob);
				long timeNow = Calendar.getInstance().getTimeInMillis();
				java.sql.Timestamp ts = new java.sql.Timestamp(timeNow);
				stmt.setTimestamp(2, ts);
				// stmt.setDate(2, new Date());
				stmt.setString(3, taskId);
			} else {
				query = "insert into  ACT_HI_COMMENT  values(?,?,?,?,?,?,?,?,?)";
				stmt = connection.prepareStatement(query);
				UUID uuid = UUID.randomUUID();
				String randomUUIDString = uuid.toString();

				stmt.setString(1, randomUUIDString);
				stmt.setString(2, "Chat"); // TYPE as chat
				// Calendar cal = Calendar.getInstance();
				long timeNow = Calendar.getInstance().getTimeInMillis();
				java.sql.Timestamp ts = new java.sql.Timestamp(timeNow);
				stmt.setTimestamp(3, ts);

				stmt.setString(4, fromUserId);
				stmt.setString(5, taskId);
				stmt.setString(6, null);

				// Store Group (or) Individual Chat Messages
				if (groupName != null && !"".equalsIgnoreCase(groupName)) {
					stmt.setString(7, "GroupChat");
					stmt.setString(8, groupName);
				} else {
					stmt.setString(7, "Chat");
					stmt.setString(8, chatWindow1);

				}
				byte[] byteContent = fullMsg.getBytes();
				Blob blob = connection.createBlob();
				blob.setBytes(1, byteContent);

				stmt.setBlob(9, blob);
			}
			int rs1 = stmt.executeUpdate();
			if (rs1 >= 0)
				result = "{\"nps\":\"true\"}";
		} catch (Exception e) {
			LOGGER.error("In updateChatMessage method catch block due to " + e);
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

	public String getChatMessages(String taskId, String fromUserId, String toUserId, String groupName) {
		String result = "{\"nps\":\"false\"}";
		String query = "", filterQuery = "";
		if (groupName != null && !"".equalsIgnoreCase(groupName)) {
			filterQuery = filterQuery + " and MESSAGE_ = '" + groupName + "'";
		} else if (fromUserId != null && !"".equalsIgnoreCase(fromUserId) && toUserId != null
				&& !"".equalsIgnoreCase(toUserId)) {
			String chatWindow1 = fromUserId + "|" + toUserId;
			String chatWindow2 = toUserId + "|" + fromUserId;
			filterQuery = filterQuery + " and ACTION_='Chat' and ( MESSAGE_='" + chatWindow1 + "' or MESSAGE_='"
					+ chatWindow2 + "')";
		} else {
			return result;
		}
		query = "select * from ACT_HI_COMMENT where TASK_ID_='" + taskId + "' and TYPE_='Chat' ";
		if (filterQuery != null && !"".equalsIgnoreCase(filterQuery))
			query = query + filterQuery;

		Connection connection = null;
		Statement stmt = null;
		try {
			connection = activitiDatasource.getConnection();
			stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			if (rs.next()) {
				Blob fullMsg = rs.getBlob("FULL_MSG_");
				JSONObject obj = new JSONObject();
				String fullMessage = new String(fullMsg.getBytes(1l, (int) fullMsg.length()));
				obj.put("message", fullMessage);
				result = obj.toString();
			}
		} catch (Exception e) {
			LOGGER.error("In getChatMessages method catch block due to " + e);
			return result;
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					LOGGER.error(e);
				}
			}
		}

		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		}
		return result;
	}

	public String getChatGroupsForTask(String taskId) {
		String result = null;
		String query = " SELECT MESSAGE_ as GROUP_NAME,TIME_ as CREATED_DATE,USER_ID_ as CREATED_BY from  ACT_HI_COMMENT where TYPE_='Chat' and ACTION_='GroupChat' ";
		if (taskId != null && !"".equalsIgnoreCase(taskId))
			query = query + " and TASK_ID_='" + taskId + "'";
		query = query + " order by TIME_ desc";

		result = dbUtils.getJSONResultOnQuery(query, "ACTIVITI");

		if ((result == null) || (result.trim().length() == 0)) {
			result = "{\"nps\":0}";
		}
		return result;
	}

}
