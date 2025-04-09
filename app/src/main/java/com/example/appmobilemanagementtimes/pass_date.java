package com.example.appmobilemanagementtimes;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;
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
    private ActivityResultLauncher<Intent> updateTaskLauncher; // Thêm launcher cho chỉnh sửa

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.passdate);

        db = FirebaseFirestore.getInstance();

        // Khởi tạo ActivityResultLauncher để nhận dữ liệu từ update_items
        updateTaskLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String taskName = result.getData().getStringExtra("taskName");
                        String startTime = result.getData().getStringExtra("startTime");
                        String endTime = result.getData().getStringExtra("endTime");
                        String originalTaskId = result.getData().getStringExtra("originalTaskId");

                        if (taskName != null && startTime != null && endTime != null && originalTaskId != null) {
                            // Xóa task cũ và thêm task mới với dữ liệu đã chỉnh sửa
                            db.collection("tasks").document(originalTaskId).delete();
                            Task2 updatedTask = new Task2(taskName, startTime, endTime);
                            Map<String, Object> taskData = new HashMap<>();
                            taskData.put("name", updatedTask.getName());
                            taskData.put("startTime", updatedTask.getStartTime());
                            taskData.put("endTime", updatedTask.getEndTime());
                            taskData.put("status", "overdue");
                            db.collection("tasks")
                                    .document(taskName + "_" + startTime)
                                    .set(taskData);
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
            db.collection("tasks").document(task.getName() + "_" + task.getStartTime())
                    .delete();
            addTaskToFirestore(new Task(task.getName(), task.getStartTime()), "done");
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

        Calendar today = Calendar.getInstance();
        calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        updateDate();
        listenToFirestoreChanges();

        btnPrevDay.setOnClickListener(v -> {
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            updateDate();
            filterTasksByDate();
        });

        btnNextDay.setOnClickListener(v -> {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            if (!calendar.getTime().before(today.getTime())) {
                Intent intent = new Intent(pass_date.this, Today.class);
                startActivity(intent);
                finish();
            } else {
                updateDate();
                filterTasksByDate();
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
            ImageButton btnEdit = itemView.findViewById(R.id.btnEditTask);
            ImageButton btnDelete = itemView.findViewById(R.id.btnDeleteTask);
            ImageView label = itemView.findViewById(R.id.label);

            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
            label.setVisibility(View.VISIBLE);
        }
    }

    private void addTaskToFirestore(Task task, String status) {
        Map<String, Object> taskData = new HashMap<>();
        taskData.put("name", task.getName());
        taskData.put("time", task.getTime());
        taskData.put("status", status);

        db.collection("tasks")
                .document(task.getName() + "_" + task.getTime())
                .set(taskData);
    }

    private void listenToFirestoreChanges() {
        db.collection("tasks")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;

                    overdueTasks.clear();
                    doneTasks.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String status = doc.getString("status");
                        String name = doc.getString("name");
                        if ("overdue".equals(status)) {
                            String startTime = doc.getString("startTime");
                            String endTime = doc.getString("endTime");
                            overdueTasks.add(new Task2(name, startTime, endTime));
                        } else if ("done".equals(status)) {
                            String time = doc.getString("time");
                            doneTasks.add(new Task(name, time));
                        }
                    }
                    filterTasksByDate();
                });
    }

    private void updateDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        tvDate.setText(sdf.format(calendar.getTime()));
    }

    private void filterTasksByDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String currentDate = sdf.format(calendar.getTime());
        Calendar today = Calendar.getInstance();
        String todayDate = sdf.format(today.getTime());

        List<Task2> filteredOverdueTasks = new ArrayList<>();
        for (Task2 task : overdueTasks) {
            String taskDate = task.getStartTime().substring(0, 10);
            if (taskDate.equals(currentDate) && taskDate.compareTo(todayDate) < 0) {
                filteredOverdueTasks.add(task);
            }
        }
        overdueAdapter.updateTasks(filteredOverdueTasks);

        List<Task> filteredDoneTasks = new ArrayList<>();
        for (Task task : doneTasks) {
            String taskDate = task.getTime().substring(0, 10);
            if (taskDate.equals(currentDate) && taskDate.compareTo(todayDate) < 0) {
                filteredDoneTasks.add(task);
            }
        }
        doneAdapter.updateTasks(filteredDoneTasks);
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
            mAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
        }

        @Override
        public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                float dX, float dY, int actionState, boolean isCurrentlyActive) {
            View itemView = viewHolder.itemView;
            ImageView label = itemView.findViewById(R.id.label);
            ImageButton btnEdit = itemView.findViewById(R.id.btnEditTask);
            ImageButton btnDelete = itemView.findViewById(R.id.btnDeleteTask);

            if (dX < 0) { // Vuốt sang trái
                btnEdit.setVisibility(View.VISIBLE);
                btnDelete.setVisibility(View.VISIBLE);
                label.setVisibility(View.GONE);

                // Thêm sự kiện bấm nút chỉnh sửa
                btnEdit.setOnClickListener(v -> {
                    int position = viewHolder.getAdapterPosition();
                    Task2 task = overdueTasks.get(position);
                    Intent intent = new Intent(pass_date.this, update_items.class);
                    intent.putExtra("taskName", task.getName());
                    intent.putExtra("startTime", task.getStartTime());
                    intent.putExtra("endTime", task.getEndTime());
                    intent.putExtra("originalTaskId", task.getName() + "_" + task.getStartTime());
                    updateTaskLauncher.launch(intent);
                });

                itemView.setTranslationX(0);
            } else { // Vuốt sang phải hoặc trở về trạng thái ban đầu
                btnEdit.setVisibility(View.GONE);
                btnDelete.setVisibility(View.GONE);
                label.setVisibility(View.VISIBLE);
            }
            itemView.setTranslationX(0);

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }

    public static class SpaceItemDecoration extends RecyclerView.ItemDecoration {
        private final int space;

        public SpaceItemDecoration(int space) {
            this.space = space;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            outRect.bottom = space;
        }
    }
}