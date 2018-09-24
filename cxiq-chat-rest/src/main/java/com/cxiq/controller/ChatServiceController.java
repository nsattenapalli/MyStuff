package com.cxiq.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
public class ChatServiceController {

    @MessageMapping("/chat.sendMessage")
    @SendTo("/chat/recieve")
    public String sendMessage(@Payload String chatMessage) {
    	System.out.println(chatMessage);
        return chatMessage;
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/chat/recieve")
    public String addUser(@Payload String user, 
                               SimpMessageHeaderAccessor headerAccessor) {
        headerAccessor.getSessionAttributes().put("username", user);
        return user;
    }
    
    @MessageMapping("/chat.addGroup")
    @SendTo("/chat/recieveGroup")
    public String addGroup(@Payload String user, 
                               SimpMessageHeaderAccessor headerAccessor) {
        headerAccessor.getSessionAttributes().put("username", user);
        return user;
    }
    
    @MessageMapping("/chat.sendComment")
    @SendTo("/chat/recieveComment")
    public String sendComment(@Payload String comment) {
    	System.out.println(comment);
        return comment;
    }
    
    @MessageMapping("/chat.sendGroupchat")
    @SendTo("/chat/recieveGroupchat")
    public String sendGroupChat(@Payload String grpChat) {
    	System.out.println(grpChat);
        return grpChat;
    }

}
