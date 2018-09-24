package com.techsophy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.webflow.executor.FlowExecutor;
import org.springframework.webflow.mvc.servlet.FlowController;

@Controller
public class DaveFlowController extends FlowController {

	@Autowired
	public DaveFlowController(FlowExecutor flowExecutor) {
		this.setFlowExecutor(flowExecutor);
	}

	public boolean someDecision(DaveDto daveDto) {
		System.out.println("test");
		return true;
	}

}
