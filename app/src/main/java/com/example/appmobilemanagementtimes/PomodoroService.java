package com.example.appmobilemanagementtimes;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class PomodoroService extends Service {
    private static final String TAG = "PomodoroService";
    private static final String CHANNEL_ID = "PomodoroChannel";
    private static final int NOTIFICATION_ID = 1;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int countdown = 10;
    private NotificationManager notificationManager; // Loại bỏ final

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: PomodoroService created");
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            Log.e(TAG, "onCreate: NotificationManager is null");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: Starting PomodoroService");
        Intent notificationIntent = new Intent(this, PomodoroActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Pomodoro Timer")
                .setContentText("Vui lòng quay lại màn hình trong " + countdown + "s, nếu không pomodoro sẽ bị hủy.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        try {
            startForeground(NOTIFICATION_ID, builder.build());
        } catch (Exception e) {
            Log.e(TAG, "onStartCommand: Failed to start foreground service", e);
            stopSelf();
            return START_NOT_STICKY;
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (countdown > 0) {
                    countdown--;
                    builder.setContentText("Vui lòng quay lại màn hình trong " + countdown + "s, nếu không pomodoro sẽ bị hủy.");
                    if (notificationManager != null) {
                        notificationManager.notify(NOTIFICATION_ID, builder.build());
                        Log.d(TAG, "onStartCommand: Notification updated, countdown: " + countdown);
                    } else {
                        Log.e(TAG, "onStartCommand: NotificationManager is null, cannot update notification");
                    }
                    handler.postDelayed(this, 1000);
                } else {
                    Intent broadcastIntent = new Intent("com.example.appmobilemanagementtimes.RESET_POMODORO");
                    broadcastIntent.setPackage("com.example.appmobilemanagementtimes");
                    sendBroadcast(broadcastIntent);
                    stopSelf();
                    Log.d(TAG, "onStartCommand: Countdown finished, broadcast sent with package, service stopped");
                }
            }
        });

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        Log.d(TAG, "onDestroy: PomodoroService destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}