package com.example.appmobilemanagementtimes;

public class Task {
    private String title;
    private String timeRange;

    public Task(String title, String timeRange) {
        this.title = title;
        this.timeRange = timeRange;
    }

    public String getTitle() {
        return title;
    }

    public String getTimeRange() {
        return timeRange;
    }
} 