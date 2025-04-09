package com.example.appmobilemanagementtimes;

public class Task {
    private String name;
    private String time;
    private boolean isCompleted; // Thêm thuộc tính này nếu cần

    public Task(String name, String time) {
        this.name = name;
        this.time = time;
        this.isCompleted = true; // Mặc định là true khi chuyển sang Done
    }

    public String getName() {
        return name;
    }

    public String getTime() {
        return time;
    }

    public boolean isCompleted() {
        return isCompleted;
    }
}