package com.example.appmobilemanagementtimes;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity2 extends AppCompatActivity {
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


    }}
