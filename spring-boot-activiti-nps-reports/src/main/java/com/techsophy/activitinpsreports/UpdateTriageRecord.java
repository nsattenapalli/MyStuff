package com.techsophy.activitinpsreports;

import java.util.Map;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.delegate.JavaDelegate;
import org.apache.log4j.Logger;

public class UpdateTriageRecord implements JavaDelegate {

	static final Logger LOGGER = Logger.getLogger(UpdateTriageRecord.class.getName());

	private Expression statusVal;

	public void execute(DelegateExecution execution) throws Exception {
		@SuppressWarnings("unchecked")
		Map<String, String> idMap = (Map<String, String>) execution.getVariable("id");

		String tenantId = execution.getTenantId();
		execution.setVariable("tenantId", tenantId);
		String id = idMap.get("id");
		String status = (String) statusVal.getValue(execution);
		String brand = (String) execution.getVariable("brand");
		if (brand == null || "".equalsIgnoreCase(brand))
			brand = "decooda";
		boolean result = updateRecord(tenantId, brand, id, status);
		
		if("Processed".equalsIgnoreCase(status))
	    {
		    idMap.put("groupId", "test_group");
		    idMap.put("groupName", "test_group_name");
	    }

		execution.setVariable("result", result);
	}


	public boolean updateRecord(String tenantId, String brandName, String id, String status) {
		System.out.println("Updated record with id " + id + " and status " + status);
		return true;
	}
}