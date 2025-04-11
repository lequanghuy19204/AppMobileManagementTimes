package com.example.appmobilemanagementtimes;

public class Task {
    private String title;
    private String timeRange;
    private String name;
    private String time;
    private boolean isCompleted; // Thêm thuộc tính này nếu cần

    public Task(String name, String time) {
        this.name = name;
        this.time = time;
        this.isCompleted = true; // Mặc định là true khi chuyển sang Done
    }

    public Task(String title, String timeRange) {
        this.title = title;
        this.timeRange = timeRange;
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