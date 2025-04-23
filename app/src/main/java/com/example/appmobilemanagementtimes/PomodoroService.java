package com.example.appmobilemanagementtimes;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

public class PomodoroService extends Service {

    private static final String TAG = "PomodoroService";
    private static final String CHANNEL_ID = "PomodoroChannel";
    private static final int NOTIFICATION_ID = 2;
    private static final String ACTION_PERMISSION_DENIED = "com.example.appmobilemanagementtimes.PERMISSION_DENIED";
    private CountDownTimer countdownTimer;
    private int countdownSeconds = 11; // Đếm ngược từ 11 để tick cuối là 0
    private NotificationCompat.Builder builder;
    private NotificationManager notificationManager;
    private String userId;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: PomodoroService created");
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        Log.d(TAG, "createNotificationChannel: Creating notification channel");
        CharSequence name = "Pomodoro Notifications";
        String description = "Channel for Pomodoro timer notifications";
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
            Log.d(TAG, "createNotificationChannel: Notification channel created with ID: " + CHANNEL_ID);
        } else {
            Log.e(TAG, "createNotificationChannel: NotificationManager is null");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: PomodoroService started");

        // Lấy userId từ Intent hoặc SharedPreferences
        userId = intent.getStringExtra("userId");
        if (userId == null) {
            userId = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("userId", null);
            Log.w(TAG, "onStartCommand: userId not found in Intent, using SharedPreferences: " + userId);
        }
        if (userId == null) {
            Log.e(TAG, "onStartCommand: userId is null, stopping service");
            stopSelf();
            return START_NOT_STICKY;
        }
        Log.d(TAG, "onStartCommand: userId=" + userId);

        // Kiểm tra quyền POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "onStartCommand: POST_NOTIFICATIONS permission not granted");
            Intent permissionDeniedIntent = new Intent(ACTION_PERMISSION_DENIED);
            sendBroadcast(permissionDeniedIntent);
            Log.d(TAG, "onStartCommand: Sent broadcast " + ACTION_PERMISSION_DENIED);
            stopSelf();
            return START_NOT_STICKY;
        }

        // Retry lấy NotificationManager nếu null
        if (notificationManager == null) {
            for (int i = 0; i < 3; i++) {
                notificationManager = getSystemService(NotificationManager.class);
                if (notificationManager != null) {
                    Log.d(TAG, "onStartCommand: Successfully initialized NotificationManager after retry " + (i + 1));
                    break;
                }
                try {
                    Thread.sleep(100); // Chờ 100ms trước khi thử lại
                } catch (InterruptedException e) {
                    Log.e(TAG, "onStartCommand: Retry interrupted", e);
                }
            }
            if (notificationManager == null) {
                Log.e(TAG, "onStartCommand: Failed to initialize NotificationManager after retries");
                stopSelf();
                return START_NOT_STICKY;
            }
        }

        // Tạo PendingIntent cho notification
        Intent notificationIntent = new Intent(this, PomodoroActivity.class);
        notificationIntent.putExtra("fromNotification", true);
        notificationIntent.putExtra("userId", userId);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        // Tạo notification
        builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Pomodoro Timer")
                .setContentText("Quay lại phiên của bạn! Còn " + countdownSeconds + " giây")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setContentIntent(pendingIntent);

        // Bắt đầu foreground service
        try {
            startForeground(NOTIFICATION_ID, builder.build());
            Log.d(TAG, "onStartCommand: Foreground service started with notification ID: " + NOTIFICATION_ID);
        } catch (Exception e) {
            Log.e(TAG, "onStartCommand: Failed to start foreground service", e);
            stopSelf();
            return START_NOT_STICKY;
        }

        // Dừng timer cũ nếu có
        if (countdownTimer != null) {
            countdownTimer.cancel();
        }

        // Bắt đầu đếm ngược 11s
        countdownSeconds = 11;
        countdownTimer = new CountDownTimer(12000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                countdownSeconds = (int) (millisUntilFinished / 1000);
                if (notificationManager != null) {
                    builder.setContentText("Quay lại phiên của bạn! Còn " + countdownSeconds + " giây");
                    try {
                        notificationManager.notify(NOTIFICATION_ID, builder.build());
                        Log.d(TAG, "Countdown updated: " + countdownSeconds + " seconds left");
                    } catch (Exception e) {
                        Log.e(TAG, "Countdown: Failed to update notification", e);
                    }
                } else {
                    Log.e(TAG, "Countdown: NotificationManager is null");
                    notificationManager = getSystemService(NotificationManager.class);
                }
            }

            @Override
            public void onFinish() {
                Intent resetIntent = new Intent("com.example.appmobilemanagementtimes.RESET_POMODORO");
                sendBroadcast(resetIntent);
                Log.d(TAG, "Countdown finished, sent RESET_POMODORO broadcast");
                stopForeground(true);
                stopSelf();
            }
        }.start();
        Log.d(TAG, "onStartCommand: Started countdown timer for 11 seconds");

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: PomodoroService destroyed");
        if (countdownTimer != null) {
            countdownTimer.cancel();
            countdownTimer = null;
        }
    }
}