package com.example.appmobilemanagementtimes;

public class Task2 {
    private String name;
    private String startTime;
    private String endTime;
    private String repeatMode;
    private String groupId;
    private String reminder;
    private String label; // New field for label

    public Task2(String name, String startTime, String endTime, String repeatMode, String groupId) {
        this(name, startTime, endTime, repeatMode, groupId, "none", null);
    }

    public Task2(String name, String startTime, String endTime, String repeatMode, String groupId, String reminder) {
        this(name, startTime, endTime, repeatMode, groupId, reminder, null);
    }

    public Task2(String name, String startTime, String endTime, String repeatMode, String groupId, String reminder, String label) {
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.repeatMode = repeatMode != null ? repeatMode : "never";
        this.groupId = groupId;
        this.reminder = reminder != null ? reminder : "none";
        this.label = label; // Initialize label
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

    public String getRepeatMode() {
        return repeatMode;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getReminder() {
        return reminder;
    }

    public String getLabel() {
        return label;
    }
}