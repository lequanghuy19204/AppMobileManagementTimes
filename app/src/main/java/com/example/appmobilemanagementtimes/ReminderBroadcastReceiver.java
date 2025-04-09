package com.example.appmobilemanagementtimes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import android.app.NotificationManager;
import android.app.PendingIntent;

public class ReminderBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String taskName = intent.getStringExtra("taskName");

        Intent notificationIntent = new Intent(context, Today.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "reminder_channel")
                .setSmallIcon(R.drawable.ic_calendar)
                .setContentTitle("Nhắc nhở công việc")
                .setContentText("Công việc: " + taskName + " sắp bắt đầu!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(taskName.hashCode(), builder.build());
    }
}