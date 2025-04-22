package com.example.appmobilemanagementtimes;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class StatisticActivity extends AppCompatActivity {
    private LineChart lineChart;
    private FirebaseFirestore db;
    private ImageButton btnPreDay, btnNextDay;
    private TextView tvDate, tvToday;
    private Calendar calendar;
    private String userId;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd MMM", Locale.getDefault());
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.statistic);

        // Initialize
        initViews();
        userId = getUserId();
        if (userId == null) return;

        db = FirebaseFirestore.getInstance();

        setupChart();
        setupBottomNavigation();
        updateDateAndUI();
    }

    private void initViews() {
        tvToday = findViewById(R.id.tv_today);
        tvDate = findViewById(R.id.tv_date);
        btnPreDay = findViewById(R.id.btn_prev_day);
        btnNextDay = findViewById(R.id.btn_next_day);
        lineChart = findViewById(R.id.linechart);
        calendar = Calendar.getInstance();

        btnNextDay.setOnClickListener(v -> {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            updateDateAndUI();
        });

        btnPreDay.setOnClickListener(v -> {
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            updateDateAndUI();
        });
    }

    private String getUserId() {
        Intent intent = getIntent();
        String userId = intent.getStringExtra("userId");

        if (userId == null) {
            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            userId = prefs.getString("userId", null);
        }

        if (userId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }

        return userId;
    }

    private void setupChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getLegend().setEnabled(true);
        lineChart.setNoDataText("Đang tải dữ liệu...");
        lineChart.setNoDataTextColor(Color.GRAY);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelRotationAngle(-45);
        xAxis.setValueFormatter(new IndexAxisValueFormatter());
    }

    private void updateDateAndUI() {
        tvDate.setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(calendar.getTime()));

        Calendar today = Calendar.getInstance();
        zeroTime(today);
        Calendar selectedDate = (Calendar) calendar.clone();
        zeroTime(selectedDate);

        if (selectedDate.before(today)) {
            tvToday.setText("Ngày đã qua");
        } else if (selectedDate.equals(today)) {
            tvToday.setText("Hôm nay");
        } else {
            tvToday.setText("Ngày tới");
        }

        loadLast7DaysDoneTasksData();
    }

    private void zeroTime(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    private void loadLast7DaysDoneTasksData() {
        Calendar startCal = (Calendar) calendar.clone();
        startCal.add(Calendar.DAY_OF_MONTH, -6);

        String startDate = dateFormat.format(startCal.getTime());
        String endDate = dateFormat.format(calendar.getTime());

        db.collection("tasks")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "done")
                .whereGreaterThanOrEqualTo("startTime", startDate + " 00:00")
                .whereLessThanOrEqualTo("startTime", endDate + " 23:59")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Map<String, Float> dailyData = initializeDailyData(startCal);
                        int taskCount = 0;

                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            try {
                                String startStr = doc.getString("startTime");
                                String endStr = doc.getString("endTime");

                                if (startStr != null && endStr != null) {
                                    Date start = dateTimeFormat.parse(startStr);
                                    Date end = dateTimeFormat.parse(endStr);

                                    if (start != null && end != null) {
                                        float hours = calculateTaskHours(start, end);
                                        String dayKey = dateFormat.format(start);
                                        if (dailyData.containsKey(dayKey)) {
                                            dailyData.put(dayKey, dailyData.get(dayKey) + hours);
                                            taskCount++;
                                        }
                                    }
                                }
                            } catch (ParseException e) {
                                Log.e(TAG, "Lỗi phân tích thời gian", e);
                            }
                        }

                        if (taskCount == 0) showNoDataMessage();
                        else updateChartWith7DaysData(dailyData, startCal);

                    } else {
                        Toast.makeText(this, "Lỗi khi tải dữ liệu", Toast.LENGTH_SHORT).show();
                        showNoDataMessage();
                    }
                });
    }

    private Map<String, Float> initializeDailyData(Calendar startCal) {
        Map<String, Float> map = new LinkedHashMap<>();
        Calendar temp = (Calendar) startCal.clone();
        for (int i = 0; i < 7; i++) {
            map.put(dateFormat.format(temp.getTime()), 0f);
            temp.add(Calendar.DAY_OF_MONTH, 1);
        }
        return map;
    }

    private float calculateTaskHours(Date start, Date end) {
        long diff = end.getTime() - start.getTime();
        return Math.min((float) diff / (1000 * 60 * 60), 24); // tối đa 24h
    }

    private void updateChartWith7DaysData(Map<String, Float> data, Calendar startCal) {
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        Calendar temp = (Calendar) startCal.clone();
        for (int i = 0; i < 7; i++) {
            String dateKey = dateFormat.format(temp.getTime());
            entries.add(new Entry(i, data.getOrDefault(dateKey, 0f)));
            labels.add(displayDateFormat.format(temp.getTime()));
            temp.add(Calendar.DAY_OF_MONTH, 1);
        }

        LineDataSet dataSet = new LineDataSet(entries, "Giờ làm việc đã hoàn thành");
        dataSet.setColor(ColorTemplate.MATERIAL_COLORS[0]);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setValueTextSize(12f);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setDrawValues(true);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelCount(labels.size());

        lineChart.getAxisLeft().setAxisMinimum(0f);
        lineChart.getAxisLeft().setGranularity(1f);
        lineChart.animateY(1000);
        lineChart.invalidate();
    }

    private void showNoDataMessage() {
        lineChart.clear();
        lineChart.setNoDataText("Không có công việc đã hoàn thành trong 7 ngày này");
        lineChart.invalidate();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.navigation_statistic);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.navigation_home) {
                startActivity(new Intent(this, Today.class).putExtra("userId", userId));
                finish();
                return true;
            } else if (id == R.id.navigation_statistic) {
                return true;
            }
            return false;
        });
    }
}
