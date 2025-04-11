package com.example.appmobilemanagementtimes;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;

public class StatisticActivity extends AppCompatActivity {
    private BarChart barChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.statistic);

        barChart = findViewById(R.id.barchart);

        // Tạo dữ liệu cho biểu đồ
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, 30f));
        entries.add(new BarEntry(1f, 80f));
        entries.add(new BarEntry(2f, 60f));
        entries.add(new BarEntry(3f, 90f));
        entries.add(new BarEntry(4f, 70f));

        // Tạo BarDataSet và thiết lập các tùy chọn
        BarDataSet barDataSet = new BarDataSet(entries, "Sample Data");
        barDataSet.setColors(ColorTemplate.MATERIAL_COLORS);

        // Tạo BarData và thiết lập cho biểu đồ
        BarData barData = new BarData(barDataSet);
        barChart.setData(barData);
        barChart.invalidate(); // Cập nhật lại biểu đồ

        // Thêm code xử lý bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.navigation_statistic); // Đánh dấu tab hiện tại
        
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                startActivity(new Intent(StatisticActivity.this, MainActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.navigation_upcoming) {
                startActivity(new Intent(StatisticActivity.this, UpcomingActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.navigation_pomo) {
                // Chuyển đến PomoActivity
                return true;
            } else if (itemId == R.id.navigation_statistic) {
                return true;
            }
            return false;
        });
    }
}
