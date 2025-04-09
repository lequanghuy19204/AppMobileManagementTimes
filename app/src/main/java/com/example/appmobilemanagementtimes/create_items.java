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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class create_items extends AppCompatActivity {
    private String selectedStartTime;
    private String selectedEndTime;
    private TextView editStartTime, editEndTime, editStartTimeHours, editEndTimeHours;
    private EditText editTextTaskName;
    private ImageView rightButton;
    private LinearLayout linearLayoutStartTime, linearLayoutEndTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_items);

        editStartTime = findViewById(R.id.editStartTime);
        editEndTime = findViewById(R.id.editEndTime);
        editStartTimeHours = findViewById(R.id.editStartTimeHours);
        editEndTimeHours = findViewById(R.id.editEndTimeHours);
        linearLayoutStartTime = findViewById(R.id.linearLayoutStartTime);
        linearLayoutEndTime = findViewById(R.id.linearLayoutEndTime);
        editTextTaskName = findViewById(R.id.editTextTaskName);
        rightButton = findViewById(R.id.rightButton);

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd 'thg' M, yyyy", new Locale("vi", "VN"));
        SimpleDateFormat storageFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String displayDate = dateFormat.format(calendar.getTime());

        // Gán giá trị mặc định cho selectedStartTime và selectedEndTime
        selectedStartTime = storageFormat.format(calendar.getTime());
        selectedEndTime = storageFormat.format(calendar.getTime());

        editStartTime.setText(displayDate.replace("Thu", "T5"));
        editEndTime.setText(displayDate.replace("Thu", "T5"));

        linearLayoutStartTime.setOnClickListener(view -> showDateTimePicker(true));
        linearLayoutEndTime.setOnClickListener(view -> showDateTimePicker(false));

        ImageButton imageButton = findViewById(R.id.leftButton);
        imageButton.setOnClickListener(v -> {
            Intent returnIntent = new Intent(create_items.this, Today.class);
            startActivity(returnIntent);
            finish();
        });

        rightButton.setOnClickListener(v -> {
            String taskName = editTextTaskName.getText().toString().trim();

            if (!taskName.isEmpty() && selectedStartTime != null && selectedEndTime != null) {
                Log.d("create_items", "Creating task - Name: " + taskName + ", StartTime: " + selectedStartTime + ", EndTime: " + selectedEndTime);
                Intent resultIntent = new Intent();
                resultIntent.putExtra("taskName", taskName);
                resultIntent.putExtra("startTime", selectedStartTime);
                resultIntent.putExtra("endTime", selectedEndTime);
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
}