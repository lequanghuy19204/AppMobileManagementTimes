package com.example.appmobilemanagementtimes;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
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

        // Tạo dữ liệu cho biểu đồ: mỗi ngày 1 giá trị
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, 5f));    // T2
        entries.add(new BarEntry(1f, 6f));    // T3
        entries.add(new BarEntry(2f, 4.5f));  // T4
        entries.add(new BarEntry(3f, 7f));    // T5
        entries.add(new BarEntry(4f, 3.5f));  // T6
        entries.add(new BarEntry(5f, 2f));    // T7
        entries.add(new BarEntry(6f, 5f));    // CN

        // Nhãn cho trục X
        final String[] days = new String[]{"T2", "T3", "T4", "T5", "T6", "T7", "CN"};

        // Lấy màu từ resources thay vì thuộc tính theme
        int accentColor = getResources().getColor(R.color.accent);
        
        // Lấy màu văn bản dựa trên chế độ sáng/tối hiện tại
        int textColor;
        int nightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
            textColor = getResources().getColor(R.color.text_primary_light); // Màu văn bản cho dark mode
        } else {
            textColor = getResources().getColor(R.color.text_primary_light); // Màu văn bản cho light mode
        }
        
        BarDataSet barDataSet = new BarDataSet(entries, "Số giờ làm việc");
        barDataSet.setColors(accentColor);
        barDataSet.setValueTextSize(14f);
        barDataSet.setValueTextColor(textColor);

        BarData barData = new BarData(barDataSet);
        barChart.setData(barData);

        // Thiết lập trục X hiển thị nhãn ngày
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(days));
        xAxis.setGranularity(1f); // Bước nhảy 1 đơn vị
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(0); // Góc xoay nhãn
        xAxis.setTextColor(textColor); // Đặt màu chữ cho trục X

        // Thiết lập màu chữ cho trục Y bên trái và phải
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setTextColor(textColor);
        
        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setTextColor(textColor);

        // Thiết lập màu cho legend (chú thích)
        barChart.getLegend().setTextColor(textColor);
        
        // Đặt màu nền cho chart
        barChart.setDrawGridBackground(false);
        barChart.setBackgroundColor(Color.TRANSPARENT);
        
        barChart.getDescription().setEnabled(false); // Tắt mô tả mặc định
        barChart.animateY(1000); // Hiệu ứng chạy biểu đồ
        barChart.invalidate(); // Vẽ lại

        // Thêm code xử lý bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.navigation_statistic); // Đánh dấu tab hiện tại

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
}
