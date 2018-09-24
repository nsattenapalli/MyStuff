package com.techsophy.registration.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.Base64;
import java.util.Iterator;
import java.util.Properties;

import org.codehaus.jettison.json.JSONObject;

import com.techsophy.registration.controller.UserRegistrationImpl;

public class Utils {

	static Base64.Encoder encoder = Base64.getEncoder();

	public static String getPropertyValue(String key) {
		String content = "";
		try {
			String filePath = UserRegistrationImpl.class.getClassLoader().getResource("").getPath()
					+ "//mail.properties";
			File file = new File(filePath);
			FileInputStream fileInput;

			fileInput = new FileInputStream(file);
			Properties properties = new Properties();
			properties.load(fileInput);
			content = properties.get(key) != null ? properties.get(key).toString() : "";

		} catch (Exception e) {
			e.printStackTrace();
		}
		return content;
	}

	public static String prepareVerificationMailBody(String mailBody, String productName, JSONObject obj) {
		try {
			mailBody = mailBody.replace("$product", productName);
			Iterator<?> itr = obj.keys();
			while (itr.hasNext()) {
				String key = (String) itr.next();
				String value = (String) obj.get(key);
				mailBody = mailBody.replace(key, key);
				mailBody = mailBody.replace("$" + key.substring(0, 1).toLowerCase() + key.substring(1, key.length()),
						value);
			}
			obj.put("token", System.currentTimeMillis());
			String contextURL = obj.getString("contextURL").toString();
			JSONObject obj1 = new JSONObject();
			obj1.put("key", new String(encoder.encodeToString(obj.toString().getBytes())));
			String link = contextURL + "/?identifier=" + obj1.toString();
			mailBody = mailBody.replace("$link", link);
		} catch (Exception e) {

		}
		return mailBody;
	}

	public static String prepareApprovalMailBody(String mailBody, String productName, JSONObject obj) {
		try {
			mailBody = mailBody.replace("$product", productName);
			Iterator<?> itr = obj.keys();
			while (itr.hasNext()) {
				String key = (String) itr.next();
				String value = (String) obj.get(key);
				mailBody = mailBody.replace(key, key);
				mailBody = mailBody.replace("$" + key.substring(0, 1).toLowerCase() + key.substring(1, key.length()),
						value);
			}
			obj.put("token", System.currentTimeMillis());
			JSONObject obj1 = new JSONObject();
			obj1.put("key", new String(encoder.encodeToString(obj.toString().getBytes())));
			obj1.put("status", "approved");
			String contextURL = obj.getString("contextURL").toString();
			String approvalLink = contextURL + "?input=" + obj1;
			obj1.put("status", "rejected");
			String rejectionLink = contextURL + "?input=" + obj1;
			int buttonCount = Integer.parseInt(Utils.getPropertyValue("buttons").toString());
			for (int i = 1; i <= buttonCount; i++) {
				if (Utils.getPropertyValue("button" + i).equalsIgnoreCase("approve")) {
					String approvalText = getApprovalText(i, approvalLink);
					mailBody = mailBody.replace("$button" + i, approvalText);
				} else if (Utils.getPropertyValue("button" + i).equalsIgnoreCase("reject")) {
					String rejectText = getRejectText(i, rejectionLink);
					mailBody = mailBody.replace("$button" + i, rejectText);
				}
			}
		} catch (Exception e) {

		}
		return mailBody;
	}

	private static String getApprovalText(int i, String approvalLink) {
		return "<a href='" + approvalLink
				+ "' target=\"_blank\" name=\"approve\" style=\"background-color: rgb(92, 184, 92);font-family: sans-serif; color: rgb(255, 255, 255); font-size: 15px; line-height: 1.5; padding: 5px 10px; box-shadow: 0px -2px 0px rgba(0, 0, 0, 0.05) inset; background-image: none; white-space: initial; border: 1px solid; border-radius: 3px;text-decoration:none;\">"
				+ Utils.getPropertyValue("button" + i + "displaytext") + "</a>";
	}

	private static String getRejectText(int i, String rejectLink) {
		return "<a href='" + rejectLink
				+ "' target=\"_blank\" style=\"background-color: #d9534f; color: rgb(255, 255, 255); font-size: 15px;font-family: sans-serif; line-height: 1.5; padding: 4px 10px; box-shadow: 0px -2px 0px rgba(0, 0, 0, 0.05) inset; background-image: none; white-space: initial; border: 1px solid #d43f3a; border-radius: 3px;text-decoration:none\" name=\"reject\">&nbsp;&nbsp;"
				+ Utils.getPropertyValue("button" + i + "displaytext") + "&nbsp;&nbsp;</a>";
	}

	public static String getMailBodyContent(String fileName) {
		String content = "";
		String filePath = UserRegistrationImpl.class.getClassLoader().getResource("").getPath() + fileName;
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(filePath));
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				sb.append(line);
				sb.append("\n");
				line = br.readLine();
			}
			br.close();
			content = sb.toString();
		} catch (Exception e) {
		}

		return content;
	}
}
