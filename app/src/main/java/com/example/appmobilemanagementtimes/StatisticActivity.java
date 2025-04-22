package com.example.appmobilemanagementtimes;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class StatisticActivity extends AppCompatActivity {
    private LineChart lineChart; // Thay LineChart thành LineChart
    private FirebaseFirestore db;
    private ImageButton btnPreDay, btnNextDay;
    private TextView tvDate, tvToday;
    private Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.statistic);

        // Khởi tạo Firebase
        db = FirebaseFirestore.getInstance();

        // Khởi tạo giao diện
        tvToday = findViewById(R.id.tv_today);
        tvDate = findViewById(R.id.tv_date);
        btnPreDay = findViewById(R.id.btn_prev_day);
        btnNextDay = findViewById(R.id.btn_next_day);
        lineChart = findViewById(R.id.linechart); // ID vẫn là lineChart, nhưng kiểu là LineChart

        // Khởi tạo calendar
        calendar = Calendar.getInstance();
        if (getIntent().hasExtra("selectedDate")) {
            calendar.setTimeInMillis(getIntent().getLongExtra("selectedDate", calendar.getTimeInMillis()));
        }
        updateDateAndUI(false); // Gọi với tham số false khi khởi tạo

        // Xử lý nút chuyển ngày
        btnNextDay.setOnClickListener(v -> {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            updateDateAndUI(true);
        });

        btnPreDay.setOnClickListener(v -> {
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            updateDateAndUI(false);
        });

        // Thiết lập biểu đồ đường (LineChart)
        ArrayList<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0f, 5f));    // T2
        entries.add(new Entry(1f, 6f));    // T3
        entries.add(new Entry(2f, 4.5f));  // T4
        entries.add(new Entry(3f, 7f));    // T5
        entries.add(new Entry(4f, 3.5f));  // T6
        entries.add(new Entry(5f, 2f));    // T7
        entries.add(new Entry(6f, 5f));    // CN

        final String[] days = new String[]{"T2", "T3", "T4", "T5", "T6", "T7", "CN"};

        LineDataSet lineDataSet = new LineDataSet(entries, "Số giờ làm việc");
        lineDataSet.setColors(ColorTemplate.MATERIAL_COLORS[3]);
        lineDataSet.setValueTextSize(14f);
        lineDataSet.setLineWidth(2f); // Độ dày đường
        lineDataSet.setCircleRadius(4f); // Kích thước điểm trên đường
        lineDataSet.setDrawCircleHole(false); // Không vẽ lỗ trong điểm

        LineData lineData = new LineData(lineDataSet);
        lineChart.setData(lineData);

        // Thiết lập trục X
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(days));
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(0);

        lineChart.getDescription().setEnabled(false);
        lineChart.animateY(1000);
        lineChart.invalidate();

        // Xử lý bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.navigation_statistic);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                startActivity(new Intent(StatisticActivity.this, Today.class));
                finish();
                return true;
            } else if (itemId == R.id.navigation_upcoming) {
                startActivity(new Intent(StatisticActivity.this, UpcomingActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.navigation_pomo) {
                startActivity(new Intent(StatisticActivity.this, PomodoroActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.navigation_statistic) {
                return true;
            }
            return false;
        });
    }

    // Phương thức cập nhật ngày
    private void updateDateAndUI(boolean isNextDay) {
        if (isNextDay) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        } else {
            calendar.add(Calendar.DAY_OF_MONTH, -1);
        }

        // Cập nhật tvDate để hiển thị ngày
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        tvDate.setText(sdf.format(calendar.getTime()));

        // Cập nhật tvToday để hiển thị trạng thái ngày
        Calendar todayCal = Calendar.getInstance();
        todayCal.set(Calendar.HOUR_OF_DAY, 0);
        todayCal.set(Calendar.MINUTE, 0);
        todayCal.set(Calendar.SECOND, 0);
        todayCal.set(Calendar.MILLISECOND, 0);

        Calendar selectedCal = (Calendar) calendar.clone();
        selectedCal.set(Calendar.HOUR_OF_DAY, 0);
        selectedCal.set(Calendar.MINUTE, 0);
        selectedCal.set(Calendar.SECOND, 0);
        selectedCal.set(Calendar.MILLISECOND, 0);

        if (selectedCal.before(todayCal)) {
            tvToday.setText("Quá khứ");
        } else if (selectedCal.equals(todayCal)) {
            tvToday.setText("Hôm nay");
        } else {
            tvToday.setText("Tương lai");
        }
    }
}