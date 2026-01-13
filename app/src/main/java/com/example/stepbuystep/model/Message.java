package com.example.stepbuystep.model;

import com.google.firebase.Timestamp;

public class Message {
    private String messageId;
    private String coachId;
    private String coachName;
    private String traineeId;
    private String messageText;
    private Timestamp timestamp;
    private boolean isRead;

    // Empty constructor required for Firestore
    public Message() {
    }

    public Message(String messageId, String coachId, String coachName, String traineeId,
                   String messageText, Timestamp timestamp, boolean isRead) {
        this.messageId = messageId;
        this.coachId = coachId;
        this.coachName = coachName;
        this.traineeId = traineeId;
        this.messageText = messageText;
        this.timestamp = timestamp;
        this.isRead = isRead;
    }

    // Getters
    public String getMessageId() {
        return messageId;
    }

    public String getCoachId() {
        return coachId;
    }

    public String getCoachName() {
        return coachName;
    }

    public String getTraineeId() {
        return traineeId;
    }

    public String getMessageText() {
        return messageText;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    // Setters
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public void setCoachId(String coachId) {
        this.coachId = coachId;
    }

    public void setCoachName(String coachName) {
        this.coachName = coachName;
    }

    public void setTraineeId(String traineeId) {
        this.traineeId = traineeId;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}