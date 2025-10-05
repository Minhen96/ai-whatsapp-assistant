package com.mh.AIAssistant.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = getUserIdFromSession(session);
        sessions.put(session.getId(), session);
        
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                    .add(session.getId());
        
        System.out.println("WebSocket connection established for user: " + userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String userId = getUserIdFromSession(session);
        System.out.println("Received message from user " + userId + ": " + message.getPayload());
        
        // Echo the message back to the client
        session.sendMessage(new TextMessage("Echo: " + message.getPayload()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = getUserIdFromSession(session);
        sessions.remove(session.getId());
        
        Set<String> userSessionIds = userSessions.get(userId);
        if (userSessionIds != null) {
            userSessionIds.remove(session.getId());
            if (userSessionIds.isEmpty()) {
                userSessions.remove(userId);
            }
        }
        
        System.out.println("WebSocket connection closed for user: " + userId);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("WebSocket transport error: " + exception.getMessage());
        exception.printStackTrace();
    }

    public void sendMessageToUser(String userId, String message) {
        Set<String> sessionIds = userSessions.get(userId);
        if (sessionIds != null) {
            for (String sessionId : sessionIds) {
                WebSocketSession session = sessions.get(sessionId);
                if (session != null && session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(message));
                    } catch (IOException e) {
                        System.err.println("Error sending message to user " + userId + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void broadcastMessage(String message) {
        for (WebSocketSession session : sessions.values()) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    System.err.println("Error broadcasting message: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    private String getUserIdFromSession(WebSocketSession session) {
        // Extract user ID from session attributes or query parameters
        String userId = (String) session.getAttributes().get("userId");
        if (userId == null) {
            // Try to get from query parameters
            String query = session.getUri().getQuery();
            if (query != null && query.contains("userId=")) {
                userId = query.substring(query.indexOf("userId=") + 7);
                if (userId.contains("&")) {
                    userId = userId.substring(0, userId.indexOf("&"));
                }
            }
        }
        return userId != null ? userId : "anonymous";
    }

    public Map<String, Set<String>> getUserSessions() {
        return Collections.unmodifiableMap(userSessions);
    }

    public int getActiveSessionCount() {
        return sessions.size();
    }
}
