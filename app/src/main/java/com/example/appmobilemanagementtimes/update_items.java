package com.example.appmobilemanagementtimes;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
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
    private TextView editStartTime, editEndTime, editStartTimeHours, editEndTimeHours, tvRepeatMode, tvReminderTime;
    private EditText editTextTaskName;
    private ImageView rightButton;
    private LinearLayout linearLayoutStartTime, linearLayoutEndTime, linearLayoutRepeatMode, linearLayoutReminderTime;
    private Switch switchPin;
    private ImageView[] labelIcons;
    private String originalTaskId;
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

        Log.d(TAG, "Received data - taskName: " + taskName + ", startTime: " + startTime +
                ", endTime: " + endTime + ", repeatMode: " + repeatMode +
                ", reminder: " + reminder + ", label: " + label +
                ", groupId: " + groupId + ", originalTaskId: " + originalTaskId);

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

        rightButton.setOnClickListener(v -> {
            String taskNameUpdated = editTextTaskName.getText().toString().trim();

            if (!taskNameUpdated.isEmpty() && selectedStartTime != null && selectedEndTime != null) {
                if (!isValidTimeRange(selectedStartTime, selectedEndTime)) {
                    Toast.makeText(this, "Thời gian kết thúc phải sau thời gian bắt đầu", Toast.LENGTH_SHORT).show();
                    return;
                }

                String newGroupId = selectedRepeatMode.equals("never") ? null : UUID.randomUUID().toString();

                Log.d(TAG, "Updating task - Name: " + taskNameUpdated + ", StartTime: " + selectedStartTime +
                        ", EndTime: " + selectedEndTime + ", RepeatMode: " + selectedRepeatMode +
                        ", Reminder: " + selectedReminder + ", GroupId: " + newGroupId +
                        ", OriginalTaskId: " + originalTaskId + ", Label: " + selectedLabel);
                Intent resultIntent = new Intent();
                resultIntent.putExtra("taskName", taskNameUpdated);
                resultIntent.putExtra("startTime", selectedStartTime);
                resultIntent.putExtra("endTime", selectedEndTime);
                resultIntent.putExtra("repeatMode", selectedRepeatMode);
                resultIntent.putExtra("reminder", selectedReminder);
                resultIntent.putExtra("groupId", groupId);
                resultIntent.putExtra("newGroupId", newGroupId);
                resultIntent.putExtra("originalTaskId", originalTaskId);
                resultIntent.putExtra("label", selectedLabel);
                setResult(RESULT_OK, resultIntent);
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
            Calendar startCal = Calendar.getInstance();
            Calendar endCal = Calendar.getInstance();
            startCal.setTime(sdf.parse(startTime));
            endCal.setTime(sdf.parse(endTime));
            return endCal.after(startCal);
        } catch (ParseException e) {
            Log.e(TAG, "Error validating time range", e);
            return false;
        }
    }
}