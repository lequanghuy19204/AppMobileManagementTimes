package com.example.appmobilemanagementtimes;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class Taskadapter2 extends RecyclerView.Adapter<Taskadapter2.TaskViewHolder> {
    private List<Task2> taskList;
    private boolean isOverdue;
    private Consumer<Task2> onTaskCompleted;
    private Consumer<Task2> onTaskDeleted;
    private static final String TAG = "Taskadapter2";

    private static final int VIEW_TYPE_TODO = 0;
    private static final int VIEW_TYPE_OVERDUE = 1;

    public Taskadapter2(List<Task2> taskList, boolean isOverdue, Consumer<Task2> onTaskCompleted, Consumer<Task2> onTaskDeleted) {
        this.taskList = taskList;
        this.isOverdue = isOverdue;
        this.onTaskCompleted = onTaskCompleted;
        this.onTaskDeleted = onTaskDeleted;
    }

    public void setOverdue(boolean isOverdue) {
        this.isOverdue = isOverdue;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return isOverdue ? VIEW_TYPE_OVERDUE : VIEW_TYPE_TODO;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = (viewType == VIEW_TYPE_OVERDUE) ? R.layout.item_task_overdue : R.layout.item_task;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task2 task = taskList.get(position);
        holder.taskName.setText(task.getName());

        // Định dạng thời gian
        SimpleDateFormat storageFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.US);
        String timeText = "";

        try {
            if (task.getStartTime() != null) {
                timeText = timeFormat.format(storageFormat.parse(task.getStartTime()))
                        .replace("AM", "SA").replace("PM", "CH");
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error parsing time for task: " + task.getName(), e);
            timeText = "";
        }

        holder.taskTime.setText(timeText);

        // Xử lý hiển thị nhãn
        if (task.getLabel() != null && !task.getLabel().isEmpty() && !"null".equals(task.getLabel())) {
            holder.label.setVisibility(View.VISIBLE);
            // Set label icon
            String label = task.getLabel();
            if (label != null) {
                switch (label) {
                    case "label1":
                        holder.label.setImageResource(R.drawable.pause1);
                        break;
                    case "label2":
                        holder.label.setImageResource(R.drawable.pause2);
                        break;
                    case "label3":
                        holder.label.setImageResource(R.drawable.pause3);
                        break;
                    case "label4":
                        holder.label.setImageResource(R.drawable.pause4);
                        break;
                    case "label5":
                        holder.label.setImageResource(R.drawable.pause5);
                        break;
                    case "label6":
                        holder.label.setImageResource(R.drawable.global);
                        break;
                    default:
                        holder.label.setImageResource(R.drawable.pause1); // Default
                }
            } else {
                holder.label.setImageResource(R.drawable.pause1); // Default
            }
        } else {
            holder.label.setVisibility(View.GONE);
        }

        // Thiết lập click listener cho item để hiển thị/ẩn các nút
        holder.itemView.setOnClickListener(v -> {
            // Toggle hiển thị nút edit và delete
            boolean isVisible = holder.btnEdit.getVisibility() == View.VISIBLE;
            int newVisibility = isVisible ? View.GONE : View.VISIBLE;
            holder.btnEdit.setVisibility(newVisibility);
            holder.btnDelete.setVisibility(newVisibility);
        });

        // Xử lý sự kiện khi checkbox được chọn
        holder.checkbox.setOnCheckedChangeListener(null);
        holder.checkbox.setChecked(false);
        holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && onTaskCompleted != null) {
                holder.checkbox.setChecked(true);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        onTaskCompleted.accept(task);
                        taskList.remove(pos);
                        notifyItemRemoved(pos);
                    }
                }, 300);
            }
        });

        // Xử lý sự kiện khi nhấn nút edit
        holder.btnEdit.setOnClickListener(v -> {
            if (position < taskList.size()) {
                Task2 taskToEdit = taskList.get(position);
                Context context = holder.itemView.getContext();
                Intent intent = new Intent(context, update_items.class);
                
                // Truyền dữ liệu của task qua để chỉnh sửa
                intent.putExtra("taskName", taskToEdit.getName());
                intent.putExtra("startTime", taskToEdit.getStartTime());
                intent.putExtra("endTime", taskToEdit.getEndTime());
                intent.putExtra("repeatMode", taskToEdit.getRepeatMode());
                intent.putExtra("reminder", taskToEdit.getReminder());
                intent.putExtra("groupId", taskToEdit.getGroupId());
                intent.putExtra("label", taskToEdit.getLabel());
                intent.putExtra("userId", taskToEdit.getUserId());
                
                // Thêm extra để xác định trạng thái hiện tại
                intent.putExtra("status", isOverdue ? "overdue" : "overdue");
                
                context.startActivity(intent);
            }
        });

        // Xử lý sự kiện khi nhấn nút delete
        holder.btnDelete.setOnClickListener(v -> {
            if (position < taskList.size() && onTaskDeleted != null) {
                onTaskDeleted.accept(taskList.get(position));
                // Hide buttons after deletion
                holder.btnEdit.setVisibility(View.GONE);
                holder.btnDelete.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public void updateTasks(List<Task2> newTasks) {
        this.taskList = newTasks;
        notifyDataSetChanged();
    }

    public List<Task2> getTaskList() {
        return taskList;
    }

    private String getRepeatDisplay(String repeatMode) {
        if (repeatMode == null) return "";
        switch (repeatMode) {
            case "every_day":
                return "Mỗi ngày";
            case "every_week":
                return "Mỗi tuần";
            case "every_2_weeks":
                return "Mỗi 2 tuần";
            case "every_3_weeks":
                return "Mỗi 3 tuần";
            case "every_month":
                return "Mỗi tháng";
            case "every_year":
                return "Mỗi năm";
            default:
                return "";
        }
    }

    private String getReminderDisplay(String reminder) {
        if (reminder == null) return "";
        switch (reminder) {
            case "1m":
                return "1 phút trước";
            case "5m":
                return "5 phút trước";
            case "15m":
                return "15 phút trước";
            case "30m":
                return "30 phút trước";
            case "1h":
                return "1 giờ trước";
            case "1d":
                return "1 ngày trước";
            default:
                return "";
        }
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView taskName, taskTime;
        CheckBox checkbox;
        ImageButton btnEdit, btnDelete;
        ImageView label;

        public TaskViewHolder(View itemView) {
            super(itemView);
            taskName = itemView.findViewById(R.id.task_name);
            taskTime = itemView.findViewById(R.id.task_time);
            checkbox = itemView.findViewById(R.id.checkbox_task);
            btnEdit = itemView.findViewById(R.id.btnEditTask);
            btnDelete = itemView.findViewById(R.id.btnDeleteTask);
            label = itemView.findViewById(R.id.label);
        }
    }
}