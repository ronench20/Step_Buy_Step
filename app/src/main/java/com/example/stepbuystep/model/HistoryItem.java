package com.example.stepbuystep.model;

public class HistoryItem {
    private String id;
    private String title;      // e.g., "Run", "HIIT"
    private String subtitle;   // e.g., "5.2km • 1200 steps", "Gym A"
    private String date;       // e.g., "2023-10-25"
    private long timestamp;
    private String iconType;   // "run", "walk", "gym" to decide icon

    public HistoryItem() {
        // Empty constructor for Firestore
    }

    public HistoryItem(String id, String title, String subtitle, String date, long timestamp, String iconType) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.date = date;
        this.timestamp = timestamp;
        this.iconType = iconType;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public String getDate() { return date; }
    public long getTimestamp() { return timestamp; }
    public String getIconType() { return iconType; }
}
