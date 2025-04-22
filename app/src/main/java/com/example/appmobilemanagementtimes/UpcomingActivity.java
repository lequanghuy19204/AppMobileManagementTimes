package com.example.appmobilemanagementtimes;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.text.ParseException;
import java.util.HashMap;

import android.widget.SearchView;

public class UpcomingActivity extends AppCompatActivity {
    private RecyclerView recyclerTodo;
    private RecyclerView recyclerDone;
    private RecyclerView calendarDaysRecyclerView;
    private Taskadapter2 todoAdapter;
    private Taskadapter3 doneAdapter;
    private CalendarDayAdapter calendarDayAdapter;
    private TextView dateHeader;
    private TextView tvTodoLabel;
    private ImageButton prevButton;
    private ImageButton nextButton;
    private Calendar currentCalendar;
    private FirebaseFirestore db;
    private String userId;
    private static final String TAG = "UpcomingActivity";
    private List<Task2> todoTasks = new ArrayList<>();
    private List<Task2> doneTasks = new ArrayList<>();
    private boolean isOverdue = false;
    private FloatingActionButton fabAdd;

    private RecyclerView searchResultsRecyclerView;
    private SearchResultAdapter searchResultAdapter;
    private List<Task2> searchResultTasks = new ArrayList<>();
    private androidx.cardview.widget.CardView searchResultsCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upcoming);

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
        
        // Khởi tạo Firestore
        db = FirebaseFirestore.getInstance();

        // Khởi tạo các view
        dateHeader = findViewById(R.id.dateHeader);
        prevButton = findViewById(R.id.prevButton);
        nextButton = findViewById(R.id.nextButton);
        calendarDaysRecyclerView = findViewById(R.id.calendarDaysRecyclerView);
        recyclerTodo = findViewById(R.id.recycler_todo);
        recyclerDone = findViewById(R.id.recycler_done);
        tvTodoLabel = findViewById(R.id.tv_todo_label);
        fabAdd = findViewById(R.id.fabAdd);

        // Thiết lập RecyclerView cho tasks
        recyclerTodo.setLayoutManager(new LinearLayoutManager(this));
        recyclerDone.setLayoutManager(new LinearLayoutManager(this));

        // Khởi tạo adapters
        todoAdapter = new Taskadapter2(todoTasks, isOverdue, 
            task -> {
                // Xử lý khi task được đánh dấu hoàn thành
                addTaskToFirestore(task, "done");
                loadTasksForSelectedDate();
            },
            task -> {
                // Xử lý khi task bị xóa
                deleteTasksByGroupId(task.getGroupId());
            }
        );
        
        doneAdapter = new Taskadapter3(doneTasks);
        recyclerTodo.setAdapter(todoAdapter);
        recyclerDone.setAdapter(doneAdapter);

        // Khởi tạo calendar
        currentCalendar = Calendar.getInstance();
        updateCalendarView();

        // Thiết lập sự kiện cho nút prev và next
        prevButton.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, -1);
            updateCalendarView();
        });

        nextButton.setOnClickListener(v -> {
            currentCalendar.add(Calendar.MONTH, 1);
            updateCalendarView();
        });

        // Thiết lập sự kiện cho nút thêm nhiệm vụ
        fabAdd.setOnClickListener(v -> {
            Intent addIntent = new Intent(UpcomingActivity.this, create_items.class);
            // Truyền userId sang trang create_items
            addIntent.putExtra("userId", userId);
            
            // Truyền ngày hiện tại đang chọn trong lịch
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String selectedDate = sdf.format(currentCalendar.getTime());
            addIntent.putExtra("selectedDate", selectedDate);
            
            startActivity(addIntent);
        });

        // Thiết lập bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.navigation_upcoming);
        
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                startActivity(new Intent(UpcomingActivity.this, Today.class));
                finish();
                return true;
            } else if (itemId == R.id.navigation_upcoming) {
                return true;
            } else if (itemId == R.id.navigation_pomo) {
                startActivity(new Intent(UpcomingActivity.this, PomodoroActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.navigation_statistic) {
                startActivity(new Intent(UpcomingActivity.this, StatisticActivity.class));
                finish();
                return true;
            }
            return false;
        });

        // Khởi tạo thêm view cho tìm kiếm
        searchResultsCard = findViewById(R.id.searchResultsCard);
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        // Khởi tạo adapter cho kết quả tìm kiếm
        searchResultAdapter = new SearchResultAdapter(searchResultTasks, task -> {
            // Khi người dùng nhấp vào kết quả tìm kiếm
            // Chuyển đến ngày của nhiệm vụ
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                Date taskDate = sdf.parse(task.getStartTime());
                if (taskDate != null) {
                    // Đặt calendar về ngày của nhiệm vụ
                    currentCalendar.setTime(taskDate);
                    // Cập nhật calendar view
                    updateCalendarView();
                    // Đặt ngày được chọn trong adapter
                    calendarDayAdapter.setSelectedDay(currentCalendar.get(Calendar.DAY_OF_MONTH));
                    // Tải nhiệm vụ cho ngày được chọn
                    loadTasksForSelectedDate();
                    // Ẩn kết quả tìm kiếm
                    searchResultsCard.setVisibility(View.GONE);
                }
            } catch (ParseException e) {
                Log.e(TAG, "Lỗi phân tích ngày: " + task.getStartTime(), e);
            }
        });
        searchResultsRecyclerView.setAdapter(searchResultAdapter);
        
        // Thiết lập sự kiện tìm kiếm
        SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchTasks(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    searchResultsCard.setVisibility(View.GONE);
                } else {
                    searchTasks(newText);
                }
                return true;
            }
        });
    }

    private void updateCalendarView() {
        // Cập nhật tiêu đề tháng
        SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM yyyy", Locale.getDefault());
        dateHeader.setText(dateFormat.format(currentCalendar.getTime()));

        // Lưu lại ngày đang được chọn
        int selectedDay = currentCalendar.get(Calendar.DAY_OF_MONTH);
        
        // Tạo danh sách ngày trong tháng
        List<Integer> days = new ArrayList<>();
        
        // Đặt calendar về ngày đầu tiên của tháng
        Calendar tempCalendar = (Calendar) currentCalendar.clone();
        tempCalendar.set(Calendar.DAY_OF_MONTH, 1);
        
        // Lấy thứ của ngày đầu tiên (0 = Chủ nhật, 1 = Thứ 2, ...)
        int firstDayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK);
        
        // Điều chỉnh để thứ 2 là ngày đầu tuần (2 = Thứ 2, 3 = Thứ 3, ..., 1 = Chủ nhật)
        if (firstDayOfWeek == Calendar.SUNDAY) {
            firstDayOfWeek = 7;
        } else {
            firstDayOfWeek = firstDayOfWeek - 1;
        }
        
        // Thêm các ô trống cho các ngày trước ngày đầu tiên của tháng
        for (int i = 1; i < firstDayOfWeek; i++) {
            days.add(0);
        }
        
        // Thêm các ngày trong tháng
        int daysInMonth = tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int i = 1; i <= daysInMonth; i++) {
            days.add(i);
        }
        
        // Thêm các ô trống để hoàn thành tuần cuối cùng
        int remainingCells = 42 - days.size(); // 6 hàng x 7 cột = 42 ô
        for (int i = 0; i < remainingCells; i++) {
            days.add(0);
        }
        
        // Thiết lập adapter
        calendarDayAdapter = new CalendarDayAdapter(this, days, currentCalendar.get(Calendar.MONTH), currentCalendar.get(Calendar.YEAR));
        calendarDayAdapter.setOnDayClickListener(day -> {
            currentCalendar.set(Calendar.DAY_OF_MONTH, day);
            updateDateHeader();
            loadTasksForSelectedDate();
            calendarDayAdapter.setSelectedDay(day);
        });
        
        // Đặt ngày được chọn là ngày đã lưu
        calendarDayAdapter.setSelectedDay(selectedDay);
        
        calendarDaysRecyclerView.setLayoutManager(new GridLayoutManager(this, 7));
        calendarDaysRecyclerView.setAdapter(calendarDayAdapter);
        
        // Lấy dữ liệu các ngày có nhiệm vụ
        fetchDaysWithTasks(days, currentCalendar.get(Calendar.MONTH), currentCalendar.get(Calendar.YEAR));
        
        updateDateHeader();
        loadTasksForSelectedDate();
    }
    
    private void fetchDaysWithTasks(List<Integer> days, int month, int year) {
        Set<Integer> daysWithTasks = new HashSet<>();
        
        if (userId == null || userId.isEmpty()) return;
        
        Calendar startOfMonth = Calendar.getInstance();
        startOfMonth.set(year, month, 1, 0, 0, 0);
        startOfMonth.set(Calendar.MILLISECOND, 0);
        
        Calendar endOfMonth = Calendar.getInstance();
        endOfMonth.set(year, month, startOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
        endOfMonth.set(Calendar.MILLISECOND, 999);
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String startDateStr = sdf.format(startOfMonth.getTime());
        String endDateStr = sdf.format(endOfMonth.getTime());
        
        Log.d(TAG, "Tìm nhiệm vụ từ " + startDateStr + " đến " + endDateStr);
        
        db.collection("tasks")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                    String startTime = document.getString("startTime");
                    if (startTime != null && !startTime.isEmpty()) {
                        try {
                            Date date = sdf.parse(startTime);
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(date);
                            
                            // Chỉ xử lý nhiệm vụ trong tháng được chọn
                            if (cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year) {
                                int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
                                daysWithTasks.add(dayOfMonth);
                                Log.d(TAG, "Nhiệm vụ tìm thấy cho ngày: " + dayOfMonth);
                            }
                        } catch (ParseException e) {
                            Log.e(TAG, "Lỗi phân tích ngày: " + startTime, e);
                        }
                    }
                }
                
                Log.d(TAG, "Tổng số ngày có nhiệm vụ: " + daysWithTasks.size());
                Log.d(TAG, "Các ngày có nhiệm vụ: " + daysWithTasks.toString());
                
                // Cập nhật adapter với các ngày có nhiệm vụ
                calendarDayAdapter.setDaysWithTasks(daysWithTasks);
            })
            .addOnFailureListener(e -> Log.e(TAG, "Lỗi khi tải nhiệm vụ", e));
    }
    
    private void loadTasksForSelectedDate() {
        // Lấy ngày được chọn hiện tại dưới dạng yyyy-MM-dd
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String selectedDate = sdf.format(currentCalendar.getTime());
        Log.d(TAG, "Đang tải nhiệm vụ cho ngày: " + selectedDate);
        
        // Kiểm tra xem ngày được chọn có phải là quá khứ không
        Calendar todayCal = Calendar.getInstance();
        todayCal.set(Calendar.HOUR_OF_DAY, 0);
        todayCal.set(Calendar.MINUTE, 0);
        todayCal.set(Calendar.SECOND, 0);
        todayCal.set(Calendar.MILLISECOND, 0);

        Calendar selectedCal = (Calendar) currentCalendar.clone();
        selectedCal.set(Calendar.HOUR_OF_DAY, 0);
        selectedCal.set(Calendar.MINUTE, 0);
        selectedCal.set(Calendar.SECOND, 0);
        selectedCal.set(Calendar.MILLISECOND, 0);

        if (selectedCal.before(todayCal)) {
            tvTodoLabel.setText("Overdue");
            isOverdue = true;
        } else {
            tvTodoLabel.setText("To Do");
            isOverdue = false;
        }
        
        // Cập nhật adapter state
        todoAdapter.setOverdue(isOverdue);
        
        // Lấy dữ liệu từ Firestore
        db.collection("tasks")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener(queryDocuments -> {
                todoTasks.clear();
                doneTasks.clear();
                
                for (DocumentSnapshot doc : queryDocuments.getDocuments()) {
                    String status = doc.getString("status");
                    String name = doc.getString("name");
                    String startTime = doc.getString("startTime");
                    String endTime = doc.getString("endTime");
                    String repeatMode = doc.getString("repeatMode");
                    String groupId = doc.getString("groupId");
                    String reminder = doc.getString("reminder");
                    String label = doc.getString("label");
                    
                    if (startTime != null) {
                        String taskDate = startTime.substring(0, 10);
                        if (taskDate.equals(selectedDate)) {
                            if ("overdue".equals(status)) {
                                todoTasks.add(new Task2(name, startTime, endTime, 
                                    repeatMode, groupId, reminder, label, userId));
                                Log.d(TAG, "Thêm nhiệm vụ: " + name + " cho " + taskDate);
                            } else if ("done".equals(status)) {
                                doneTasks.add(new Task2(name, startTime, null, 
                                    repeatMode, groupId, reminder, label, userId));
                                Log.d(TAG, "Thêm nhiệm vụ hoàn thành: " + name + " cho " + taskDate);
                            }
                        }
                    }
                }
                
                // Cập nhật adapters
                todoAdapter.updateTasks(todoTasks);
                doneAdapter.updateTasks(doneTasks);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Lỗi khi tải nhiệm vụ từ Firestore", e);
                Toast.makeText(UpcomingActivity.this, "Không thể tải nhiệm vụ", Toast.LENGTH_SHORT).show();
            });
    }
    
    private void addTaskToFirestore(Task2 task, String status) {
        java.util.Map<String, Object> taskData = new java.util.HashMap<>();
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
        Log.d(TAG, "Thêm nhiệm vụ vào Firestore: " + taskData.toString());
        db.collection("tasks")
            .document(documentId)
            .set(taskData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Nhiệm vụ thêm thành công: " + documentId);
                // Cập nhật lại danh sách nhiệm vụ
                loadTasksForSelectedDate();
            })
            .addOnFailureListener(e -> Log.e(TAG, "Lỗi khi thêm nhiệm vụ", e));
    }
    
    private void deleteTasksByGroupId(String groupId) {
        if (groupId == null || groupId.isEmpty()) {
            Log.e(TAG, "GroupId rỗng hoặc null, không thể xóa nhiệm vụ định kỳ");
            return;
        }

        Log.d(TAG, "Đang xóa nhiệm vụ với groupId: " + groupId);
        db.collection("tasks")
            .whereEqualTo("groupId", groupId)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener(querySnapshot -> {
                Log.d(TAG, "Tìm thấy " + querySnapshot.size() + " nhiệm vụ với groupId: " + groupId);
                if (querySnapshot.isEmpty()) {
                    Log.d(TAG, "Không tìm thấy nhiệm vụ với groupId: " + groupId);
                }
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    String docId = doc.getId();
                    db.collection("tasks").document(docId)
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Đã xóa nhiệm vụ: " + docId);
                            // Cập nhật lại danh sách nhiệm vụ
                            loadTasksForSelectedDate();
                        })
                        .addOnFailureListener(e -> Log.e(TAG, "Lỗi khi xóa nhiệm vụ: " + docId, e));
                }
            })
            .addOnFailureListener(e -> Log.e(TAG, "Lỗi khi truy vấn nhiệm vụ theo groupId: " + groupId, e));
    }

    private void updateDateHeader() {
        // Cập nhật tiêu đề tháng
        SimpleDateFormat dateFormat = new SimpleDateFormat("d MMMM yyyy", Locale.getDefault());
        dateHeader.setText(dateFormat.format(currentCalendar.getTime()));
    }

    private void updateTaskInFirestore(Task2 task) {
        if (task == null || task.getGroupId() == null) return;
        
        Map<String, Object> taskData = new HashMap<>();
        taskData.put("name", task.getName());
        taskData.put("startTime", task.getStartTime());
        taskData.put("endTime", task.getEndTime());
        taskData.put("status", "done");
        taskData.put("groupId", task.getGroupId());
        taskData.put("repeatMode", task.getRepeatMode());
        taskData.put("reminder", task.getReminder());
        taskData.put("label", task.getLabel());
        taskData.put("userId", userId);
        
        db.collection("tasks")
            .document(task.getGroupId())
            .set(taskData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Task updated successfully");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error updating task", e);
            });
    }

    // Thêm phương thức tìm kiếm nhiệm vụ
    private void searchTasks(String query) {
        if (query.isEmpty()) {
            searchResultsCard.setVisibility(View.GONE);
            return;
        }
        
        query = query.toLowerCase();
        final String finalQuery = query;
        
        db.collection("tasks")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                searchResultTasks.clear();
                
                for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                    String name = doc.getString("name");
                    if (name != null && name.toLowerCase().contains(finalQuery)) {
                        String startTime = doc.getString("startTime");
                        String endTime = doc.getString("endTime");
                        String repeatMode = doc.getString("repeatMode");
                        String groupId = doc.getString("groupId");
                        String reminder = doc.getString("reminder");
                        String label = doc.getString("label");
                        String status = doc.getString("status");
                        
                        Task2 task = new Task2(name, startTime, endTime, 
                            repeatMode, groupId, reminder, label, userId);
                        
                        searchResultTasks.add(task);
                    }
                }
                
                if (searchResultTasks.isEmpty()) {
                    // Không có kết quả tìm kiếm
                    searchResultsCard.setVisibility(View.GONE);
                } else {
                    // Có kết quả tìm kiếm
                    searchResultAdapter.updateTasks(searchResultTasks);
                    searchResultsCard.setVisibility(View.VISIBLE);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Lỗi khi tìm kiếm nhiệm vụ", e);
                Toast.makeText(UpcomingActivity.this, "Không thể tìm kiếm nhiệm vụ", Toast.LENGTH_SHORT).show();
            });
    }
} 