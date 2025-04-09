package com.example.appmobilemanagementtimes;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class update_items extends AppCompatActivity {
    private String selectedStartTime;
    private String selectedEndTime;
    private TextView editStartTime, editEndTime, editStartTimeHours, editEndTimeHours;
    private EditText editTextTaskName;
    private ImageView rightButton;
    private LinearLayout linearLayoutStartTime, linearLayoutEndTime;
    private String originalTaskId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.update_items);

        editStartTime = findViewById(R.id.editStartTime);
        editEndTime = findViewById(R.id.editEndTime);
        editStartTimeHours = findViewById(R.id.editStartTimeHours);
        editEndTimeHours = findViewById(R.id.editEndTimeHours);
        linearLayoutStartTime = findViewById(R.id.linearLayoutStartTime);
        linearLayoutEndTime = findViewById(R.id.linearLayoutEndTime);
        editTextTaskName = findViewById(R.id.editTextTaskName);
        rightButton = findViewById(R.id.rightButton);

        Intent intent = getIntent();
        String taskName = intent.getStringExtra("taskName");
        String startTime = intent.getStringExtra("startTime");
        String endTime = intent.getStringExtra("endTime");
        originalTaskId = intent.getStringExtra("originalTaskId");

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

        linearLayoutStartTime.setOnClickListener(view -> showDateTimePicker(true));
        linearLayoutEndTime.setOnClickListener(view -> showDateTimePicker(false));

        ImageButton imageButton = findViewById(R.id.leftButton);
        imageButton.setOnClickListener(v -> {
            Intent returnIntent = new Intent(update_items.this, Today.class);
            startActivity(returnIntent);
            finish();
        });

        rightButton.setOnClickListener(v -> {
            String taskNameUpdated = editTextTaskName.getText().toString().trim();

            if (!taskNameUpdated.isEmpty() && selectedStartTime != null && selectedEndTime != null) {
                Log.d("update_items", "Updating task - Name: " + taskNameUpdated + ", StartTime: " + selectedStartTime + ", EndTime: " + selectedEndTime + ", OriginalTaskId: " + originalTaskId);
                Intent resultIntent = new Intent();
                resultIntent.putExtra("taskName", taskNameUpdated);
                resultIntent.putExtra("startTime", selectedStartTime);
                resultIntent.putExtra("endTime", selectedEndTime);
                resultIntent.putExtra("originalTaskId", originalTaskId);
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
            e.printStackTrace();
            Toast.makeText(this, "Lỗi hiển thị thời gian", Toast.LENGTH_SHORT).show();
        }
    }
}