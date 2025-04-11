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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Today extends AppCompatActivity {
    private LinearLayout rootLayout;
    private ScrollView taskScrollView;
    private ActivityResultLauncher<Intent> createTaskLauncher;
    private ActivityResultLauncher<Intent> updateTaskLauncher;
    private RecyclerView recyclerToday, recyclerDone;
    private Taskadapter2 todayAdapter;
    private Taskadapter3 doneAdapter;
    private List<Task2> todayTasks = new ArrayList<>();
    private List<Task2> doneTasks = new ArrayList<>();
    private TextView tvToday, tvDate, tvTodoLabel;
    private ImageButton btnPrevDay, btnNextDay, btnAdd;
    private Calendar calendar;
    private FirebaseFirestore db;
    private DrawerLayout drawerLayout;
    private static final String TAG = "Today";
    private boolean isOverdue = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.today);
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        navigationView.setNavigationItemSelectedListener(item -> {
            navigationView.getMenu().setGroupCheckable(0, true, true);
            item.setChecked(true);
            int itemId = item.getItemId();
            if (itemId == R.id.nav_logout) {
                Intent intent = new Intent(Today.this, LoginActivity.class);
                startActivity(intent);
                finish();
                // Handle logout
            } else if (itemId == R.id.nav_language) {
                // Handle language change
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
                            // Xóa task cũ khỏi Firestore
                            db.collection("tasks").document(originalTaskId).delete();

                            // Tạo task mới
                            Task2 updatedTask = new Task2(taskName, startTime, endTime);
                            addTaskToFirestore(updatedTask, "overdue");

                            // Kiểm tra xem task mới có thuộc ngày hiện tại không
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                            String currentDate = sdf.format(calendar.getTime());
                            String taskDate = startTime.substring(0, 10);

                            // Nếu task không thuộc ngày hiện tại, xóa khỏi todayTasks
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

        recyclerToday.setLayoutManager(new LinearLayoutManager(this));
        recyclerDone.setLayoutManager(new LinearLayoutManager(this));

        todayAdapter = new Taskadapter2(todayTasks, isOverdue,
                task -> {
                    db.collection("tasks").document(task.getName() + "_" + task.getStartTime()).delete();
                    Task2 doneTask = new Task2(task.getName(), task.getStartTime(), null);
                    addTaskToFirestore(doneTask, "done");
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

        btnAdd.setOnClickListener(v -> {
            Intent intent = new Intent(Today.this, create_items.class);
            createTaskLauncher.launch(intent);
        });

        calendar = Calendar.getInstance();
        if (getIntent().hasExtra("selectedDate")) {
            calendar.setTimeInMillis(getIntent().getLongExtra("selectedDate", calendar.getTimeInMillis()));
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
            if (itemId == R.id.navigation_home) return true;
            else if (itemId == R.id.navigation_upcoming) return true;
            else if (itemId == R.id.navigation_pomo) return true;
            else if (itemId == R.id.navigation_statistic) return true;
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

        String documentId = task.getName() + "_" + task.getStartTime();
        Log.d(TAG, "Adding task to Firestore: " + taskData.toString());
        db.collection("tasks")
                .document(documentId)
                .set(taskData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Task added successfully: " + documentId))
                .addOnFailureListener(e -> Log.e(TAG, "Error adding task", e));
    }

    private void hideSwipeActions() {
        for (int i = 0; i < recyclerToday.getChildCount(); i++) {
            View itemView = recyclerToday.getChildAt(i);
            ImageView label = itemView.findViewById(R.id.label);
            ImageButton btnEdit = itemView.findViewById(R.id.btnEditTask);
            ImageButton btnDelete = itemView.findViewById(R.id.btnDeleteTask);
            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
            label.setVisibility(View.VISIBLE);
        }
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

                            if ("overdue".equals(status) && startTime != null) {
                                String taskDate = startTime.substring(0, 10);
                                if (taskDate.equals(currentDate)) {
                                    todayTasks.add(new Task2(name, startTime, endTime));
                                    Log.d(TAG, "Added task: " + name + " for " + taskDate);
                                }
                            } else if ("done".equals(status) && startTime != null) {
                                String taskDate = startTime.substring(0, 10);
                                if (taskDate.equals(currentDate)) {
                                    doneTasks.add(new Task2(name, startTime, null));
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
        }

        @Override
        public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                float dX, float dY, int actionState, boolean isCurrentlyActive) {
            View itemView = viewHolder.itemView;
            ImageView label = itemView.findViewById(R.id.label);
            ImageButton btnEdit = itemView.findViewById(R.id.btnEditTask);
            ImageButton btnDelete = itemView.findViewById(R.id.btnDeleteTask);

            if (dX < 0) {
                label.setVisibility(View.GONE);
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

                btnDelete.setOnClickListener(v -> {
                    int position = viewHolder.getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        Task2 task = todayTasks.get(position);
                        String taskId = task.getName() + "_" + task.getStartTime();
                        db.collection("tasks")
                                .document(taskId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Task deleted from Firestore: " + taskId);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error deleting task from Firestore: " + taskId, e);
                                });
                    }
                });
            } else {
                label.setVisibility(View.VISIBLE);
                btnEdit.setVisibility(View.GONE);
                btnDelete.setVisibility(View.GONE);
            }
            itemView.setTranslationX(0);
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }
}