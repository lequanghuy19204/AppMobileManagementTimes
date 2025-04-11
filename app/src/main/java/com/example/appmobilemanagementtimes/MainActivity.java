package com.example.appmobilemanagementtimes;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                return true;
            } else if (itemId == R.id.navigation_upcoming) {
                startActivity(new Intent(MainActivity.this, UpcomingActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.navigation_pomo) {
                // Chuyển đến PomoActivity
                return true;
            } else if (itemId == R.id.navigation_statistic) {
                // Xử lý khi chọn Statistic
                Intent intent = new Intent(MainActivity.this, StatisticActivity.class);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }
}