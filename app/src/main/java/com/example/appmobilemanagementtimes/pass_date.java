package com.example.appmobilemanagementtimes;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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

public class pass_date extends AppCompatActivity {
    private RecyclerView recyclerOverdue, recyclerDone;
    private LinearLayout Recyclerview;
    private ScrollView TASK;
    private Taskadapter4 overdueAdapter;
    private Taskadapter3 doneAdapter;
    private List<Task2> overdueTasks = new ArrayList<>();
    private List<Task> doneTasks = new ArrayList<>();
    private TextView tvDate;
    private ImageButton btnPrevDay, btnNextDay;
    private Calendar calendar;
    private FirebaseFirestore db;
    private ActivityResultLauncher<Intent> updateTaskLauncher;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.passdate);

        db = FirebaseFirestore.getInstance();

        updateTaskLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String taskName = result.getData().getStringExtra("taskName");
                        String startTime = result.getData().getStringExtra("startTime");
                        String endTime = result.getData().getStringExtra("endTime");
                        String originalTaskId = result.getData().getStringExtra("originalTaskId");

                        if (taskName != null && startTime != null && endTime != null && originalTaskId != null) {
                            db.collection("tasks").document(originalTaskId)
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Task2 updatedTask = new Task2(taskName, startTime, endTime);
                                        Map<String, Object> taskData = new HashMap<>();
                                        taskData.put("name", updatedTask.getName());
                                        taskData.put("startTime", updatedTask.getStartTime());
                                        taskData.put("endTime", updatedTask.getEndTime());
                                        taskData.put("status", "overdue");

                                        String newTaskId = taskName + "_" + startTime;
                                        db.collection("tasks")
                                                .document(newTaskId)
                                                .set(taskData)
                                                .addOnSuccessListener(aVoid1 -> {
                                                    checkAndTransferToToday(startTime);
                                                })
                                                .addOnFailureListener(e -> Log.e("pass_date", "Error adding updated task", e));
                                    })
                                    .addOnFailureListener(e -> Log.e("pass_date", "Error deleting old task", e));
                        }
                    }
                });

        recyclerOverdue = findViewById(R.id.recycler_todo);
        recyclerDone = findViewById(R.id.recyclerView);
        Recyclerview = findViewById(R.id.root_layout);
        TASK = findViewById(R.id.task);
        tvDate = findViewById(R.id.tv_date);
        btnPrevDay = findViewById(R.id.btn_prev_day);
        btnNextDay = findViewById(R.id.btn_next_day);

        recyclerOverdue.setLayoutManager(new LinearLayoutManager(this));
        recyclerOverdue.setHasFixedSize(true);
        recyclerDone.setLayoutManager(new LinearLayoutManager(this));
        recyclerDone.setHasFixedSize(true);

        overdueAdapter = new Taskadapter4(overdueTasks, task -> {
            String taskId = task.getName() + "_" + task.getStartTime();
            db.collection("tasks").document(taskId)
                    .delete()
                    .addOnSuccessListener(aVoid -> addTaskToFirestore(new Task(task.getName(), task.getStartTime()), "done"));
        });
        doneAdapter = new Taskadapter3(doneTasks);
        recyclerOverdue.setAdapter(overdueAdapter);
        recyclerDone.setAdapter(doneAdapter);

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeToActionCallback(overdueAdapter));
        itemTouchHelper.attachToRecyclerView(recyclerOverdue);

        Recyclerview.setOnTouchListener((v, event) -> {
            hideEditDeleteButtons();
            return false;
        });
        recyclerOverdue.setOnTouchListener((v, event) -> {
            hideEditDeleteButtons();
            return false;
        });
        recyclerDone.setOnTouchListener((v, event) -> {
            hideEditDeleteButtons();
            return false;
        });
        TASK.setOnTouchListener((v, event) -> {
            hideEditDeleteButtons();
            return false;
        });

        calendar = Calendar.getInstance();
        if (getIntent().hasExtra("selectedDate")) {
            calendar.setTimeInMillis(getIntent().getLongExtra("selectedDate", calendar.getTimeInMillis()));
        } else {
            calendar.add(Calendar.DAY_OF_MONTH, -1);
        }
        updateDate();
        listenToFirestoreChanges();

        btnPrevDay.setOnClickListener(v -> {
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            updateDate();
            listenToFirestoreChanges();
        });

        btnNextDay.setOnClickListener(v -> {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            if (!calendar.before(today)) {
                Intent intent = new Intent(pass_date.this, Today.class);
                intent.putExtra("selectedDate", calendar.getTimeInMillis());
                startActivity(intent);
                finish();
            } else {
                updateDate();
                listenToFirestoreChanges();
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

    private void hideEditDeleteButtons() {
        for (int i = 0; i < recyclerOverdue.getChildCount(); i++) {
            View itemView = recyclerOverdue.getChildAt(i);
            if (itemView != null) {
                ImageButton btnEdit = itemView.findViewById(R.id.btnEditTask);
                ImageButton btnDelete = itemView.findViewById(R.id.btnDeleteTask);
                ImageView label = itemView.findViewById(R.id.label);

                if (btnEdit != null) btnEdit.setVisibility(View.GONE);
                if (btnDelete != null) btnDelete.setVisibility(View.GONE);
                if (label != null) label.setVisibility(View.VISIBLE);
            }
        }
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
        db.collection("tasks")
                .document(documentId)
                .set(taskData)
                .addOnSuccessListener(aVoid -> Log.d("pass_date", "Task added successfully: " + documentId))
                .addOnFailureListener(e -> Log.e("pass_date", "Error adding task", e));
    }

    private void listenToFirestoreChanges() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String currentDate = sdf.format(calendar.getTime());

        db.collection("tasks")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e("pass_date", "Firestore listen failed", e);
                        return;
                    }

                    overdueTasks.clear();
                    doneTasks.clear();

                    if (snapshots != null && !snapshots.isEmpty()) {
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            String status = doc.getString("status");
                            String name = doc.getString("name");
                            String startTime = doc.getString("startTime");
                            String endTime = doc.getString("endTime");
                            String time = doc.getString("time");

                            if ("overdue".equals(status) && startTime != null && endTime != null) {
                                String taskDate = startTime.substring(0, 10);
                                if (taskDate.equals(currentDate)) {
                                    overdueTasks.add(new Task2(name, startTime, endTime));
                                }
                            } else if ("done".equals(status) && time != null) {
                                String taskDate = time.substring(0, 10);
                                if (taskDate.equals(currentDate)) {
                                    doneTasks.add(new Task(name, time));
                                }
                            }
                        }
                    }

                    runOnUiThread(() -> {
                        overdueAdapter.notifyDataSetChanged();
                        doneAdapter.notifyDataSetChanged();
                    });
                });
    }

    private void updateDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        tvDate.setText(sdf.format(calendar.getTime()));
    }

    private void checkAndTransferToToday(String startTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar taskCalendar = Calendar.getInstance();
            taskCalendar.setTime(sdf.parse(startTime.substring(0, 10)));
            Calendar todayCal = Calendar.getInstance();
            String todayDate = sdf.format(todayCal.getTime());

            if (startTime.substring(0, 10).equals(todayDate)) {
                Intent intent = new Intent(pass_date.this, Today.class);
                intent.putExtra("selectedDate", todayCal.getTimeInMillis());
                startActivity(intent);
                finish();
            }
        } catch (Exception e) {
            Log.e("pass_date", "Error checking startTime", e);
        }
    }

    private class SwipeToActionCallback extends ItemTouchHelper.SimpleCallback {
        private Taskadapter4 mAdapter;

        SwipeToActionCallback(Taskadapter4 adapter) {
            super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
            mAdapter = adapter;
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            // Reset the view position
            viewHolder.itemView.setTranslationX(0);
            mAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
        }

        @Override
        public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                float dX, float dY, int actionState, boolean isCurrentlyActive) {
            View itemView = viewHolder.itemView;
            ImageButton btnEdit = itemView.findViewById(R.id.btnEditTask);
            ImageButton btnDelete = itemView.findViewById(R.id.btnDeleteTask);
            ImageView label = itemView.findViewById(R.id.label);

            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                if (dX < 0) { // Swipe left
                    btnEdit.setVisibility(View.VISIBLE);
                    btnDelete.setVisibility(View.VISIBLE);
                    label.setVisibility(View.GONE);

                    btnEdit.setOnClickListener(v -> {
                        int position = viewHolder.getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            Task2 task = overdueTasks.get(position);
                            Intent intent = new Intent(pass_date.this, update_items.class);
                            intent.putExtra("taskName", task.getName());
                            intent.putExtra("startTime", task.getStartTime());
                            intent.putExtra("endTime", task.getEndTime());
                            intent.putExtra("originalTaskId", task.getName() + "_" + task.getStartTime());
                            updateTaskLauncher.launch(intent);
                            hideEditDeleteButtons();
                            recyclerOverdue.getAdapter().notifyItemChanged(position);
                        }
                    });

                    btnDelete.setOnClickListener(v -> {
                        int position = viewHolder.getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION && position < overdueTasks.size()) {
                            Task2 task = overdueTasks.get(position);
                            String taskId = task.getName() + "_" + task.getStartTime();

                            db.collection("tasks")
                                    .document(taskId)
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        runOnUiThread(() -> {
                                            if (position < overdueTasks.size()) {
                                                overdueTasks.remove(position);
                                                mAdapter.notifyItemRemoved(position);
                                                mAdapter.notifyItemRangeChanged(position, overdueTasks.size());
                                            }
                                            hideEditDeleteButtons();
                                        });
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("pass_date", "Error deleting task", e);
                                        runOnUiThread(() -> {
                                            mAdapter.notifyItemChanged(position);
                                            hideEditDeleteButtons();
                                        });
                                    });
                        }
                    });
                } else { // Swipe right or no swipe
                    btnEdit.setVisibility(View.GONE);
                    btnDelete.setVisibility(View.GONE);
                    label.setVisibility(View.VISIBLE);
                }
            }

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }
}