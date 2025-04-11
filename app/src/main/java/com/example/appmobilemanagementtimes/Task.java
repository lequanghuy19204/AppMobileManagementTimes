package com.example.appmobilemanagementtimes;

public class Task {
    private String title;
    private String timeRange;
    private String name;
    private String time;
    private boolean isCompleted;

    public Task(String name, String time, boolean isDone) {
        this.name = name;
        this.time = time;
        this.isCompleted = isDone;
        this.title = name;
        this.timeRange = time;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public String getTime() {
        return time;
    }

    public String getTimeRange() {
        return timeRange;
    }

    public boolean isCompleted() {
        return isCompleted;
    }
}