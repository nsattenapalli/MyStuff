package com.techsophy.activitinpsreports;

import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;


public class NPSTaskListener implements TaskListener{

	private static final long serialVersionUID = 1L;

	@Override
	public void notify(DelegateTask delegateTask) {
		delegateTask.setVariable("NPS_TASKID", delegateTask.getId());
		}
}
