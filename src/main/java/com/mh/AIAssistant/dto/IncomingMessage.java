package com.mh.AIAssistant.dto;

public class IncomingMessage {
    private String from;
    private String body;

    // getters and setters
    public String getFrom() {
        return from;
    }
    public void setFrom(String from) {
        this.from = from;
    }

    public String getBody() {
        return body;
    }
    public void setBody(String body) {
        this.body = body;
    }
}
