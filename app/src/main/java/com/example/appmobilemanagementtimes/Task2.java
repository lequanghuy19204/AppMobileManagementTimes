package com.example.appmobilemanagementtimes;

public class Task2 {
    private String name;
    private String startTime;
    private String endTime;
    private String repeatMode;
    private String groupId;
    private String reminder;
    private String label;
    private String userId; // Thêm trường userId

    public Task2(String name, String startTime, String endTime, String repeatMode, String groupId) {
        this(name, startTime, endTime, repeatMode, groupId, "none", null, null);
    }

    public Task2(String name, String startTime, String endTime, String repeatMode, String groupId, String reminder) {
        this(name, startTime, endTime, repeatMode, groupId, reminder, null, null);
    }

    public Task2(String name, String startTime, String endTime, String repeatMode, String groupId, String reminder, String label) {
        this(name, startTime, endTime, repeatMode, groupId, reminder, label, null);
    }

    public Task2(String name, String startTime, String endTime, String repeatMode, String groupId, String reminder, String label, String userId) {
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.repeatMode = repeatMode != null ? repeatMode : "never";
        this.groupId = groupId;
        this.reminder = reminder != null ? reminder : "none";
        this.label = label;
        this.userId = userId;
    }

    public String getName() { return name; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getRepeatMode() { return repeatMode; }
    public String getGroupId() { return groupId; }
    public String getReminder() { return reminder; }
    public String getLabel() { return label; }
    public String getUserId() { return userId; } // Thêm getter cho userId

    public void setUserId(String userId) { this.userId = userId; } // Thêm setter nếu cần
}