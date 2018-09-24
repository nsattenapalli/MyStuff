package com.techsophy.activitinpsreports;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;

public class TriageProcess implements JavaDelegate {
	
	public void execute(DelegateExecution execution) throws Exception {
		String tenantId=execution.getTenantId();
		execution.setVariable("tenantId", tenantId);
		String brand=(String) execution.getVariable("brand");
		Map<String, Map<String, String>> idMap = new HashMap<String, Map<String, String>>();
	    idMap=getNPSData(tenantId,brand);
	    List<Map<String, String>> idMapList=new ArrayList<Map<String, String>>(idMap.values());
	    execution.setVariable("idMap",idMapList);
	  }

	public Map<String, Map<String, String>> getNPSData(
		 String tenantId,String brand) {

		Map<String, Map<String, String>> idMap = new HashMap<String, Map<String, String>>();
		Map<String, String> variableMap = new HashMap<String, String>();
		
		variableMap.put("id", "1234");
		variableMap.put("content", "Test");
		idMap.put("1", variableMap);
		
		return idMap;
	}
}