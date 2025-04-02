package com.example.appmobilemanagementtimes;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Calendar;
import java.util.List;

public class create_items extends AppCompatActivity {
    private RecyclerView recyclerToday, recyclerDone;
    private Taskadapter2 todayAdapter, doneAdapter;
    private List<Task2> todayTasks, doneTasks;
    private TextView tvDate;
    private ImageButton btnPrevDay, btnNextDay;
    private Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_items);

        ImageButton imageButton = findViewById(R.id.leftButton);

        // Xử lý sự kiện khi bấm vào ImageButton
        imageButton.setOnClickListener(v -> {
            // Tạo Intent để chuyển sang Activity khác
            Intent intent = new Intent(create_items.this, Today.class);
            startActivity(intent); // Chuyển sang SecondActivity
        });


    }}
