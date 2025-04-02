package com.example.appmobilemanagementtimes;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity3 extends AppCompatActivity {
    private RecyclerView recyclerToday, recyclerDone;
    private Taskadapter4 todayAdapter;
    private Taskadapter3 doneAdapter;
    private List<Task2> todayTasks, doneTasks;
    private TextView tvDate;
    private ImageButton btnPrevDay, btnNextDay;
    private Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.passdate);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                // Xử lý khi chọn Home
                return true;
            } else if (itemId == R.id.navigation_upcoming) {
                // Xử lý khi chọn Upcoming
                return true;
            } else if (itemId == R.id.navigation_pomo) {
                // Xử lý khi chọn Pomo
                return true;
            } else if (itemId == R.id.navigation_statistic) {
                // Xử lý khi chọn Statistic
                return true;
            }
            return false;
        });

        recyclerToday = findViewById(R.id.recycler_todo);
        recyclerDone = findViewById(R.id.recyclerView);
        tvDate = findViewById(R.id.tv_date);
        btnPrevDay = findViewById(R.id.btn_prev_day);
        btnNextDay = findViewById(R.id.btn_next_day);

        // Cấu hình RecyclerView
        recyclerToday.setLayoutManager(new LinearLayoutManager(this));
        recyclerDone.setLayoutManager(new LinearLayoutManager(this));

        // Khởi tạo danh sách công việc
        todayTasks = new ArrayList<>();
        doneTasks = new ArrayList<>();

        // Thêm công việc Today
        todayTasks.add(new Task2("Làm bài tập", "08:00 AM"));
        todayTasks.add(new Task2("Họp nhóm", "10:30 AM"));
        todayTasks.add(new Task2("Tập thể dục", "06:00 PM"));

        // Thêm công việc Done
        doneTasks.add(new Task2("Đi chợ", "07:00 AM"));
        doneTasks.add(new Task2("Đọc sách", "09:00 PM"));

        // Cấu hình Adapter
        todayAdapter = new Taskadapter4(todayTasks);
        doneAdapter = new Taskadapter3(doneTasks);
        recyclerToday.setAdapter(todayAdapter);
        recyclerDone.setAdapter(doneAdapter);

        // Hiển thị ngày hiện tại
        calendar = Calendar.getInstance();
        updateDate();

        // Xử lý chuyển ngày
        btnPrevDay.setOnClickListener(v -> {
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            updateDate();
        });

        btnNextDay.setOnClickListener(v -> {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            updateDate();
        });
    }

    private void updateDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        tvDate.setText(sdf.format(calendar.getTime()));
    }
    public static class SpaceItemDecoration extends RecyclerView.ItemDecoration {
        private final int space;

        public SpaceItemDecoration(int space) {
            this.space = space;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            outRect.bottom = space;
        }

    }}

