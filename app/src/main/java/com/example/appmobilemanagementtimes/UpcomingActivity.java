package com.example.appmobilemanagementtimes;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class UpcomingActivity extends AppCompatActivity {
    private RecyclerView taskRecyclerView;
    private RecyclerView calendarDaysRecyclerView;
    private TaskAdapter taskAdapter;
    private CalendarDayAdapter calendarDayAdapter;
    private TextView dateHeader;
    private ImageButton prevButton;
    private ImageButton nextButton;
    private Calendar currentCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upcoming);

        // Khởi tạo các view
        dateHeader = findViewById(R.id.dateHeader);
        prevButton = findViewById(R.id.prevButton);
        nextButton = findViewById(R.id.nextButton);
        calendarDaysRecyclerView = findViewById(R.id.calendarDaysRecyclerView);
        taskRecyclerView = findViewById(R.id.taskRecyclerView);

        // Thiết lập RecyclerView cho tasks
        taskRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Tạo dữ liệu mẫu cho tasks
        List<Task> taskList = new ArrayList<>();
        taskList.add(new Task("Breathtaking", "20:15 - 22:15", false));
        taskList.add(new Task("Breathtaking", "20:15 - 22:15", false));
        taskList.add(new Task("Breathtaking", "20:15 - 22:15", false));

        // Thiết lập adapter cho tasks
        taskAdapter = new TaskAdapter(taskList);
        taskRecyclerView.setAdapter(taskAdapter);

        // Khởi tạo calendar
        currentCalendar = Calendar.getInstance();
        updateCalendarView();

        // Thiết lập sự kiện cho nút prev và next
        prevButton.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            updateCalendarView();
        });

        nextButton.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            updateCalendarView();
        });

        // Thiết lập bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.navigation_upcoming);
        
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                startActivity(new Intent(UpcomingActivity.this, Today.class));
                finish();
                return true;
            } else if (itemId == R.id.navigation_upcoming) {
                return true;
            } else if (itemId == R.id.navigation_pomo) {
                // TODO: Implement Pomo screen
                return true;
            } else if (itemId == R.id.navigation_statistic) {
                startActivity(new Intent(UpcomingActivity.this, StatisticActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    private void updateCalendarView() {
        // Cập nhật tiêu đề tháng
        SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM yyyy", Locale.getDefault());
        dateHeader.setText(dateFormat.format(currentCalendar.getTime()));

        // Tạo danh sách ngày trong tháng
        List<Integer> days = new ArrayList<>();
        
        // Lưu ngày hiện tại
        int currentDay = currentCalendar.get(Calendar.DAY_OF_MONTH);
        
        // Đặt calendar về ngày đầu tiên của tháng
        Calendar tempCalendar = (Calendar) currentCalendar.clone();
        tempCalendar.set(Calendar.DAY_OF_MONTH, 1);
        
        // Lấy thứ của ngày đầu tiên (0 = Chủ nhật, 1 = Thứ 2, ...)
        int firstDayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK);
        
        // Điều chỉnh để thứ 2 là ngày đầu tuần (2 = Thứ 2, 3 = Thứ 3, ..., 1 = Chủ nhật)
        if (firstDayOfWeek == Calendar.SUNDAY) {
            firstDayOfWeek = 7;
        } else {
            firstDayOfWeek = firstDayOfWeek - 1;
        }
        
        // Thêm các ô trống cho các ngày trước ngày đầu tiên của tháng
        for (int i = 1; i < firstDayOfWeek; i++) {
            days.add(0);
        }
        
        // Thêm các ngày trong tháng
        int daysInMonth = tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int i = 1; i <= daysInMonth; i++) {
            days.add(i);
        }
        
        // Thêm các ô trống để hoàn thành tuần cuối cùng
        int remainingCells = 42 - days.size(); // 6 hàng x 7 cột = 42 ô
        for (int i = 0; i < remainingCells; i++) {
            days.add(0);
        }
        
        // Thiết lập adapter cho calendar
        calendarDayAdapter = new CalendarDayAdapter(this, days, 
                currentCalendar.get(Calendar.MONTH), 
                currentCalendar.get(Calendar.YEAR));
        calendarDayAdapter.setSelectedDay(currentDay);
        calendarDayAdapter.setOnDayClickListener(day -> {
            // Xử lý khi người dùng chọn một ngày
            currentCalendar.set(Calendar.DAY_OF_MONTH, day);
            SimpleDateFormat newDateFormat = new SimpleDateFormat("d MMMM yyyy", Locale.getDefault());
            dateHeader.setText(newDateFormat.format(currentCalendar.getTime()));
            
            // Ở đây bạn có thể cập nhật danh sách task dựa trên ngày được chọn
        });
        
        calendarDaysRecyclerView.setLayoutManager(new GridLayoutManager(this, 7));
        calendarDaysRecyclerView.setAdapter(calendarDayAdapter);
    }
} 