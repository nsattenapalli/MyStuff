package com.cxiq;

import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;


public class CXIQTaskListener implements TaskListener{

	private static final long serialVersionUID = 1L;

	@Override
	public void notify(DelegateTask delegateTask) {
		delegateTask.setVariable("CXIQ_TASKID", delegateTask.getId());
		}
}
