package com.example.appmobilemanagementtimes;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Today extends AppCompatActivity {
    private ActivityResultLauncher<Intent> createTaskLauncher;
    private ActivityResultLauncher<Intent> updateTaskLauncher;
    private RecyclerView recyclerToday, recyclerDone;
    private Taskadapter2 todayAdapter;
    private Taskadapter3 doneAdapter;
    private List<Task2> todayTasks = new ArrayList<>();
    private List<Task> doneTasks = new ArrayList<>();
    private TextView tvDate;
    private ImageButton btnPrevDay, btnNextDay;
    private Calendar calendar;
    private FirebaseFirestore db;
    private static final String TAG = "Today";

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.today);

        db = FirebaseFirestore.getInstance();

        createTaskLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String taskName = result.getData().getStringExtra("taskName");
                        String startTime = result.getData().getStringExtra("startTime");
                        String endTime = result.getData().getStringExtra("endTime");
                        if (taskName != null && startTime != null && endTime != null) {
                            Task2 newTask = new Task2(taskName, startTime, endTime);
                            addTaskToFirestore(newTask, "overdue");
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
                        String originalTaskId = result.getData().getStringExtra("originalTaskId");
                        if (taskName != null && startTime != null && endTime != null && originalTaskId != null) {
                            db.collection("tasks").document(originalTaskId).delete();
                            Task2 updatedTask = new Task2(taskName, startTime, endTime);
                            addTaskToFirestore(updatedTask, "overdue");
                        }
                    }
                });

        recyclerToday = findViewById(R.id.recycler_todo);
        recyclerDone = findViewById(R.id.recyclerView);
        tvDate = findViewById(R.id.tv_date);
        btnPrevDay = findViewById(R.id.btn_prev_day);
        btnNextDay = findViewById(R.id.btn_next_day);

        recyclerToday.setLayoutManager(new LinearLayoutManager(this));
        recyclerDone.setLayoutManager(new LinearLayoutManager(this));

        todayAdapter = new Taskadapter2(todayTasks,
                task -> {
                    db.collection("tasks").document(task.getName() + "_" + task.getStartTime()).delete();
                    addTaskToFirestore(new Task(task.getName(), task.getStartTime()), "done");
                },
                task -> {
                    db.collection("tasks").document(task.getName() + "_" + task.getStartTime())
                            .delete()
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Task deleted: " + task.getName()))
                            .addOnFailureListener(e -> Log.e(TAG, "Error deleting task", e));
                });
        doneAdapter = new Taskadapter3(doneTasks);
        recyclerToday.setAdapter(todayAdapter);
        recyclerDone.setAdapter(doneAdapter);

        ItemTouchHelper todayTouchHelper = new ItemTouchHelper(new SwipeToActionCallback(todayAdapter));
        todayTouchHelper.attachToRecyclerView(recyclerToday);

        ImageButton imageButton = findViewById(R.id.btn_add);
        imageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Today.this, create_items.class);
            createTaskLauncher.launch(intent);
        });

        calendar = Calendar.getInstance();
        if (getIntent().hasExtra("selectedDate")) {
            calendar.setTimeInMillis(getIntent().getLongExtra("selectedDate", calendar.getTimeInMillis()));
        }
        updateDate();
        listenToFirestoreChanges();

        btnNextDay.setOnClickListener(v -> {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            updateDate();
            listenToFirestoreChanges(); // Gọi lại để lọc dữ liệu cho ngày mới
        });

        btnPrevDay.setOnClickListener(v -> {
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            Calendar todayCal = Calendar.getInstance();
            todayCal.set(Calendar.HOUR_OF_DAY, 0);
            todayCal.set(Calendar.MINUTE, 0);
            todayCal.set(Calendar.SECOND, 0);
            todayCal.set(Calendar.MILLISECOND, 0);

            if (calendar.before(todayCal)) {
                Intent intent = new Intent(Today.this, pass_date.class);
                intent.putExtra("selectedDate", calendar.getTimeInMillis());
                startActivity(intent);
                finish();
            } else {
                updateDate();
                listenToFirestoreChanges(); // Gọi lại để lọc dữ liệu cho ngày mới
            }
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) return true;
            else if (itemId == R.id.navigation_upcoming) return true;
            else if (itemId == R.id.navigation_pomo) return true;
            else if (itemId == R.id.navigation_statistic) return true;
            return false;
        });
    }

    private void addTaskToFirestore(Object task, String status) {
        Map<String, Object> taskData = new HashMap<>();
        if (task instanceof Task2) {
            Task2 t = (Task2) task;
            taskData.put("name", t.getName());
            taskData.put("startTime", t.getStartTime());
            taskData.put("endTime", t.getEndTime());
        } else if (task instanceof Task) {
            Task t = (Task) task;
            taskData.put("name", t.getName());
            taskData.put("time", t.getTime());
        }
        taskData.put("status", status);

        String documentId = taskData.get("name") + "_" + (taskData.get("startTime") != null ? taskData.get("startTime") : taskData.get("time"));
        Log.d(TAG, "Adding task to Firestore: " + taskData.toString());
        db.collection("tasks")
                .document(documentId)
                .set(taskData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Task added successfully: " + documentId))
                .addOnFailureListener(e -> Log.e(TAG, "Error adding task", e));
    }

    private void listenToFirestoreChanges() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String currentDate = sdf.format(calendar.getTime());
        Log.d(TAG, "Listening for tasks on date: " + currentDate);

        db.collection("tasks")
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
                            String time = doc.getString("time");

                            if ("overdue".equals(status) && startTime != null && endTime != null) {
                                String taskDate = startTime.substring(0, 10);
                                if (taskDate.equals(currentDate)) {
                                    todayTasks.add(new Task2(name, startTime, endTime));
                                    Log.d(TAG, "Added overdue task: " + name + " for " + taskDate);
                                }
                            } else if ("done".equals(status) && time != null) {
                                String taskDate = time.substring(0, 10);
                                if (taskDate.equals(currentDate)) {
                                    doneTasks.add(new Task(name, time));
                                    Log.d(TAG, "Added done task: " + name + " for " + taskDate);
                                }
                            }
                        }
                    }

                    todayAdapter.notifyDataSetChanged();
                    doneAdapter.notifyDataSetChanged();
                });
    }

    private void updateDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        tvDate.setText(sdf.format(calendar.getTime()));
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
        }

        @Override
        public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                float dX, float dY, int actionState, boolean isCurrentlyActive) {
            View itemView = viewHolder.itemView;
            ImageButton btnEdit = itemView.findViewById(R.id.btnEditTask);
            ImageButton btnDelete = itemView.findViewById(R.id.btnDeleteTask);

            if (dX < 0) {
                btnEdit.setVisibility(View.VISIBLE);
                btnDelete.setVisibility(View.VISIBLE);

                btnEdit.setOnClickListener(v -> {
                    int position = viewHolder.getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        Task2 task = todayTasks.get(position);
                        Intent intent = new Intent(Today.this, update_items.class);
                        intent.putExtra("taskName", task.getName());
                        intent.putExtra("startTime", task.getStartTime());
                        intent.putExtra("endTime", task.getEndTime());
                        intent.putExtra("originalTaskId", task.getName() + "_" + task.getStartTime());
                        updateTaskLauncher.launch(intent);
                    }
                });
            } else {
                btnEdit.setVisibility(View.GONE);
                btnDelete.setVisibility(View.GONE);
            }
            itemView.setTranslationX(0);
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }
}