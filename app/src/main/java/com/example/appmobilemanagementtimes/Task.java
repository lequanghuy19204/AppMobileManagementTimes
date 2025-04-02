package com.example.appmobilemanagementtimes;

public class Task {
    private String name;
    private String date;
    private boolean isCompleted;

    public Task(String name, String date, boolean isCompleted) {
        this.name = name;
        this.date = date;
        this.isCompleted = isCompleted;
    }

    public String getName() {
        return name;
    }

    public String getDate() {
        return date;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }
}
