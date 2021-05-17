package org.jroots.queueing.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Message {

    private String id;
    private String content;
    private String identifier;
    private int requeueCounter;

    private String receiptHandle;

    public Message() {

    }

    @JsonProperty
    public String getReceiptHandle() {
        return receiptHandle;
    }

    @JsonProperty
    public void setReceiptHandle(String receiptHandle) {
        this.receiptHandle = receiptHandle;
    }

    @JsonProperty
    public String getIdentifier() {
        return identifier;
    }

    @JsonProperty
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @JsonProperty
    public String getUUID() {
        return id;
    }

    @JsonProperty
    public String getContent() {
        return content;
    }

    @JsonProperty
    public void setUUID(String id) {
        this.id = id;
    }

    @JsonProperty
    public void setContent(String content) {
        this.content = content;
    }

    public String serializeToJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        String string = "{}";
        try {
            string = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return string;
    }

    public static Message deserializeFromJson(String json) {
        ObjectMapper objectMapper = new ObjectMapper();
        Message message = null;
        try {
            message = objectMapper.readValue(json, Message.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return message;
    }

    public void setRequeueCounter(int requeueCounter) {
        this.requeueCounter = requeueCounter;
    }

    public int getRequeueCounter() {
        return requeueCounter;
    }
}
