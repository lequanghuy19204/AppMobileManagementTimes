package com.example.appmobilemanagementtimes;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class update_items extends AppCompatActivity {
    private String selectedStartTime;
    private String selectedEndTime;
    private String tempStartTime; // Lưu thời gian trước khi bật Switch
    private String tempEndTime;   // Lưu thời gian trước khi bật Switch
    private String selectedRepeatMode = "never";
    private String selectedReminder = "none";
    private String selectedLabel = null;
    private String groupId;
    private String userId;
    private String status;
    private TextView editStartTime, editEndTime, editStartTimeHours, editEndTimeHours, tvRepeatMode, tvReminderTime;
    private EditText editTextTaskName;
    private ImageView rightButton;
    private LinearLayout linearLayoutStartTime, linearLayoutEndTime, linearLayoutRepeatMode, linearLayoutReminderTime;
    private Switch switchPin;
    private ImageView[] labelIcons;
    private String originalTaskId;
    private ActivityResultLauncher<Intent> exactAlarmPermissionLauncher;
    private String pendingTaskName, pendingStartTime, pendingReminder, pendingGroupId;
    private static final String TAG = "update_items";

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.update_items);

        // Initialize views
        editStartTime = findViewById(R.id.editStartTime);
        editEndTime = findViewById(R.id.editEndTime);
        editStartTimeHours = findViewById(R.id.editStartTimeHours);
        editEndTimeHours = findViewById(R.id.editEndTimeHours);
        linearLayoutStartTime = findViewById(R.id.linearLayoutStartTime);
        linearLayoutEndTime = findViewById(R.id.linearLayoutEndTime);
        linearLayoutRepeatMode = findViewById(R.id.linearLayoutRepeatMode);
        linearLayoutReminderTime = findViewById(R.id.linearLayoutReminderTime);
        tvRepeatMode = findViewById(R.id.tvRepeatMode);
        tvReminderTime = findViewById(R.id.tvReminderTime);
        editTextTaskName = findViewById(R.id.editTextTaskName);
        rightButton = findViewById(R.id.rightButton);
        switchPin = findViewById(R.id.switch_pin2);

        // Initialize label icons
        labelIcons = new ImageView[]{
                findViewById(R.id.label1),
                findViewById(R.id.label2),
                findViewById(R.id.label3),
                findViewById(R.id.label4),
                findViewById(R.id.label5),
                findViewById(R.id.label6)
        };

        // Initialize ActivityResultLauncher for exact alarm permission
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

        // Receive data from intent
        Intent intent = getIntent();
        String taskName = intent.getStringExtra("taskName");
        String startTime = intent.getStringExtra("startTime");
        String endTime = intent.getStringExtra("endTime");
        String repeatMode = intent.getStringExtra("repeatMode");
        String reminder = intent.getStringExtra("reminder");
        String label = intent.getStringExtra("label");
        groupId = intent.getStringExtra("groupId");
        originalTaskId = intent.getStringExtra("originalTaskId");
        userId = intent.getStringExtra("userId");
        status = intent.getStringExtra("status");

        Log.d(TAG, "Received data - taskName: " + taskName + ", startTime: " + startTime +
                ", endTime: " + endTime + ", repeatMode: " + repeatMode +
                ", reminder: " + reminder + ", label: " + label +
                ", groupId: " + groupId + ", originalTaskId: " + originalTaskId +
                ", userId: " + userId + ", status: " + status);

        // Populate fields with received data
        if (taskName != null) {
            editTextTaskName.setText(taskName);
        }
        if (startTime != null) {
            selectedStartTime = startTime;
            displayTime(startTime, true);
        }
        if (endTime != null) {
            selectedEndTime = endTime;
            displayTime(endTime, false);
        }
        if (repeatMode != null) {
            selectedRepeatMode = repeatMode;
            tvRepeatMode.setText(getRepeatModeDisplayText(repeatMode));
        }
        if (reminder != null) {
            selectedReminder = reminder;
            tvReminderTime.setText(getReminderDisplayText(reminder));
        }
        if (label != null) {
            selectedLabel = label;
            for (ImageView labelIcon : labelIcons) {
                if (label.equals(labelIcon.getTag())) {
                    labelIcon.setAlpha(1.0f);
                } else {
                    labelIcon.setAlpha(0.5f);
                }
            }
        }

        // Handle Switch for all-day
        switchPin.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd 'thg' M, yyyy", new Locale("vi", "VN"));
                Calendar startCal = Calendar.getInstance();

                if (isChecked) {
                    // Lưu thời gian hiện tại trước khi bật
                    tempStartTime = selectedStartTime;
                    tempEndTime = selectedEndTime;

                    // Use the date from selectedStartTime if available, else current date
                    try {
                        if (selectedStartTime != null) {
                            startCal.setTime(sdf.parse(selectedStartTime));
                        }
                    } catch (ParseException e) {
                        Log.e(TAG, "Error parsing selectedStartTime: " + selectedStartTime, e);
                    }

                    // Set time to 00:00 - 23:59
                    startCal.set(Calendar.HOUR_OF_DAY, 0);
                    startCal.set(Calendar.MINUTE, 0);
                    startCal.set(Calendar.SECOND, 0);
                    startCal.set(Calendar.MILLISECOND, 0);

                    Calendar endCal = (Calendar) startCal.clone();
                    endCal.set(Calendar.HOUR_OF_DAY, 23);
                    endCal.set(Calendar.MINUTE, 59);

                    selectedStartTime = sdf.format(startCal.getTime());
                    selectedEndTime = sdf.format(endCal.getTime());

                    String displayDate = dateFormat.format(startCal.getTime());
                    String startDisplayTime = "12:00 SA";
                    String endDisplayTime = "11:59 CH";

                    editStartTime.setText(displayDate.replace("Thu", "T5"));
                    editStartTimeHours.setText(startDisplayTime);
                    editEndTime.setText(displayDate.replace("Thu", "T5"));
                    editEndTimeHours.setText(endDisplayTime);

                    // Enable start time date selection, disable end time
                    linearLayoutStartTime.setEnabled(true);
                    linearLayoutEndTime.setEnabled(false);
                } else {
                    // Khôi phục thời gian trước khi bật (nếu có)
                    if (tempStartTime != null && tempEndTime != null) {
                        selectedStartTime = tempStartTime;
                        selectedEndTime = tempEndTime;
                        displayTime(selectedStartTime, true);
                        displayTime(selectedEndTime, false);
                    } else {
                        // Nếu không có thời gian trước đó, giữ thời gian từ Intent
                        if (selectedStartTime != null && selectedEndTime != null) {
                            displayTime(selectedStartTime, true);
                            displayTime(selectedEndTime, false);
                        } else {
                            // Nếu không có dữ liệu, dùng thời gian hiện tại
                            Calendar currentCal = Calendar.getInstance();
                            selectedStartTime = sdf.format(currentCal.getTime());
                            selectedEndTime = sdf.format(currentCal.getTime());
                            String displayDate = dateFormat.format(currentCal.getTime());
                            String displayTime = new SimpleDateFormat("hh:mm a", Locale.US)
                                    .format(currentCal.getTime())
                                    .replace("AM", "SA").replace("PM", "CH");
                            editStartTime.setText(displayDate.replace("Thu", "T5"));
                            editStartTimeHours.setText(displayTime);
                            editEndTime.setText(displayDate.replace("Thu", "T5"));
                            editEndTimeHours.setText(displayTime);
                        }
                    }

                    // Enable both time selections
                    linearLayoutStartTime.setEnabled(true);
                    linearLayoutEndTime.setEnabled(true);
                }
            }
        });

        // Set click listeners for other UI elements
        linearLayoutStartTime.setOnClickListener(view -> {
            if (switchPin.isChecked()) {
                // Chỉ chọn ngày khi Switch bật
                showDatePickerOnly(true);
            } else {
                // Chọn cả ngày và giờ khi Switch tắt
                showDateTimePicker(true);
            }
        });
        linearLayoutEndTime.setOnClickListener(view -> {
            if (!switchPin.isChecked()) {
                // Chỉ cho phép chọn khi Switch tắt
                showDateTimePicker(false);
            }
        });
        linearLayoutRepeatMode.setOnClickListener(v -> showRepeatModeDialog());
        linearLayoutReminderTime.setOnClickListener(v -> showReminderDialog());

        // Set click listeners for label icons
        for (ImageView labelIcon : labelIcons) {
            labelIcon.setOnClickListener(v -> {
                for (ImageView icon : labelIcons) {
                    icon.setAlpha(0.5f);
                }
                v.setAlpha(1.0f);
                selectedLabel = (String) v.getTag();
                Log.d(TAG, "Selected label: " + selectedLabel);
            });
        }

        ImageButton imageButton = findViewById(R.id.leftButton);
        imageButton.setOnClickListener(v -> {
            Intent returnIntent = new Intent(update_items.this, Today.class);
            startActivity(returnIntent);
            finish();
        });

        rightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String taskName = editTextTaskName.getText().toString().trim();

                if (taskName.isEmpty()) {
                    Toast.makeText(update_items.this, "Vui lòng nhập tên nhiệm vụ", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (selectedStartTime == null || selectedEndTime == null) {
                    Toast.makeText(update_items.this, "Vui lòng chọn thời gian", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!isValidTimeRange(selectedStartTime, selectedEndTime)) {
                    Toast.makeText(update_items.this, "Thời gian kết thúc phải sau thời gian bắt đầu", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Cập nhật task vào Firestore
                updateTaskInFirestore(taskName, selectedStartTime, selectedEndTime, selectedRepeatMode, selectedReminder, groupId, selectedLabel);
            }
        });

        // Nếu không có userId từ intent, lấy từ SharedPreferences
        if (userId == null) {
            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            userId = prefs.getString("userId", null);
        }
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

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("taskName", editTextTaskName.getText().toString());
        outState.putString("selectedStartTime", selectedStartTime);
        outState.putString("selectedEndTime", selectedEndTime);
        outState.putString("tempStartTime", tempStartTime);
        outState.putString("tempEndTime", tempEndTime);
        outState.putString("selectedRepeatMode", selectedRepeatMode);
        outState.putString("selectedReminder", selectedReminder);
        outState.putString("selectedLabel", selectedLabel);
        outState.putBoolean("isAllDay", switchPin.isChecked());
    }

    private void showDatePickerOnly(boolean isStartTime) {
        Calendar calendar = Calendar.getInstance();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            if (selectedStartTime != null) {
                calendar.setTime(sdf.parse(selectedStartTime));
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing selectedStartTime: " + selectedStartTime, e);
        }

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year1, month1, dayOfMonth) -> {
            Calendar startCal = Calendar.getInstance();
            startCal.set(year1, month1, dayOfMonth, 0, 0, 0);
            startCal.set(Calendar.MILLISECOND, 0);

            Calendar endCal = (Calendar) startCal.clone();
            endCal.set(Calendar.HOUR_OF_DAY, 23);
            endCal.set(Calendar.MINUTE, 59);

            SimpleDateFormat storageFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd 'thg' M, yyyy", new Locale("vi", "VN"));

            selectedStartTime = storageFormat.format(startCal.getTime());
            selectedEndTime = storageFormat.format(endCal.getTime());

            String displayDate = dateFormat.format(startCal.getTime());
            String startDisplayTime = "12:00 SA";
            String endDisplayTime = "11:59 CH";

            editStartTime.setText(displayDate.replace("Thu", "T5"));
            editStartTimeHours.setText(startDisplayTime);
            editEndTime.setText(displayDate.replace("Thu", "T5"));
            editEndTimeHours.setText(endDisplayTime);

        }, year, month, day);
        datePickerDialog.show();
    }

    private void showRepeatModeDialog() {
        String[] repeatOptions = {"Không bao giờ", "Mỗi ngày", "Mỗi tuần", "Mỗi 2 tuần", "Mỗi 3 tuần", "Mỗi tháng", "Mỗi năm"};
        String[] repeatValues = {"never", "every_day", "every_week", "every_2_weeks", "every_3_weeks", "every_month", "every_year"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn chế độ lặp lại");
        builder.setItems(repeatOptions, (dialog, which) -> {
            selectedRepeatMode = repeatValues[which];
            tvRepeatMode.setText(repeatOptions[which]);
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
            tvReminderTime.setText(reminderOptions[which]);
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private String getRepeatModeDisplayText(String repeatMode) {
        switch (repeatMode) {
            case "every_day":
                return "Mỗi ngày";
            case "every_week":
                return "Mỗi tuần";
            case "every_2_weeks":
                return "Mỗi 2 tuần";
            case "every_3_weeks":
                return "Mỗi 3 tuần";
            case "every_month":
                return "Mỗi tháng";
            case "every_year":
                return "Mỗi năm";
            case "never":
            default:
                return "Không bao giờ";
        }
    }

    private String getReminderDisplayText(String reminder) {
        switch (reminder) {
            case "1m":
                return "1 phút trước";
            case "5m":
                return "5 phút trước";
            case "15m":
                return "15 phút trước";
            case "30m":
                return "30 phút trước";
            case "1h":
                return "1 giờ trước";
            case "1d":
                return "1 ngày trước";
            case "none":
            default:
                return "Không nhắc nhở";
        }
    }

    private void displayTime(String time, boolean isStartTime) {
        try {
            SimpleDateFormat storageFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(storageFormat.parse(time));

            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd 'thg' M, yyyy", new Locale("vi", "VN"));
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.US);
            String displayDate = dateFormat.format(calendar.getTime());
            String displayTime = timeFormat.format(calendar.getTime())
                    .replace("AM", "SA").replace("PM", "CH");

            if (isStartTime) {
                editStartTime.setText(displayDate.replace("Thu", "T5"));
                editStartTimeHours.setText(displayTime);
            } else {
                editEndTime.setText(displayDate.replace("Thu", "T5"));
                editEndTimeHours.setText(displayTime);
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing time: " + time, e);
            Toast.makeText(this, "Lỗi hiển thị thời gian", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isValidTimeRange(String startTime, String endTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Date startDate = sdf.parse(startTime);
            Date endDate = sdf.parse(endTime);

            long timeDifferenceMillis = endDate.getTime() - startDate.getTime();
            long timeDifferenceSeconds = timeDifferenceMillis / 1000;

            return timeDifferenceSeconds > 0;
        } catch (ParseException e) {
            Log.e(TAG, "Lỗi khi phân tích thời gian: " + e.getMessage());
            Toast.makeText(this, "Lỗi định dạng thời gian", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void updateTaskInFirestore(String name, String startTime, String endTime, String repeatMode, String reminder, String groupId, String label) {
        if (groupId == null || groupId.isEmpty()) {
            Log.e(TAG, "groupId is null or empty, generating new groupId");
            groupId = UUID.randomUUID().toString();
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Bước 1: Xóa tất cả task có cùng groupId
        String finalGroupId = groupId;
        db.collection("tasks")
                .whereEqualTo("groupId", groupId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "Tìm thấy " + querySnapshot.size() + " task với groupId: " + finalGroupId);
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        db.collection("tasks").document(doc.getId())
                                .delete()
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Đã xóa task: " + doc.getId()))
                                .addOnFailureListener(e -> Log.e(TAG, "Lỗi xóa task: " + doc.getId(), e));
                    }

                    // Bước 2: Tạo task chính với dữ liệu cập nhật
                    Map<String, Object> taskData = new HashMap<>();
                    taskData.put("name", name);
                    taskData.put("startTime", startTime);
                    taskData.put("endTime", endTime);
                    taskData.put("repeatMode", repeatMode);
                    taskData.put("reminder", reminder);
                    taskData.put("groupId", finalGroupId);
                    taskData.put("label", label);
                    taskData.put("userId", userId);
                    taskData.put("status", status);

                    String documentId = name + "_" + startTime;
                    Log.d(TAG, "Tạo task mới với documentId: " + documentId + ", groupId: " + finalGroupId);
                    db.collection("tasks").document(documentId)
                            .set(taskData)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Tạo task thành công: " + documentId);
                                Toast.makeText(update_items.this, "Cập nhật nhiệm vụ thành công", Toast.LENGTH_SHORT).show();

                                // Bước 3: Lập lịch nhắc nhở cho nhiệm vụ chính
                                scheduleReminder(name, startTime, reminder, finalGroupId);

                                // Bước 4: Tạo các task lặp lại nếu cần
                                if (!repeatMode.equals("never")) {
                                    createRecurringTasks(name, startTime, endTime, repeatMode, reminder, finalGroupId, label);
                                }

                                // Bước 5: Trả dữ liệu về activity Today
                                Intent resultIntent = new Intent();
                                resultIntent.putExtra("taskName", name);
                                resultIntent.putExtra("startTime", startTime);
                                resultIntent.putExtra("endTime", endTime);
                                resultIntent.putExtra("repeatMode", repeatMode);
                                resultIntent.putExtra("reminder", reminder);
                                resultIntent.putExtra("groupId", finalGroupId);
                                resultIntent.putExtra("label", label);
                                resultIntent.putExtra("originalTaskId", originalTaskId);
                                resultIntent.putExtra("newGroupId", finalGroupId);
                                setResult(RESULT_OK, resultIntent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Lỗi tạo task: " + documentId, e);
                                Toast.makeText(update_items.this, "Lỗi khi cập nhật nhiệm vụ", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi truy vấn task với groupId: " + finalGroupId, e);
                    Toast.makeText(update_items.this, "Lỗi khi truy vấn nhiệm vụ", Toast.LENGTH_SHORT).show();
                });
    }

    private void createRecurringTasks(String taskName, String startTime, String endTime, String repeatMode, String reminder, String groupId, String label) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        Calendar startCal = Calendar.getInstance();
        Calendar endCal = Calendar.getInstance();

        try {
            startCal.setTime(sdf.parse(startTime));
            endCal.setTime(sdf.parse(endTime));

            for (int i = 0; i < 7; i++) {
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
                Task2 recurringTask = new Task2(taskName, newStartTime, newEndTime, repeatMode, groupId, reminder, label, userId);
                addTaskToFirestore(recurringTask, "overdue");

                // Lập lịch nhắc nhở cho nhiệm vụ lặp lại
                scheduleReminder(taskName, newStartTime, reminder, groupId);

                if (startCal.getTimeInMillis() > Calendar.getInstance().getTimeInMillis() + 365L * 24 * 60 * 60 * 1000) {
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating recurring tasks", e);
        }
    }

    private void addTaskToFirestore(Task2 task, String status) {
        task.setUserId(userId);

        Map<String, Object> taskData = new HashMap<>();
        taskData.put("name", task.getName());
        taskData.put("startTime", task.getStartTime());
        taskData.put("endTime", task.getEndTime());
        taskData.put("status", status);
        taskData.put("repeatMode", task.getRepeatMode());
        taskData.put("groupId", task.getGroupId());
        taskData.put("reminder", task.getReminder());
        taskData.put("label", task.getLabel());
        taskData.put("userId", task.getUserId());

        String documentId = task.getName() + "_" + task.getStartTime();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("tasks")
                .document(documentId)
                .set(taskData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Task added: " + documentId))
                .addOnFailureListener(e -> Log.e(TAG, "Error adding task", e));
    }

    private void updateSelectedLabelUI() {
        for (ImageView icon : labelIcons) {
            icon.setBackground(null);
        }

        if (selectedLabel != null && !selectedLabel.isEmpty()) {
            int index = -1;
            switch (selectedLabel) {
                case "label1":
                    index = 0;
                    break;
                case "label2":
                    index = 1;
                    break;
                case "label3":
                    index = 2;
                    break;
                case "label4":
                    index = 3;
                    break;
                case "label5":
                    index = 4;
                    break;
                case "label6":
                    index = 5;
                    break;
            }

            if (index >= 0 && index < labelIcons.length) {
                labelIcons[index].setBackground(getResources().getDrawable(R.drawable.round_bg_selected));
            }
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
                            Log.w(TAG, "Cannot schedule exact alarms, prompting user for permission");
                            Toast.makeText(this, "Vui lòng cấp quyền báo thức chính xác để nhận nhắc nhở đúng giờ.", Toast.LENGTH_LONG).show();
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
                    Log.d(TAG, "Scheduled reminder for task: " + taskName + " at " + sdf.format(reminderCal.getTime()));
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException when setting exact alarm", e);
                    alarmManager.set(AlarmManager.RTC_WAKEUP, reminderCal.getTimeInMillis(), pendingIntent);
                }
            } else {
                Log.d(TAG, "Reminder is in the past, not scheduling: " + taskName + " at " + sdf.format(reminderCal.getTime()));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling reminder", e);
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
                Log.d(TAG, "Scheduled reminder after permission granted for task: " + taskName + " at " + sdf.format(reminderCal.getTime()));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling reminder after permission", e);
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
                Log.d(TAG, "Scheduled inexact reminder for task: " + taskName + " at " + sdf.format(reminderCal.getTime()));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling inexact reminder", e);
        }
    }

    private void requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(android.net.Uri.fromParts("package", getPackageName(), null));
            exactAlarmPermissionLauncher.launch(intent);
        }
    }
}