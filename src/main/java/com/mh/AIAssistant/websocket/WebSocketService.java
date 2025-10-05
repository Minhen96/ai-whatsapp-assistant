package com.mh.AIAssistant.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;
import java.util.HashMap;

@Service
public class WebSocketService {

    @Autowired
    private ChatWebSocketHandler webSocketHandler;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void notifyWhatsAppMessage(String from, String message, String response) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "whatsapp_message");
            notification.put("from", from);
            notification.put("message", message);
            notification.put("response", response);
            notification.put("timestamp", System.currentTimeMillis());

            String jsonMessage = objectMapper.writeValueAsString(notification);
            
            // Broadcast to all connected frontend clients
            webSocketHandler.broadcastMessage(jsonMessage);
            
            System.out.println("Notified frontend clients about WhatsApp message from " + from);
        } catch (Exception e) {
            System.err.println("Error sending WebSocket notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void notifyFrontendMessage(String userId, String message, String response) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "frontend_message");
            notification.put("userId", userId);
            notification.put("message", message);
            notification.put("response", response);
            notification.put("timestamp", System.currentTimeMillis());

            String jsonMessage = objectMapper.writeValueAsString(notification);
            
            // Send to specific user's frontend sessions
            webSocketHandler.sendMessageToUser(userId, jsonMessage);
            
            System.out.println("Notified user " + userId + " about frontend message");
        } catch (Exception e) {
            System.err.println("Error sending WebSocket notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void notifySystemMessage(String message) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "system_message");
            notification.put("message", message);
            notification.put("timestamp", System.currentTimeMillis());

            String jsonMessage = objectMapper.writeValueAsString(notification);
            
            // Broadcast to all connected clients
            webSocketHandler.broadcastMessage(jsonMessage);
            
            System.out.println("Notified all clients about system message: " + message);
        } catch (Exception e) {
            System.err.println("Error sending system notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Map<String, Object> getConnectionStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeSessions", webSocketHandler.getActiveSessionCount());
        stats.put("userSessions", webSocketHandler.getUserSessions());
        return stats;
    }
}
