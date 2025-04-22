package com.example.appmobilemanagementtimes;

import static android.content.ContentValues.TAG;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class create_items extends AppCompatActivity {
    private String selectedStartTime;
    private String selectedEndTime;
    private String tempStartTime; // Lưu thời gian trước khi bật Switch
    private String tempEndTime;   // Lưu thời gian trước khi bật Switch
    private String selectedRepeatMode = "never";
    private String selectedReminder = "none";
    private String selectedLabel = null;
    private TextView editStartTime, editEndTime, editStartTimeHours, editEndTimeHours, repeatText, reminderText;
    private EditText editTextTaskName;
    private ImageView rightButton;
    private LinearLayout linearLayoutStartTime, linearLayoutEndTime, repeatLayout, reminderLayout;
    private Switch switchPin;
    private ImageView[] labelIcons;
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
        switchPin = findViewById(R.id.switch_pin);
        editTextTaskName.setFocusable(true);
        editTextTaskName.setFocusableInTouchMode(true);

        // Hiển thị bàn phím khi nhấn vào EditText
        editTextTaskName.setOnClickListener(v -> {
            editTextTaskName.requestFocus(); // Yêu cầu focus cho EditText
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(editTextTaskName, InputMethodManager.SHOW_IMPLICIT);
        });

        // Tùy chọn: Hiển thị bàn phím khi EditText nhận focus
        editTextTaskName.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(editTextTaskName, InputMethodManager.SHOW_IMPLICIT);
            }
        });
        editTextTaskName.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                // Ẩn bàn phím
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                // Bỏ focus khỏi EditText
                v.clearFocus();
                return true;
            }
            return false;
        });

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
                for (ImageView icon : labelIcons) {
                    icon.setAlpha(0.5f);
                }
                v.setAlpha(1.0f);
                selectedLabel = (String) v.getTag();
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

        // Handle Switch for all-day
        switchPin.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // Lưu thời gian hiện tại trước khi bật
                    tempStartTime = selectedStartTime;
                    tempEndTime = selectedEndTime;

                    // Set time to 00:00 - 23:59 of current day
                    Calendar startCal = Calendar.getInstance();
                    startCal.set(Calendar.HOUR_OF_DAY, 0);
                    startCal.set(Calendar.MINUTE, 0);
                    startCal.set(Calendar.SECOND, 0);
                    startCal.set(Calendar.MILLISECOND, 0);

                    Calendar endCal = (Calendar) startCal.clone();
                    endCal.set(Calendar.HOUR_OF_DAY, 23);
                    endCal.set(Calendar.MINUTE, 59);

                    selectedStartTime = storageFormat.format(startCal.getTime());
                    selectedEndTime = storageFormat.format(endCal.getTime());

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
                        // Nếu không có thời gian trước đó, giữ thời gian hiện tại
                        Calendar currentCal = Calendar.getInstance();
                        selectedStartTime = storageFormat.format(currentCal.getTime());
                        selectedEndTime = storageFormat.format(currentCal.getTime());
                        String displayDate = dateFormat.format(currentCal.getTime());
                        String displayTime = new SimpleDateFormat("hh:mm a", Locale.US)
                                .format(currentCal.getTime())
                                .replace("AM", "SA").replace("PM", "CH");
                        editStartTime.setText(displayDate.replace("Thu", "T5"));
                        editStartTimeHours.setText(displayTime);
                        editEndTime.setText(displayDate.replace("Thu", "T5"));
                        editEndTimeHours.setText(displayTime);
                    }

                    // Enable both time selections
                    linearLayoutStartTime.setEnabled(true);
                    linearLayoutEndTime.setEnabled(true);
                }
            }
        });

        // Set click listeners
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
                if (!isValidTimeRange(selectedStartTime, selectedEndTime)) {
                    Toast.makeText(this, "Thời gian kết thúc phải sau thời gian bắt đầu", Toast.LENGTH_SHORT).show();
                    return;
                }
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
                resultIntent.putExtra("label", selectedLabel);
                setResult(RESULT_OK, resultIntent);

                createRecurringTasks(taskName, selectedStartTime, selectedEndTime, selectedRepeatMode, selectedReminder, groupId, selectedLabel);

                finish();
            } else {
                Toast.makeText(this, "Vui lòng nhập tên nhiệm vụ và chọn cả hai thời gian", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void showDateTimePicker(boolean isStartTime) {
        // Kiểm tra trạng thái Activity để tránh hiển thị dialog khi không hợp lệ
        if (isFinishing() || isDestroyed()) {
            Log.w("create_items", "Activity is finishing or destroyed, cannot show DateTimePicker");
            return;
        }

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

            try {
                timePicker.show(getSupportFragmentManager(), "TIME_PICKER");
            } catch (IllegalStateException e) {
                Log.e("create_items", "Error showing MaterialTimePicker", e);
                Toast.makeText(this, "Không thể hiển thị bộ chọn thời gian, vui lòng thử lại", Toast.LENGTH_SHORT).show();
                return;
            }

            timePicker.addOnPositiveButtonClickListener(view1 -> {
                try {
                    int hour = timePicker.getHour();
                    int minute = timePicker.getMinute();

                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(year1, month1, dayOfMonth, hour, minute, 0);
                    selectedCalendar.set(Calendar.SECOND, 0);
                    selectedCalendar.set(Calendar.MILLISECOND, 0);

                    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd 'thg' M, yyyy", new Locale("vi", "VN"));
                    SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.US);
                    SimpleDateFormat storageFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

                    String displayDate = dateFormat.format(selectedCalendar.getTime()).replace("Thu", "T5");
                    String displayTime = timeFormat.format(selectedCalendar.getTime())
                            .replace("AM", "SA").replace("PM", "CH");
                    String storageTime = storageFormat.format(selectedCalendar.getTime());

                    if (isStartTime) {
                        selectedStartTime = storageTime;
                        editStartTime.setText(displayDate);
                        editStartTimeHours.setText(displayTime);
                    } else {
                        selectedEndTime = storageTime;
                        editEndTime.setText(displayDate);
                        editEndTimeHours.setText(displayTime);
                    }

                    Log.d("create_items", (isStartTime ? "StartTime" : "EndTime") + " updated: " + storageTime);
                } catch (Exception e) {
                    Log.e("create_items", "Error processing time picker selection", e);
                    Toast.makeText(this, "Lỗi khi chọn thời gian, vui lòng thử lại", Toast.LENGTH_SHORT).show();
                }
            });

            timePicker.addOnNegativeButtonClickListener(view1 -> {
                Log.d("create_items", "Time picker cancelled");
                // Không làm gì, giữ Activity hiện tại
            });

            timePicker.addOnCancelListener(dialog -> {
                Log.d("create_items", "Time picker dismissed");
                // Không làm gì, giữ Activity hiện tại
            });

        }, year, month, day);

        try {
            datePickerDialog.show();
        } catch (IllegalStateException e) {
            Log.e("create_items", "Error showing DatePickerDialog", e);
            Toast.makeText(this, "Không thể hiển thị bộ chọn ngày, vui lòng thử lại", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDatePickerOnly(boolean isStartTime) {
        Calendar calendar = Calendar.getInstance();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            if (selectedStartTime != null) {
                calendar.setTime(sdf.parse(selectedStartTime));
            }
        } catch (ParseException e) {
            Log.e("create_items", "Error parsing selectedStartTime: " + selectedStartTime, e);
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
            Log.e("create_items", "Error parsing time: " + time, e);
            Toast.makeText(this, "Lỗi hiển thị thời gian", Toast.LENGTH_SHORT).show();
        }
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
        taskData.put("label", task.getLabel());

        String documentId = task.getName() + "_" + task.getStartTime();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("tasks")
                .document(documentId)
                .set(taskData)
                .addOnSuccessListener(aVoid -> Log.d("create_items", "Task added: " + documentId))
                .addOnFailureListener(e -> Log.e("create_items", "Error adding task", e));
    }

    private boolean isValidTimeRange(String startTime, String endTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            // Chuyển chuỗi thời gian thành đối tượng Date
            Date startDate = sdf.parse(startTime);
            Date endDate = sdf.parse(endTime);

            // Tính khoảng cách thời gian (mili giây)
            long timeDifferenceMillis = endDate.getTime() - startDate.getTime();
            // Chuyển sang giây
            long timeDifferenceSeconds = timeDifferenceMillis / 1000;

            // Kiểm tra nếu thời gian kết thúc sau thời gian bắt đầu (khoảng cách > 0)
            return timeDifferenceSeconds > 0;
        } catch (ParseException e) {
            Log.e(TAG, "Lỗi khi phân tích thời gian: " + e.getMessage());
            Toast.makeText(this, "Lỗi định dạng thời gian", Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}
