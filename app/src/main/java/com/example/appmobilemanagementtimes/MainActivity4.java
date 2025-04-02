package com.example.appmobilemanagementtimes;

import android.os.Bundle;
import android.widget.Button;
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

public class MainActivity4 extends AppCompatActivity {
    private TextView tvToday, tvDate;
    private ImageButton btnPrevDay, btnNextDay, btnAddTask;
    private RecyclerView recyclerTodo;
    private TaskAdapter taskAdapter;
    private List<Task> taskList;
    private Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.today);

        tvToday = findViewById(R.id.tv_today);
        tvDate = findViewById(R.id.tv_date);
        btnPrevDay = findViewById(R.id.btn_prev_day);
        btnNextDay = findViewById(R.id.btn_next_day);
        btnAddTask = findViewById(R.id.btn_add);
        recyclerTodo = findViewById(R.id.recycler_todo);

        calendar = Calendar.getInstance();
        updateDate();

        btnPrevDay.setOnClickListener(v -> {
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            tvToday.setText("Pass Date");
            updateDate();
        });

        btnNextDay.setOnClickListener(v -> {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            tvToday.setText("Next day");
            updateDate();
        });

        taskList = new ArrayList<>();
        taskAdapter = new TaskAdapter(taskList, this);
        recyclerTodo.setLayoutManager(new LinearLayoutManager(this));
        recyclerTodo.setAdapter(taskAdapter);

        btnAddTask.setOnClickListener(v -> addTask());
    }

    private void updateDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        tvDate.setText(sdf.format(calendar.getTime()));
    }

    private boolean isToday() {
        Calendar today = Calendar.getInstance();
        return calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
    }

    private void addTask() {
        String todayDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(calendar.getTime());
        Task newTask = new Task("New Task", todayDate, false);
        taskList.add(newTask);
        taskAdapter.notifyItemInserted(taskList.size() - 1);
    }
}
