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

/**
 * Today activity manages the display and interaction of tasks for the current day.
 * It integrates with Firestore for task persistence and supports adding, completing, and deleting tasks.
 */
public class Today extends AppCompatActivity {
    // Launcher for starting the create_items activity and receiving task creation results
    private ActivityResultLauncher<Intent> createTaskLauncher;
    // RecyclerViews for displaying "to-do" (todayTasks) and "done" tasks
    private RecyclerView recyclerToday, recyclerDone;
    // Adapters for managing task data in RecyclerViews
    private Taskadapter2 todayAdapter;
    private Taskadapter3 doneAdapter;
    // Lists to store tasks for the current day
    private List<Task2> todayTasks = new ArrayList<>(); // "To-do" tasks
    private List<Task> doneTasks = new ArrayList<>();   // "Done" tasks
    // UI elements for date navigation
    private TextView tvDate;          // Displays the current date
    private ImageButton btnPrevDay;   // Button to go to the previous day
    private ImageButton btnNextDay;   // Button to go to the next day
    // Calendar instance to track the current date being viewed
    private Calendar calendar;
    // Firestore instance for database operations
    private FirebaseFirestore db;
    // Tag for logging
    private static final String TAG = "Today";

    /**
     * Called when the activity is created. Sets up UI, adapters, listeners, and Firestore integration.
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.today); // Load the layout for this activity

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Set up the launcher to handle results from the task creation activity
        createTaskLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        // Extract task details from the result intent
                        String taskName = result.getData().getStringExtra("taskName");
                        String startTime = result.getData().getStringExtra("startTime");
                        String endTime = result.getData().getStringExtra("endTime");
                        String repeatMode = result.getData().getStringExtra("repeatMode");
                        String reminderTime = result.getData().getStringExtra("reminderTime");

                        // Validate required fields and add the task to Firestore
                        if (taskName != null && startTime != null && endTime != null) {
                            Task2 newTask = new Task2(taskName, startTime, endTime);
                            addTaskToFirestore(newTask, "overdue", repeatMode, reminderTime);
                        }
                    }
                });

        // Initialize UI components from the layout
        recyclerToday = findViewById(R.id.recycler_todo);   // RecyclerView for "to-do" tasks
        recyclerDone = findViewById(R.id.recyclerView);     // RecyclerView for "done" tasks
        tvDate = findViewById(R.id.tv_date);                // TextView for displaying the date
        btnPrevDay = findViewById(R.id.btn_prev_day);       // Button to navigate to previous day
        btnNextDay = findViewById(R.id.btn_next_day);       // Button to navigate to next day

        // Set layout managers for RecyclerViews
        recyclerToday.setLayoutManager(new LinearLayoutManager(this));
        recyclerDone.setLayoutManager(new LinearLayoutManager(this));

        // Initialize adapters with callbacks for task completion and deletion
        todayAdapter = new Taskadapter2(todayTasks,
                task -> {
                    // Callback for task completion: remove from "overdue" and add to "done" in Firestore
                    db.collection("tasks").document(task.getName() + "_" + task.getStartTime())
                            .delete();
                    addTaskToFirestore(new Task(task.getName(), task.getStartTime()), "done", null, null);
                },
                task -> {
                    // Callback for task deletion: remove from Firestore when delete button is clicked
                    db.collection("tasks").document(task.getName() + "_" + task.getStartTime())
                            .delete()
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Task deleted: " + task.getName()))
                            .addOnFailureListener(e -> Log.e(TAG, "Error deleting task", e));
                });
        doneAdapter = new Taskadapter3(doneTasks); // Adapter for "done" tasks (assumed to exist)
        recyclerToday.setAdapter(todayAdapter);    // Attach adapter to "to-do" RecyclerView
        recyclerDone.setAdapter(doneAdapter);      // Attach adapter to "done" RecyclerView

        // Set up swipe functionality for "to-do" tasks to show edit/delete buttons
        ItemTouchHelper todayTouchHelper = new ItemTouchHelper(new SwipeToActionCallback(todayAdapter));
        todayTouchHelper.attachToRecyclerView(recyclerToday);

        // Set up the "Add Task" button to launch the task creation activity
        ImageButton imageButton = findViewById(R.id.btn_add);
        imageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Today.this, create_items.class);
            createTaskLauncher.launch(intent);
        });

        // Initialize calendar to the current date and update the UI
        calendar = Calendar.getInstance();
        updateDate();
        listenToFirestoreChanges(); // Start listening for Firestore updates

        // Button to navigate to the next day
        btnNextDay.setOnClickListener(v -> {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            updateDate();
            filterTasksByDate();
        });

        // Button to navigate to the previous day
        btnPrevDay.setOnClickListener(v -> {
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            Calendar todayCal = Calendar.getInstance();
            Calendar yesterday = (Calendar) todayCal.clone();
            yesterday.add(Calendar.DAY_OF_MONTH, -1);
            if (!calendar.getTime().after(yesterday.getTime())) {
                // If navigating to a date before yesterday, switch to the pass_date activity
                Intent intent = new Intent(Today.this, pass_date.class);
                startActivity(intent);
                finish();
            } else {
                updateDate();
                filterTasksByDate();
            }
        });

        // Bottom navigation bar listener (placeholders for navigation actions)
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

    /**
     * Adds a task to Firestore with the specified status, repeat mode, and reminder time.
     * @param task The task object (Task2 or Task) to add
     * @param status The status of the task ("overdue" or "done")
     * @param repeatMode The repeat mode ("Không bao giờ", "Hàng ngày", "Hàng tuần")
     * @param reminderTime The reminder time setting
     */
    private void addTaskToFirestore(Object task, String status, String repeatMode, String reminderTime) {
        Map<String, Object> taskData = new HashMap<>();
        if (task instanceof Task2) {
            Task2 t = (Task2) task;
            taskData.put("name", t.getName());
            taskData.put("startTime", t.getStartTime());
            taskData.put("endTime", t.getEndTime());
            taskData.put("repeatMode", repeatMode != null ? repeatMode : "Không bao giờ");
            taskData.put("reminderTime", reminderTime != null ? reminderTime : "Không nhắc nhở");
        } else if (task instanceof Task) {
            Task t = (Task) task;
            taskData.put("name", t.getName());
            taskData.put("time", t.getTime());
        }
        taskData.put("status", status);

        // Use a composite key (name + time) to uniquely identify the task in Firestore
        String documentId = taskData.get("name") + "_" + (taskData.get("startTime") != null ? taskData.get("startTime") : taskData.get("time"));
        db.collection("tasks")
                .document(documentId)
                .set(taskData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Task added to Firestore: " + documentId))
                .addOnFailureListener(e -> Log.e(TAG, "Error adding task: " + documentId, e));
    }

    /**
     * Listens for real-time changes in the Firestore "tasks" collection and updates the task lists.
     */
    private void listenToFirestoreChanges() {
        db.collection("tasks")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Firestore listen failed", e);
                        return;
                    }

                    // Clear current task lists before repopulating
                    todayTasks.clear();
                    doneTasks.clear();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    String currentDate = sdf.format(calendar.getTime());
                    Calendar currentCal = (Calendar) calendar.clone();

                    // Process each document in the Firestore snapshot
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String status = doc.getString("status");
                        String name = doc.getString("name");

                        if ("overdue".equals(status)) {
                            String startTime = doc.getString("startTime");
                            String endTime = doc.getString("endTime");
                            String repeatMode = doc.getString("repeatMode");

                            // Validate required fields
                            if (startTime == null || endTime == null || repeatMode == null) {
                                Log.e(TAG, "Missing data for task: " + name);
                                continue;
                            }

                            try {
                                Calendar taskCal = Calendar.getInstance();
                                taskCal.setTime(sdf.parse(startTime.substring(0, 10)));
                                long diffInMillis = currentCal.getTimeInMillis() - taskCal.getTimeInMillis();
                                long diffInDays = diffInMillis / (1000 * 60 * 60 * 24);

                                // Handle non-repeating tasks
                                if (repeatMode.equals("Không bao giờ")) {
                                    if (startTime.substring(0, 10).equals(currentDate)) {
                                        todayTasks.add(new Task2(name, startTime, endTime));
                                        Log.d(TAG, "Added non-repeating task: " + name + " for " + currentDate);
                                    }
                                }
                                // Handle daily repeating tasks
                                else if (repeatMode.equals("Hàng ngày")) {
                                    if (diffInDays >= 0) {
                                        String adjustedStartTime = adjustTimeForDate(startTime, currentDate);
                                        String adjustedEndTime = adjustTimeForDate(endTime, currentDate);
                                        todayTasks.add(new Task2(name, adjustedStartTime, adjustedEndTime));
                                        Log.d(TAG, "Added daily task: " + name + " for " + currentDate);
                                    }
                                }
                                // Handle weekly repeating tasks
                                else if (repeatMode.equals("Hàng tuần")) {
                                    if (diffInDays >= 0 && diffInDays % 7 == 0) {
                                        String adjustedStartTime = adjustTimeForDate(startTime, currentDate);
                                        String adjustedEndTime = adjustTimeForDate(endTime, currentDate);
                                        todayTasks.add(new Task2(name, adjustedStartTime, adjustedEndTime));
                                        Log.d(TAG, "Added weekly task: " + name + " for " + currentDate);
                                    }
                                }
                            } catch (Exception ex) {
                                Log.e(TAG, "Error parsing task date: " + name, ex);
                            }
                        }
                        // Handle completed tasks
                        else if ("done".equals(status)) {
                            String time = doc.getString("time");
                            if (time != null && time.substring(0, 10).equals(currentDate)) {
                                doneTasks.add(new Task(name, time));
                                Log.d(TAG, "Added done task: " + name + " for " + currentDate);
                            }
                        }
                    }
                    // Update adapters with the new task lists
                    todayAdapter.updateTasks(todayTasks);
                    doneAdapter.updateTasks(doneTasks);
                    Log.d(TAG, "Updated tasks - Today: " + todayTasks.size() + ", Done: " + doneTasks.size());
                });
    }

    /**
     * Adjusts the time portion of a task to match the current date while preserving the original time.
     * @param originalTime The original time string (e.g., "2025-04-08 09:00")
     * @param newDate The new date string (e.g., "2025-04-09")
     * @return A new time string with the date updated (e.g., "2025-04-09 09:00")
     */
    private String adjustTimeForDate(String originalTime, String newDate) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(originalTime));
            String timePart = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.getTime());
            return newDate + " " + timePart;
        } catch (Exception e) {
            Log.e(TAG, "Error adjusting time: " + originalTime, e);
            return originalTime; // Fallback to original time if parsing fails
        }
    }

    /**
     * Updates the date displayed in the TextView based on the current calendar state.
     */
    private void updateDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        tvDate.setText(sdf.format(calendar.getTime()));
    }

    /**
     * Filters and updates the task lists in the adapters based on the current date.
     * In this implementation, it simply refreshes the adapters with the full lists since
     * filtering is already handled in listenToFirestoreChanges().
     */
    private void filterTasksByDate() {
        todayAdapter.updateTasks(todayTasks);
        doneAdapter.updateTasks(doneTasks);
    }

    /**
     * Custom callback for handling swipe gestures on "to-do" tasks in the RecyclerView.
     * Shows edit and delete buttons on swipe, but deletion only occurs via button click.
     */
    private class SwipeToActionCallback extends ItemTouchHelper.SimpleCallback {
        private Taskadapter2 mAdapter;

        /**
         * Constructor for the swipe callback.
         * @param adapter The Taskadapter2 instance managing the RecyclerView data
         */
        SwipeToActionCallback(Taskadapter2 adapter) {
            super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT); // Enable left and right swipes
            mAdapter = adapter;
        }

        /**
         * Not implemented as moving items is not supported.
         */
        @Override
        public boolean onMove(RecyclerView RecyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return false;
        }

        /**
         * Handles the swipe action. Resets the item view to show buttons without deleting.
         * @param viewHolder The ViewHolder of the swiped item
         * @param direction The direction of the swipe (LEFT or RIGHT)
         */
        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            // Reset the item view to its original state after swipe
            mAdapter.notifyItemChanged(viewHolder.getAdapterPosition());
        }

        /**
         * Customizes the appearance of the swipe action.
         * Shows edit and delete buttons when swiping left, hides them otherwise.
         */
        @Override
        public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                float dX, float dY, int actionState, boolean isCurrentlyActive) {
            View itemView = viewHolder.itemView;
            ImageButton btnEdit = itemView.findViewById(R.id.btnEditTask);
            ImageButton btnDelete = itemView.findViewById(R.id.btnDeleteTask);

            if (dX < 0) { // Swiping left
                btnEdit.setVisibility(View.VISIBLE);  // Show edit button
                btnDelete.setVisibility(View.VISIBLE); // Show delete button
            } else { // Swiping right or not swiping
                btnEdit.setVisibility(View.GONE);     // Hide edit button
                btnDelete.setVisibility(View.GONE);    // Hide delete button
            }
            itemView.setTranslationX(0); // Prevent item from visually swiping

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }
}