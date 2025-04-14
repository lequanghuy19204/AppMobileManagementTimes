package com.example.appmobilemanagementtimes;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Today extends AppCompatActivity {
    private LinearLayout rootLayout;
    private ScrollView taskScrollView;
    private ActivityResultLauncher<Intent> createTaskLauncher;
    private ActivityResultLauncher<Intent> updateTaskLauncher;
    private ActivityResultLauncher<Intent> exactAlarmPermissionLauncher;
    private RecyclerView recyclerToday, recyclerDone;
    private Taskadapter2 todayAdapter;
    private Taskadapter3 doneAdapter;
    private List<Task2> todayTasks = new ArrayList<>();
    private List<Task2> doneTasks = new ArrayList<>();
    private TextView tvToday, tvDate, tvTodoLabel;
    private ImageButton btnPrevDay, btnNextDay, btnAdd, btnNotification;
    private Calendar calendar;
    private FirebaseFirestore db;
    private DrawerLayout drawerLayout;
    private static final String TAG = "Today";
    private boolean isOverdue = false;
    private String pendingTaskName, pendingStartTime, pendingReminder, pendingGroupId;
    private String userId; // Thêm biến userId

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.today);

        // Lấy userId từ Intent hoặc SharedPreferences
        Intent intent = getIntent();
        userId = intent.getStringExtra("userId");
        if (userId == null) {
            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            userId = prefs.getString("userId", null);
            if (userId == null) {
                Log.e(TAG, "Không tìm thấy userId, chuyển hướng về đăng nhập");
                Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
                Intent loginIntent = new Intent(this, LoginActivity.class);
                startActivity(loginIntent);
                finish();
                return;
            }
        }
        Log.d(TAG, "UserId: " + userId);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        navigationView.setNavigationItemSelectedListener(item -> {
            navigationView.getMenu().setGroupCheckable(0, true, true);
            item.setChecked(true);
            int itemId = item.getItemId();
            if (itemId == R.id.nav_logout) {
                // Xử lý đăng xuất
                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                prefs.edit().remove("userId").apply();
                Intent loginIntent = new Intent(Today.this, LoginActivity.class); // Sử dụng tên khác để tránh xung đột
                startActivity(loginIntent);
                finish();
            } else if (itemId == R.id.nav_language) {
                // Handle language
            } else if (itemId == R.id.nav_dark_mode) {
                // Handle dark mode
            } else if (itemId == R.id.nav_setting) {
                // Handle settings
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        ImageView imgAvatar = findViewById(R.id.img_avatar);
        imgAvatar.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        rootLayout = findViewById(R.id.root_layout);
        taskScrollView = findViewById(R.id.task);
        db = FirebaseFirestore.getInstance();

        exactAlarmPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (pendingTaskName != null && pendingStartTime != null && pendingReminder != null && pendingGroupId != null) {
                        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                            scheduleReminderAfterPermission(pendingTaskName, pendingStartTime, pendingReminder, pendingGroupId);
                        } else {
                            scheduleInexactReminder(pendingTaskName, pendingStartTime, pendingReminder, pendingGroupId);
                        }
                        pendingTaskName = null;
                        pendingStartTime = null;
                        pendingReminder = null;
                        pendingGroupId = null;
                    }
                });

        createTaskLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String taskName = result.getData().getStringExtra("taskName");
                        String startTime = result.getData().getStringExtra("startTime");
                        String endTime = result.getData().getStringExtra("endTime");
                        String repeatMode = result.getData().getStringExtra("repeatMode");
                        String reminder = result.getData().getStringExtra("reminder");
                        String groupId = result.getData().getStringExtra("groupId");
                        String label = result.getData().getStringExtra("label");
                        if (taskName != null && startTime != null && endTime != null) {
                            Log.d(TAG, "Task created: " + taskName + ", groupId: " + groupId + ", reminder: " + reminder + ", label: " + label);
                            Task2 task = new Task2(taskName, startTime, endTime, repeatMode, groupId, reminder, label, userId);
                            addTaskToFirestore(task, "overdue");
                            scheduleReminder(taskName, startTime, reminder, groupId);
                            if (groupId != null && !repeatMode.equals("never")) {
                                createRecurringTasks(taskName, startTime, endTime, repeatMode, reminder, groupId, label);
                            }
                        }
                    }
                });

        updateTaskLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String taskName = result.getData().getStringExtra("taskName");
                        String startTime = result.getData().getStringExtra("startTime");
                        String endTime = result.getData().getStringExtra("endTime");
                        String repeatMode = result.getData().getStringExtra("repeatMode");
                        String reminder = result.getData().getStringExtra("reminder");
                        String groupId = result.getData().getStringExtra("groupId");
                        String newGroupId = result.getData().getStringExtra("newGroupId");
                        String originalTaskId = result.getData().getStringExtra("originalTaskId");
                        String label = result.getData().getStringExtra("label");
                        if (taskName != null && startTime != null && endTime != null && originalTaskId != null) {
                            Log.d(TAG, "Cập nhật nhiệm vụ - Tên: " + taskName + ", Thời gian bắt đầu: " + startTime +
                                    ", Thời gian kết thúc: " + endTime + ", Chế độ lặp: " + repeatMode +
                                    ", Nhắc nhở: " + reminder + ", GroupId: " + groupId +
                                    ", NewGroupId: " + newGroupId + ", OriginalTaskId: " + originalTaskId +
                                    ", Nhãn: " + label);

                            long timeDiffInMinutes = calculateTimeDifferenceInMinutes(startTime, endTime);
                            if (timeDiffInMinutes <= 0) {
                                Toast.makeText(Today.this, "Thời gian kết thúc phải sau thời gian bắt đầu!", Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Thời gian không hợp lệ, bỏ qua cập nhật nhiệm vụ");
                                return;
                            }
                            Log.d(TAG, "Khoảng cách thời gian: " + timeDiffInMinutes + " phút");

                            db.collection("tasks").document(originalTaskId)
                                    .delete()
                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Đã xóa nhiệm vụ gốc: " + originalTaskId))
                                    .addOnFailureListener(e -> Log.e(TAG, "Lỗi khi xóa nhiệm vụ gốc: " + originalTaskId, e));
                            cancelReminder(originalTaskId);

                            if (groupId != null && !groupId.isEmpty()) {
                                deleteTasksByGroupId(groupId);
                            }

                            Task2 updatedTask = new Task2(taskName, startTime, endTime, repeatMode, newGroupId, reminder, label, userId);
                            addTaskToFirestore(updatedTask, "overdue");

                            scheduleReminder(taskName, startTime, reminder, newGroupId);

                            if (newGroupId != null && !repeatMode.equals("never")) {
                                createRecurringTasks(taskName, startTime, endTime, repeatMode, reminder, newGroupId, label);
                            }

                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            String currentDate = sdf.format(calendar.getTime());
                            String taskDate = startTime.substring(0, 10);
                            if (!taskDate.equals(currentDate)) {
                                for (int i = 0; i < todayTasks.size(); i++) {
                                    Task2 task = todayTasks.get(i);
                                    if ((task.getName() + "_" + task.getStartTime()).equals(originalTaskId)) {
                                        todayTasks.remove(i);
                                        todayAdapter.notifyItemRemoved(i);
                                        break;
                                    }
                                }
                            }
                        } else {
                            Log.e(TAG, "Dữ liệu không hợp lệ nhận được từ update_items");
                            Toast.makeText(Today.this, "Dữ liệu nhiệm vụ không hợp lệ!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        recyclerToday = findViewById(R.id.recycler_todo);
        recyclerDone = findViewById(R.id.recyclerView);
        tvToday = findViewById(R.id.tv_today);
        tvDate = findViewById(R.id.tv_date);
        tvTodoLabel = findViewById(R.id.tv_todo_label);
        btnPrevDay = findViewById(R.id.btn_prev_day);
        btnNextDay = findViewById(R.id.btn_next_day);
        btnAdd = findViewById(R.id.btn_add);
        btnNotification = findViewById(R.id.btn_notification);

        recyclerToday.setLayoutManager(new LinearLayoutManager(this));
        recyclerDone.setLayoutManager(new LinearLayoutManager(this));

        todayAdapter = new Taskadapter2(todayTasks, isOverdue,
                task -> {
                    String taskId = task.getName() + "_" + task.getStartTime();
                    Log.d(TAG, "Marking task as done: " + taskId);
                    db.collection("tasks").document(taskId).delete();
                    cancelReminder(taskId);
                    Task2 doneTask = new Task2(task.getName(), task.getStartTime(), null, task.getRepeatMode(), task.getGroupId(), task.getReminder(), task.getLabel(), userId);
                    addTaskToFirestore(doneTask, "done");
                },
                task -> {
                    String taskId = task.getName() + "_" + task.getStartTime();
                    String groupId = task.getGroupId();
                    Log.d(TAG, "Deleting task: " + taskId + ", groupId: " + groupId);
                    if (groupId != null && !groupId.isEmpty()) {
                        deleteTasksByGroupId(groupId);
                    } else {
                        db.collection("tasks").document(taskId)
                                .delete()
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Task deleted: " + taskId))
                                .addOnFailureListener(e -> Log.e(TAG, "Error deleting task: " + taskId, e));
                        cancelReminder(taskId);
                    }
                });
        doneAdapter = new Taskadapter3(doneTasks);
        recyclerToday.setAdapter(todayAdapter);
        recyclerDone.setAdapter(doneAdapter);

        ItemTouchHelper todayTouchHelper = new ItemTouchHelper(new SwipeToActionCallback(todayAdapter));
        todayTouchHelper.attachToRecyclerView(recyclerToday);

        btnAdd.setOnClickListener(v -> {
            Intent addIntent = new Intent(Today.this, create_items.class);
            createTaskLauncher.launch(addIntent);
        });



        calendar = Calendar.getInstance();
        if (intent.hasExtra("selectedDate")) {
            calendar.setTimeInMillis(intent.getLongExtra("selectedDate", calendar.getTimeInMillis()));
        }
        updateDateAndUI();

        btnNextDay.setOnClickListener(v -> {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            updateDateAndUI();
        });

        btnPrevDay.setOnClickListener(v -> {
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            updateDateAndUI();
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                return true;
            }
            else if (itemId == R.id.navigation_upcoming)
            {
                startActivity(new Intent(Today.this, UpcomingActivity.class));
                finish();
                return true;
            }
            else if (itemId == R.id.navigation_pomo) {
                startActivity(new Intent(Today.this, PomodoroActivity.class));
                finish();
                return true;
            }
            else if (itemId == R.id.navigation_statistic) {
                startActivity(new Intent(Today.this, StatisticActivity.class));
                finish();
                return true;
            }
            return false;
        });

        rootLayout.setOnTouchListener((v, event) -> {
            hideSwipeActions();
            return false;
        });

        taskScrollView.setOnTouchListener((v, event) -> {
            hideSwipeActions();
            return false;
        });

        recyclerToday.setOnTouchListener((v, event) -> {
            hideSwipeActions();
            return false;
        });

        recyclerDone.setOnTouchListener((v, event) -> {
            hideSwipeActions();
            return false;
        });
    }

    private void addTaskToFirestore(Task2 task, String status) {
        Map<String, Object> taskData = new HashMap<>();
        taskData.put("name", task.getName());
        taskData.put("startTime", task.getStartTime());
        taskData.put("endTime", task.getEndTime());
        taskData.put("status", status);
        taskData.put("repeatMode", task.getRepeatMode());
        taskData.put("groupId", task.getGroupId());
        taskData.put("reminder", task.getReminder());
        taskData.put("label", task.getLabel());
        taskData.put("userId", task.getUserId());

        String documentId = task.getName() + "_" + task.getStartTime();
        Log.d(TAG, "Adding task to Firestore: " + taskData.toString());
        db.collection("tasks")
                .document(documentId)
                .set(taskData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Task added successfully: " + documentId))
                .addOnFailureListener(e -> Log.e(TAG, "Error adding task", e));
    }

    private void deleteTasksByGroupId(String groupId) {
        if (groupId == null || groupId.isEmpty()) {
            Log.e(TAG, "GroupId is null or empty, cannot delete recurring tasks");
            return;
        }

        Log.d(TAG, "Attempting to delete tasks with groupId: " + groupId);
        db.collection("tasks")
                .whereEqualTo("groupId", groupId)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "Found " + querySnapshot.size() + " tasks with groupId: " + groupId);
                    if (querySnapshot.isEmpty()) {
                        Log.d(TAG, "No tasks found for groupId: " + groupId);
                    }
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String docId = doc.getId();
                        db.collection("tasks").document(docId)
                                .delete()
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Deleted task: " + docId))
                                .addOnFailureListener(e -> Log.e(TAG, "Error deleting task: " + docId, e));
                        cancelReminder(docId);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error querying tasks by groupId: " + groupId, e));
    }

    private void deleteAllTasks() {
        Log.d(TAG, "Attempting to delete all tasks for user: " + userId);
        db.collection("tasks")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "Found " + querySnapshot.size() + " tasks to delete");
                    if (querySnapshot.isEmpty()) {
                        Log.d(TAG, "No tasks found in Firestore");
                    }
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String docId = doc.getId();
                        db.collection("tasks").document(docId)
                                .delete()
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Deleted task: " + docId))
                                .addOnFailureListener(e -> Log.e(TAG, "Error deleting task: " + docId, e));
                        cancelReminder(docId);
                    }
                    todayTasks.clear();
                    doneTasks.clear();
                    todayAdapter.notifyDataSetChanged();
                    doneAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error querying tasks for deletion", e));
    }

    private long calculateTimeDifferenceInMinutes(String startTime, String endTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        try {
            Date startDate = sdf.parse(startTime);
            Date endDate = sdf.parse(endTime);

            if (startDate == null || endDate == null) {
                Log.e(TAG, "Không thể phân tích thời gian: startTime=" + startTime + ", endTime=" + endTime);
                return -1;
            }

            long diffInMillis = endDate.getTime() - startDate.getTime();
            long diffInMinutes = diffInMillis / (1000 * 60);

            if (diffInMinutes <= 0) {
                Log.w(TAG, "Thời gian kết thúc phải sau thời gian bắt đầu: diffInMinutes=" + diffInMinutes);
                return -1;
            }

            return diffInMinutes;
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi tính khoảng cách thời gian", e);
            return -1;
        }
    }

    private void cancelReminder(String taskId) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ReminderBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                taskId.hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            Log.d(TAG, "Cancelled reminder for taskId: " + taskId);
        }
    }

    private void createRecurringTasks(String taskName, String startTime, String endTime, String repeatMode, String reminder, String groupId, String label) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        Calendar startCal = Calendar.getInstance();
        Calendar endCal = Calendar.getInstance();

        try {
            startCal.setTime(sdf.parse(startTime));
            endCal.setTime(sdf.parse(endTime));

            for (int i = 0; i < 365; i++) {
                switch (repeatMode) {
                    case "every_day":
                        startCal.add(Calendar.DAY_OF_MONTH, 1);
                        endCal.add(Calendar.DAY_OF_MONTH, 1);
                        break;
                    case "every_week":
                        startCal.add(Calendar.WEEK_OF_YEAR, 1);
                        endCal.add(Calendar.WEEK_OF_YEAR, 1);
                        break;
                    case "every_2_weeks":
                        startCal.add(Calendar.WEEK_OF_YEAR, 2);
                        endCal.add(Calendar.WEEK_OF_YEAR, 2);
                        break;
                    case "every_3_weeks":
                        startCal.add(Calendar.WEEK_OF_YEAR, 3);
                        endCal.add(Calendar.WEEK_OF_YEAR, 3);
                        break;
                    case "every_month":
                        startCal.add(Calendar.MONTH, 1);
                        endCal.add(Calendar.MONTH, 1);
                        break;
                    case "every_year":
                        startCal.add(Calendar.YEAR, 1);
                        endCal.add(Calendar.YEAR, 1);
                        break;
                    default:
                        return;
                }

                String newStartTime = sdf.format(startCal.getTime());
                String newEndTime = sdf.format(endCal.getTime());
                Task2 recurringTask = new Task2(taskName, newStartTime, newEndTime, repeatMode, groupId, reminder, label, userId);
                addTaskToFirestore(recurringTask, "overdue");

                scheduleReminder(taskName, newStartTime, reminder, groupId);

                if (startCal.getTimeInMillis() > Calendar.getInstance().getTimeInMillis() + 365L * 24 * 60 * 60 * 1000) {
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating recurring tasks", e);
        }
    }

    private void scheduleReminder(String taskName, String startTime, String reminder, String groupId) {
        if (reminder.equals("none")) return;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        Calendar reminderCal = Calendar.getInstance();

        try {
            reminderCal.setTime(sdf.parse(startTime));

            switch (reminder) {
                case "1m":
                    reminderCal.add(Calendar.MINUTE, -1);
                    break;
                case "5m":
                    reminderCal.add(Calendar.MINUTE, -5);
                    break;
                case "15m":
                    reminderCal.add(Calendar.MINUTE, -15);
                    break;
                case "30m":
                    reminderCal.add(Calendar.MINUTE, -30);
                    break;
                case "1h":
                    reminderCal.add(Calendar.HOUR_OF_DAY, -1);
                    break;
                case "1d":
                    reminderCal.add(Calendar.DAY_OF_MONTH, -1);
                    break;
            }

            if (reminderCal.getTimeInMillis() > System.currentTimeMillis()) {
                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                Intent intent = new Intent(this, ReminderBroadcastReceiver.class);
                intent.putExtra("taskName", taskName);
                intent.putExtra("taskId", taskName + "_" + startTime);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        this,
                        (taskName + "_" + startTime).hashCode(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (!alarmManager.canScheduleExactAlarms()) {
                        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                        boolean hasPrompted = prefs.getBoolean("hasPromptedExactAlarm", false);
                        if (!hasPrompted) {
                            Log.w(TAG, "Không thể lên lịch báo thức chính xác, yêu cầu người dùng cấp quyền");
                            Toast.makeText(this, "Vui lòng cấp quyền báo thức chính xác để nhận nhắc nhở đúng giờ.", Toast.LENGTH_LONG).show();
                            pendingTaskName = taskName;
                            pendingStartTime = startTime;
                            pendingReminder = reminder;
                            pendingGroupId = groupId;
                            Intent permissionIntent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            permissionIntent.setData(android.net.Uri.fromParts("package", getPackageName(), null));
                            exactAlarmPermissionLauncher.launch(permissionIntent);
                            prefs.edit().putBoolean("hasPromptedExactAlarm", true).apply();
                        }
                        return;
                    }
                }

                try {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderCal.getTimeInMillis(), pendingIntent);
                    Log.d(TAG, "Đã lên lịch nhắc nhở cho nhiệm vụ: " + taskName + " vào " + sdf.format(reminderCal.getTime()));
                } catch (SecurityException e) {
                    Log.e(TAG, "SecurityException khi đặt báo thức chính xác", e);
                    alarmManager.set(AlarmManager.RTC_WAKEUP, reminderCal.getTimeInMillis(), pendingIntent);
                }
            } else {
                Log.d(TAG, "Nhắc nhở trong quá khứ, không lên lịch: " + taskName + " vào " + sdf.format(reminderCal.getTime()));
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi lên lịch nhắc nhở", e);
        }
    }

    private void scheduleReminderAfterPermission(String taskName, String startTime, String reminder, String groupId) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        Calendar reminderCal = Calendar.getInstance();

        try {
            reminderCal.setTime(sdf.parse(startTime));

            switch (reminder) {
                case "1m":
                    reminderCal.add(Calendar.MINUTE, -1);
                    break;
                case "5m":
                    reminderCal.add(Calendar.MINUTE, -5);
                    break;
                case "15m":
                    reminderCal.add(Calendar.MINUTE, -15);
                    break;
                case "30m":
                    reminderCal.add(Calendar.MINUTE, -30);
                    break;
                case "1h":
                    reminderCal.add(Calendar.HOUR_OF_DAY, -1);
                    break;
                case "1d":
                    reminderCal.add(Calendar.DAY_OF_MONTH, -1);
                    break;
            }

            if (reminderCal.getTimeInMillis() > System.currentTimeMillis()) {
                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                Intent intent = new Intent(this, ReminderBroadcastReceiver.class);
                intent.putExtra("taskName", taskName);
                intent.putExtra("taskId", taskName + "_" + startTime);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        this,
                        (taskName + "_" + startTime).hashCode(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderCal.getTimeInMillis(), pendingIntent);
                Log.d(TAG, "Scheduled reminder after permission granted for task: " + taskName + " at " + sdf.format(reminderCal.getTime()));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling reminder after permission", e);
        }
    }

    private void scheduleInexactReminder(String taskName, String startTime, String reminder, String groupId) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        Calendar reminderCal = Calendar.getInstance();

        try {
            reminderCal.setTime(sdf.parse(startTime));

            switch (reminder) {
                case "1m":
                    reminderCal.add(Calendar.MINUTE, -1);
                    break;
                case "5m":
                    reminderCal.add(Calendar.MINUTE, -5);
                    break;
                case "15m":
                    reminderCal.add(Calendar.MINUTE, -15);
                    break;
                case "30m":
                    reminderCal.add(Calendar.MINUTE, -30);
                    break;
                case "1h":
                    reminderCal.add(Calendar.HOUR_OF_DAY, -1);
                    break;
                case "1d":
                    reminderCal.add(Calendar.DAY_OF_MONTH, -1);
                    break;
            }

            if (reminderCal.getTimeInMillis() > System.currentTimeMillis()) {
                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                Intent intent = new Intent(this, ReminderBroadcastReceiver.class);
                intent.putExtra("taskName", taskName);
                intent.putExtra("taskId", taskName + "_" + startTime);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        this,
                        (taskName + "_" + startTime).hashCode(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                alarmManager.set(AlarmManager.RTC_WAKEUP, reminderCal.getTimeInMillis(), pendingIntent);
                Log.d(TAG, "Scheduled inexact reminder for task: " + taskName + " at " + sdf.format(reminderCal.getTime()));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling inexact reminder", e);
        }
    }

    private void hideSwipeActions() {
        for (int i = 0; i < recyclerToday.getChildCount(); i++) {
            View itemView = recyclerToday.getChildAt(i);
            ImageView label = itemView.findViewById(R.id.label);
            ImageButton btnEdit = itemView.findViewById(R.id.btnEditTask);
            ImageButton btnDelete = itemView.findViewById(R.id.btnDeleteTask);
            if (btnEdit != null) btnEdit.setVisibility(View.GONE);
            if (btnDelete != null) btnDelete.setVisibility(View.GONE);
            if (label != null) label.setVisibility(View.VISIBLE);
        }
    }

    private void listenToFirestoreChanges() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String currentDate = sdf.format(calendar.getTime());
        Log.d(TAG, "Listening for tasks on date: " + currentDate);

        db.collection("tasks")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Firestore listen failed", e);
                        return;
                    }

                    todayTasks.clear();
                    doneTasks.clear();

                    if (snapshots == null || snapshots.isEmpty()) {
                        Log.d(TAG, "No tasks found in Firestore");
                    } else {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            Log.d(TAG, "Found task: " + doc.getData().toString());
                            String status = doc.getString("status");
                            String name = doc.getString("name");
                            String startTime = doc.getString("startTime");
                            String endTime = doc.getString("endTime");
                            String repeatMode = doc.getString("repeatMode");
                            String groupId = doc.getString("groupId");
                            String reminder = doc.getString("reminder");
                            String label = doc.getString("label");
                            String taskUserId = doc.getString("userId");

                            if ("overdue".equals(status) && startTime != null) {
                                String taskDate = startTime.substring(0, 10);
                                if (taskDate.equals(currentDate)) {
                                    todayTasks.add(new Task2(name, startTime, endTime, repeatMode, groupId, reminder, label, taskUserId));
                                    Log.d(TAG, "Added task: " + name + " for " + taskDate);
                                }
                            } else if ("done".equals(status) && startTime != null) {
                                String taskDate = startTime.substring(0, 10);
                                if (taskDate.equals(currentDate)) {
                                    doneTasks.add(new Task2(name, startTime, null, repeatMode, groupId, reminder, label, taskUserId));
                                    Log.d(TAG, "Added done task: " + name + " for " + taskDate);
                                }
                            }
                        }
                    }

                    todayAdapter.setOverdue(isOverdue);
                    todayAdapter.notifyDataSetChanged();
                    doneAdapter.notifyDataSetChanged();
                });
    }

    private void updateDateAndUI() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        tvDate.setText(sdf.format(calendar.getTime()));

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
            tvTodoLabel.setText("Overdue");
            tvToday.setText("Past Date");
            btnAdd.setVisibility(View.GONE);
            isOverdue = true;
        } else if (selectedCal.equals(todayCal)) {
            tvTodoLabel.setText("To Do");
            tvToday.setText("Today");
            btnAdd.setVisibility(View.VISIBLE);
            isOverdue = false;
        } else {
            tvTodoLabel.setText("To Do");
            tvToday.setText("Future Date");
            btnAdd.setVisibility(View.VISIBLE);
            isOverdue = false;
        }

        listenToFirestoreChanges();
    }

    private class SwipeToActionCallback extends ItemTouchHelper.SimpleCallback {
        private Taskadapter2 mAdapter;

        SwipeToActionCallback(Taskadapter2 adapter) {
            super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
            mAdapter = adapter;
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            mAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
            Log.d(TAG, "Swiped item at position: " + viewHolder.getAdapterPosition() + ", direction: " + direction);
        }

        @Override
        public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                float dX, float dY, int actionState, boolean isCurrentlyActive) {
            View itemView = viewHolder.itemView;
            ImageView label = itemView.findViewById(R.id.label);
            ImageButton btnEdit = itemView.findViewById(R.id.btnEditTask);
            ImageButton btnDelete = itemView.findViewById(R.id.btnDeleteTask);

            if (dX < 0 && actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                Log.d(TAG, "Swipe left detected, dX: " + dX);
                if (label != null) label.setVisibility(View.GONE);
                if (btnEdit != null) btnEdit.setVisibility(View.VISIBLE);
                if (btnDelete != null) btnDelete.setVisibility(View.VISIBLE);

                if (btnEdit != null) {
                    btnEdit.setOnClickListener(v -> {
                        int position = viewHolder.getAdapterPosition();
                        Log.d(TAG, "Edit button clicked at position: " + position);
                        if (position != RecyclerView.NO_POSITION && position < todayTasks.size()) {
                            Task2 task = todayTasks.get(position);
                            if (task != null) {
                                String taskId = task.getName() + "_" + task.getStartTime();
                                Log.d(TAG, "Opening edit for task: " + taskId);
                                Intent intent = new Intent(Today.this, update_items.class);
                                intent.putExtra("taskName", task.getName());
                                intent.putExtra("startTime", task.getStartTime());
                                intent.putExtra("endTime", task.getEndTime());
                                intent.putExtra("repeatMode", task.getRepeatMode());
                                intent.putExtra("reminder", task.getReminder());
                                intent.putExtra("groupId", task.getGroupId());
                                intent.putExtra("label", task.getLabel());
                                intent.putExtra("originalTaskId", taskId);
                                try {
                                    updateTaskLauncher.launch(intent);
                                    Log.d(TAG, "Launched update_items for task: " + taskId);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error launching update_items", e);
                                }
                            } else {
                                Log.e(TAG, "Task is null at position: " + position);
                            }
                        } else {
                            Log.e(TAG, "Invalid position: " + position);
                        }
                    });
                }

                if (btnDelete != null) {
                    btnDelete.setOnClickListener(v -> {
                        int position = viewHolder.getAdapterPosition();
                        Log.d(TAG, "Delete button clicked at position: " + position);
                        if (position != RecyclerView.NO_POSITION && position < todayTasks.size()) {
                            Task2 task = todayTasks.get(position);
                            if (task != null) {
                                String taskId = task.getName() + "_" + task.getStartTime();
                                String groupId = task.getGroupId();
                                Log.d(TAG, "Deleting task: " + taskId + ", groupId: " + groupId);
                                if (groupId != null && !groupId.isEmpty()) {
                                    deleteTasksByGroupId(groupId);
                                } else {
                                    db.collection("tasks")
                                            .document(taskId)
                                            .delete()
                                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Task deleted: " + taskId))
                                            .addOnFailureListener(e -> Log.e(TAG, "Error deleting task: " + taskId, e));
                                    cancelReminder(taskId);
                                }
                            } else {
                                Log.e(TAG, "Task is null at position: " + position);
                            }
                        } else {
                            Log.e(TAG, "Invalid position: " + position);
                        }
                    });
                }
            } else {
                if (label != null) label.setVisibility(View.VISIBLE);
                if (btnEdit != null) btnEdit.setVisibility(View.GONE);
                if (btnDelete != null) btnDelete.setVisibility(View.GONE);
            }
            itemView.setTranslationX(0);
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }
}