package com.example.appmobilemanagementtimes;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class create_items extends AppCompatActivity {
    private String selectedStartTime;
    private String selectedEndTime;
    private String selectedRepeatMode = "never";
    private String selectedReminder = "none";
    private String selectedLabel = null; // Field for selected label
    private TextView editStartTime, editEndTime, editStartTimeHours, editEndTimeHours, repeatText, reminderText;
    private EditText editTextTaskName;
    private ImageView rightButton;
    private LinearLayout linearLayoutStartTime, linearLayoutEndTime, repeatLayout, reminderLayout;
    private ImageView[] labelIcons; // Array to hold label ImageViews
    private ActivityResultLauncher<Intent> exactAlarmPermissionLauncher;
    private String pendingTaskName, pendingStartTime, pendingReminder, pendingGroupId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_items);

        // Initialize views
        editStartTime = findViewById(R.id.editStartTime);
        editEndTime = findViewById(R.id.editEndTime);
        editStartTimeHours = findViewById(R.id.editStartTimeHours);
        editEndTimeHours = findViewById(R.id.editEndTimeHours);
        linearLayoutStartTime = findViewById(R.id.linearLayoutStartTime);
        linearLayoutEndTime = findViewById(R.id.linearLayoutEndTime);
        repeatLayout = findViewById(R.id.repeatLayout);
        repeatText = findViewById(R.id.repeatText);
        reminderLayout = findViewById(R.id.reminderLayout);
        reminderText = findViewById(R.id.reminderText);
        editTextTaskName = findViewById(R.id.editTextTaskName);
        rightButton = findViewById(R.id.rightButton);

        // Initialize label icons
        labelIcons = new ImageView[]{
                findViewById(R.id.label1),
                findViewById(R.id.label2),
                findViewById(R.id.label3),
                findViewById(R.id.label4),
                findViewById(R.id.label5),
                findViewById(R.id.label6)
        };

        // Set click listeners for label icons
        for (ImageView labelIcon : labelIcons) {
            labelIcon.setOnClickListener(v -> {
                // Reset all icons' alpha
                for (ImageView icon : labelIcons) {
                    icon.setAlpha(0.5f); // Dim unselected icons
                }
                // Highlight selected icon by setting full opacity
                v.setAlpha(1.0f); // Full opacity for selected
                selectedLabel = (String) v.getTag(); // Store the selected label tag
                Log.d("create_items", "Selected label: " + selectedLabel);
            });
        }

        // Initialize ActivityResultLauncher
        exactAlarmPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (pendingTaskName != null && pendingStartTime != null && pendingReminder != null && pendingGroupId != null) {
                        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                            scheduleReminderAfterPermission(pendingTaskName, pendingStartTime, pendingReminder, pendingGroupId);
                            Toast.makeText(this, "Báo thức đã được đặt thành công!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Quyền báo thức chính xác chưa được cấp. Sử dụng báo thức không chính xác.", Toast.LENGTH_LONG).show();
                            scheduleInexactReminder(pendingTaskName, pendingStartTime, pendingReminder, pendingGroupId);
                        }
                        pendingTaskName = null;
                        pendingStartTime = null;
                        pendingReminder = null;
                        pendingGroupId = null;
                    }
                });

        // Initialize default values
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd 'thg' M, yyyy", new Locale("vi", "VN"));
        SimpleDateFormat storageFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String displayDate = dateFormat.format(calendar.getTime());

        selectedStartTime = storageFormat.format(calendar.getTime());
        selectedEndTime = storageFormat.format(calendar.getTime());

        editStartTime.setText(displayDate.replace("Thu", "T5"));
        editEndTime.setText(displayDate.replace("Thu", "T5"));
        repeatText.setText("Không bao giờ");
        reminderText.setText("Không nhắc nhở");

        // Set click listeners
        linearLayoutStartTime.setOnClickListener(view -> showDateTimePicker(true));
        linearLayoutEndTime.setOnClickListener(view -> showDateTimePicker(false));
        repeatLayout.setOnClickListener(v -> showRepeatDialog());
        reminderLayout.setOnClickListener(v -> showReminderDialog());

        ImageButton imageButton = findViewById(R.id.leftButton);
        imageButton.setOnClickListener(v -> {
            Intent returnIntent = new Intent(create_items.this, Today.class);
            startActivity(returnIntent);
            finish();
        });

        rightButton.setOnClickListener(v -> {
            String taskName = editTextTaskName.getText().toString().trim();

            if (!taskName.isEmpty() && selectedStartTime != null && selectedEndTime != null) {
                String groupId = UUID.randomUUID().toString();
                Log.d("create_items", "Creating task - Name: " + taskName + ", StartTime: " + selectedStartTime +
                        ", EndTime: " + selectedEndTime + ", RepeatMode: " + selectedRepeatMode +
                        ", Reminder: " + selectedReminder + ", GroupId: " + groupId + ", Label: " + selectedLabel);

                Intent resultIntent = new Intent();
                resultIntent.putExtra("taskName", taskName);
                resultIntent.putExtra("startTime", selectedStartTime);
                resultIntent.putExtra("endTime", selectedEndTime);
                resultIntent.putExtra("repeatMode", selectedRepeatMode);
                resultIntent.putExtra("reminder", selectedReminder);
                resultIntent.putExtra("groupId", groupId);
                resultIntent.putExtra("label", selectedLabel); // Pass selected label
                setResult(RESULT_OK, resultIntent);

                createRecurringTasks(taskName, selectedStartTime, selectedEndTime, selectedRepeatMode, selectedReminder, groupId, selectedLabel);

                finish();
            } else {
                Toast.makeText(this, "Vui lòng nhập tên nhiệm vụ và chọn cả hai thời gian", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDateTimePicker(boolean isStartTime) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year1, month1, dayOfMonth) -> {
            calendar.set(year1, month1, dayOfMonth);

            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_12H)
                    .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                    .setMinute(calendar.get(Calendar.MINUTE))
                    .setTitleText("Chọn thời gian")
                    .build();

            timePicker.show(getSupportFragmentManager(), "TIME_PICKER");

            timePicker.addOnPositiveButtonClickListener(view1 -> {
                int hour = timePicker.getHour();
                int minute = timePicker.getMinute();

                Calendar selectedCalendar = Calendar.getInstance();
                selectedCalendar.set(year1, month1, dayOfMonth, hour, minute);

                SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd 'thg' M, yyyy", new Locale("vi", "VN"));
                SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.US);
                String displayDate = dateFormat.format(selectedCalendar.getTime());
                String displayTime = timeFormat.format(selectedCalendar.getTime())
                        .replace("AM", "SA").replace("PM", "CH");

                SimpleDateFormat storageFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                String storageTime = storageFormat.format(selectedCalendar.getTime());

                if (isStartTime) {
                    selectedStartTime = storageTime;
                    editStartTime.setText(displayDate.replace("Thu", "T5"));
                    editStartTimeHours.setText(displayTime);
                } else {
                    selectedEndTime = storageTime;
                    editEndTime.setText(displayDate.replace("Thu", "T5"));
                    editEndTimeHours.setText(displayTime);
                }
            });

        }, year, month, day);
        datePickerDialog.show();
    }

    private void showRepeatDialog() {
        String[] repeatOptions = {"Không bao giờ", "Mỗi ngày", "Mỗi tuần", "Mỗi 2 tuần", "Mỗi 3 tuần", "Mỗi tháng", "Mỗi năm"};
        String[] repeatValues = {"never", "every_day", "every_week", "every_2_weeks", "every_3_weeks", "every_month", "every_year"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn tần suất lặp lại");
        builder.setItems(repeatOptions, (dialog, which) -> {
            selectedRepeatMode = repeatValues[which];
            repeatText.setText(repeatOptions[which]);
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void showReminderDialog() {
        String[] reminderOptions = {"Không nhắc nhở", "1 phút trước", "5 phút trước", "15 phút trước", "30 phút trước", "1 giờ trước", "1 ngày trước"};
        String[] reminderValues = {"none", "1m", "5m", "15m", "30m", "1h", "1d"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn thời gian nhắc nhở");
        builder.setItems(reminderOptions, (dialog, which) -> {
            selectedReminder = reminderValues[which];
            reminderText.setText(reminderOptions[which]);
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void createRecurringTasks(String taskName, String startTime, String endTime, String repeatMode, String reminder, String groupId, String label) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        Calendar startCal = Calendar.getInstance();
        Calendar endCal = Calendar.getInstance();

        try {
            startCal.setTime(sdf.parse(startTime));
            endCal.setTime(sdf.parse(endTime));

            Task2 task = new Task2(taskName, startTime, endTime, repeatMode, groupId, reminder, label);
            addTaskToFirestore(task, "overdue");

            scheduleReminder(taskName, startTime, reminder, groupId);

            for (int i = 0; i < 365; i++) {
                switch (repeatMode) {
                    case "every_day":
                        startCal.add(Calendar.DAY_OF_MONTH, 1);
                        endCal.add(Calendar.DAY_OF_MONTH, 1);
                        break;
                    case "every_week":
                        startCal.add(Calendar.WEEK_OF_YEAR, 1);
                        endCal.add(Calendar.WEEK_OF_YEAR, 1);
                        break;
                    case "every_2_weeks":
                        startCal.add(Calendar.WEEK_OF_YEAR, 2);
                        endCal.add(Calendar.WEEK_OF_YEAR, 2);
                        break;
                    case "every_3_weeks":
                        startCal.add(Calendar.WEEK_OF_YEAR, 3);
                        endCal.add(Calendar.WEEK_OF_YEAR, 3);
                        break;
                    case "every_month":
                        startCal.add(Calendar.MONTH, 1);
                        endCal.add(Calendar.MONTH, 1);
                        break;
                    case "every_year":
                        startCal.add(Calendar.YEAR, 1);
                        endCal.add(Calendar.YEAR, 1);
                        break;
                    default:
                        return;
                }

                String newStartTime = sdf.format(startCal.getTime());
                String newEndTime = sdf.format(endCal.getTime());
                Task2 recurringTask = new Task2(taskName, newStartTime, newEndTime, repeatMode, groupId, reminder, label);
                addTaskToFirestore(recurringTask, "overdue");

                scheduleReminder(taskName, newStartTime, reminder, groupId);

                if (startCal.getTimeInMillis() > Calendar.getInstance().getTimeInMillis() + 365L * 24 * 60 * 60 * 1000) {
                    break;
                }
            }
        } catch (Exception e) {
            Log.e("create_items", "Error creating recurring tasks", e);
        }
    }

    private void scheduleReminder(String taskName, String startTime, String reminder, String groupId) {
        if (reminder.equals("none")) return;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        Calendar reminderCal = Calendar.getInstance();

        try {
            reminderCal.setTime(sdf.parse(startTime));

            switch (reminder) {
                case "1m":
                    reminderCal.add(Calendar.MINUTE, -1);
                    break;
                case "5m":
                    reminderCal.add(Calendar.MINUTE, -5);
                    break;
                case "15m":
                    reminderCal.add(Calendar.MINUTE, -15);
                    break;
                case "30m":
                    reminderCal.add(Calendar.MINUTE, -30);
                    break;
                case "1h":
                    reminderCal.add(Calendar.HOUR_OF_DAY, -1);
                    break;
                case "1d":
                    reminderCal.add(Calendar.DAY_OF_MONTH, -1);
                    break;
            }

            if (reminderCal.getTimeInMillis() > System.currentTimeMillis()) {
                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                Intent intent = new Intent(this, ReminderBroadcastReceiver.class);
                intent.putExtra("taskName", taskName);
                intent.putExtra("taskId", taskName + "_" + startTime);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        this,
                        (taskName + "_" + startTime).hashCode(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (!alarmManager.canScheduleExactAlarms()) {
                        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                        boolean hasPrompted = prefs.getBoolean("hasPromptedExactAlarm", false);
                        if (!hasPrompted) {
                            Log.w("create_items", "Cannot schedule exact alarms, prompting user for permission");
                            Toast.makeText(this, "Không thể đặt báo thức chính xác. Vui lòng cấp quyền trong cài đặt.", Toast.LENGTH_LONG).show();
                            pendingTaskName = taskName;
                            pendingStartTime = startTime;
                            pendingReminder = reminder;
                            pendingGroupId = groupId;
                            requestExactAlarmPermission();
                            prefs.edit().putBoolean("hasPromptedExactAlarm", true).apply();
                        }
                        return;
                    }
                }

                try {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderCal.getTimeInMillis(), pendingIntent);
                    Log.d("create_items", "Scheduled reminder for task: " + taskName + " at " + sdf.format(reminderCal.getTime()));
                } catch (SecurityException e) {
                    Log.e("create_items", "SecurityException when setting exact alarm", e);
                    Toast.makeText(this, "Không thể đặt báo thức chính xác do hạn chế quyền.", Toast.LENGTH_LONG).show();
                    alarmManager.set(AlarmManager.RTC_WAKEUP, reminderCal.getTimeInMillis(), pendingIntent);
                }
            }
        } catch (Exception e) {
            Log.e("create_items", "Error scheduling reminder", e);
        }
    }

    private void scheduleReminderAfterPermission(String taskName, String startTime, String reminder, String groupId) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        Calendar reminderCal = Calendar.getInstance();

        try {
            reminderCal.setTime(sdf.parse(startTime));

            switch (reminder) {
                case "1m":
                    reminderCal.add(Calendar.MINUTE, -1);
                    break;
                case "5m":
                    reminderCal.add(Calendar.MINUTE, -5);
                    break;
                case "15m":
                    reminderCal.add(Calendar.MINUTE, -15);
                    break;
                case "30m":
                    reminderCal.add(Calendar.MINUTE, -30);
                    break;
                case "1h":
                    reminderCal.add(Calendar.HOUR_OF_DAY, -1);
                    break;
                case "1d":
                    reminderCal.add(Calendar.DAY_OF_MONTH, -1);
                    break;
            }

            if (reminderCal.getTimeInMillis() > System.currentTimeMillis()) {
                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                Intent intent = new Intent(this, ReminderBroadcastReceiver.class);
                intent.putExtra("taskName", taskName);
                intent.putExtra("taskId", taskName + "_" + startTime);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        this,
                        (taskName + "_" + startTime).hashCode(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderCal.getTimeInMillis(), pendingIntent);
                Log.d("create_items", "Scheduled reminder after permission granted for task: " + taskName + " at " + sdf.format(reminderCal.getTime()));
            }
        } catch (Exception e) {
            Log.e("create_items", "Error scheduling reminder after permission", e);
        }
    }

    private void scheduleInexactReminder(String taskName, String startTime, String reminder, String groupId) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        Calendar reminderCal = Calendar.getInstance();

        try {
            reminderCal.setTime(sdf.parse(startTime));

            switch (reminder) {
                case "1m":
                    reminderCal.add(Calendar.MINUTE, -1);
                    break;
                case "5m":
                    reminderCal.add(Calendar.MINUTE, -5);
                    break;
                case "15m":
                    reminderCal.add(Calendar.MINUTE, -15);
                    break;
                case "30m":
                    reminderCal.add(Calendar.MINUTE, -30);
                    break;
                case "1h":
                    reminderCal.add(Calendar.HOUR_OF_DAY, -1);
                    break;
                case "1d":
                    reminderCal.add(Calendar.DAY_OF_MONTH, -1);
                    break;
            }

            if (reminderCal.getTimeInMillis() > System.currentTimeMillis()) {
                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                Intent intent = new Intent(this, ReminderBroadcastReceiver.class);
                intent.putExtra("taskName", taskName);
                intent.putExtra("taskId", taskName + "_" + startTime);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        this,
                        (taskName + "_" + startTime).hashCode(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                alarmManager.set(AlarmManager.RTC_WAKEUP, reminderCal.getTimeInMillis(), pendingIntent);
                Log.d("create_items", "Scheduled inexact reminder for task: " + taskName + " at " + sdf.format(reminderCal.getTime()));
            }
        } catch (Exception e) {
            Log.e("create_items", "Error scheduling inexact reminder", e);
        }
    }

    private void requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(android.net.Uri.fromParts("package", getPackageName(), null));
            exactAlarmPermissionLauncher.launch(intent);
        }
    }

    private void addTaskToFirestore(Task2 task, String status) {
        Map<String, Object> taskData = new HashMap<>();
        taskData.put("name", task.getName());
        taskData.put("startTime", task.getStartTime());
        taskData.put("endTime", task.getEndTime());
        taskData.put("status", status);
        taskData.put("repeatMode", task.getRepeatMode());
        taskData.put("groupId", task.getGroupId());
        taskData.put("reminder", task.getReminder());
        taskData.put("label", task.getLabel()); // Store label in Firestore

        String documentId = task.getName() + "_" + task.getStartTime();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("tasks")
                .document(documentId)
                .set(taskData)
                .addOnSuccessListener(aVoid -> Log.d("create_items", "Task added: " + documentId))
                .addOnFailureListener(e -> Log.e("create_items", "Error adding task", e));
    }
}