package com.mh.AIAssistant.service;

import com.mh.AIAssistant.configuration.TwilioConfig;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.stereotype.Service;
import org.json.JSONObject;

@Service
public class WhatsappService {

    private final TwilioConfig twilioConfig;

    public WhatsappService(TwilioConfig twilioConfig) {
        this.twilioConfig = twilioConfig;
        Twilio.init(twilioConfig.getAccountSid(), twilioConfig.getAuthToken());
    }

    // Twilio interactive messages so user can tap:
    public void sendActionButtons(String to) {
        JSONObject payload = new JSONObject()
            .put("type", "interactive")
            .put("interactive", new JSONObject()
                .put("type", "button")
                .put("body", new JSONObject().put("text", "What do you want to do?"))
                .put("action", new JSONObject()
                    .put("buttons", new org.json.JSONArray()
                        .put(new JSONObject()
                            .put("type", "reply")
                            .put("reply", new JSONObject()
                                .put("id", "store")
                                .put("title", "Store in Knowledge Base")
                            )
                        )
                        .put(new JSONObject()
                            .put("type", "reply")
                            .put("reply", new JSONObject()
                                .put("id", "chat")
                                .put("title", "Chat with AI")
                            )
                        )
                    )
                )
            );

        Message.creator(
                new PhoneNumber(to),
                new PhoneNumber(twilioConfig.getFromNumber()), // Twilio WA number
                payload.toString()
        ).create();
    }
}
