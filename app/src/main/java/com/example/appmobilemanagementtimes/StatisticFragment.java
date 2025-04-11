package com.example.appmobilemanagementtimes;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.components.XAxis;

import java.util.ArrayList;

public class StatisticFragment extends AppCompatActivity {
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

        BarDataSet barDataSet = new BarDataSet(entries, "Số giờ làm việc");
        barDataSet.setColors(ColorTemplate.MATERIAL_COLORS[3]);
        barDataSet.setValueTextSize(14f);

        BarData barData = new BarData(barDataSet);
        barChart.setData(barData);

        // Thiết lập trục X hiển thị nhãn ngày
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(days));
        xAxis.setGranularity(1f); // Bước nhảy 1 đơn vị
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(0); // Góc xoay nhãn

        barChart.getDescription().setEnabled(false); // Tắt mô tả mặc định
        barChart.animateY(1000); // Hiệu ứng chạy biểu đồ
        barChart.invalidate(); // Vẽ lại
    }
}