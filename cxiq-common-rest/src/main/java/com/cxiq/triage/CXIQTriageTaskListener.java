package com.cxiq.triage;

import java.util.Map;

import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;

public class CXIQTriageTaskListener implements TaskListener{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public void notify(DelegateTask delegateTask) {
		delegateTask.setVariable("tenantId", delegateTask.getTenantId());
		Map<String, Object> idMap=delegateTask.getVariables();
		Map<String, Object> id=(Map<String, Object>) idMap.get("id");
		String disposition=(String) id.get("valence_direction");
		if(disposition!=null && !"".equalsIgnoreCase(disposition))
		{
			if("positive".equalsIgnoreCase(disposition))
			delegateTask.setCategory("Positive");
			if("negative".equalsIgnoreCase(disposition))
				delegateTask.setCategory("Negative");
			if("neutral".equalsIgnoreCase(disposition))
				delegateTask.setCategory("Neutral");
		}
	}

}
