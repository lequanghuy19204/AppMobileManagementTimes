package com.example.appmobilemanagementtimes;

public class Task2 {
    private String name;
    private String startTime;
    private String endTime;

    public Task2(String name, String startTime, String endTime) {
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getName() {
        return name;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }
}