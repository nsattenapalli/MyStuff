package com.cxiq.controller;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cxiq.service.cxiq.ChatMessageService;

@RestController
@RequestMapping(value = "/service")
public class ChatController {
	static final Logger LOGGER = Logger.getLogger(ChatController.class.getName());

	@Autowired
	ChatMessageService chatMessages;

	@PreAuthorize("hasAnyAuthority('user', 'manager','admin')")
	@RequestMapping(value = "/tasks/{taskId}/chat/messages", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public String updateChatMessage(@PathVariable("taskId") String taskId, String fullMsg) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Update chat message : taskId=" + taskId);
		String result = "";
		result = chatMessages.updateChatMessage(taskId, fullMsg);
		return result;
	}

	@PreAuthorize("hasAnyAuthority('user', 'manager','admin')")
	@RequestMapping(value = "/tasks/{taskId}/chat/messages", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getChatMessages(@PathVariable("taskId") String taskId, @RequestParam("fromUserId") String fromUserId,
			@RequestParam("toUserId") String toUserId, @RequestParam("groupName") String groupName) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Get chat messages : taskId=" + taskId + ",fromUserId=" + fromUserId + ",toUserId=" + toUserId
					+ ",groupName=" + groupName);
		String result = "";
		result = chatMessages.getChatMessages(taskId, fromUserId, toUserId, groupName);
		return result;
	}

	@PreAuthorize("hasAnyAuthority('user', 'manager','admin')")
	@RequestMapping(value = "/tasks/{taskId}/chat/groups", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getChatGroupsForTask(@PathVariable("taskId") String taskId) {
		if (LOGGER.isDebugEnabled())
			LOGGER.debug(" Get chat messages : taskId=" + taskId);
		String result = "";
		result = chatMessages.getChatGroupsForTask(taskId);
		return result;
	}

}
