package com.example.appmobilemanagementtimes;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

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
    }
}
