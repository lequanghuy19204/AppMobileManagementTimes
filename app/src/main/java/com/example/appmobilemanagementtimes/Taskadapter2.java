package com.example.appmobilemanagementtimes;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.function.Consumer;

public class Taskadapter2 extends RecyclerView.Adapter<Taskadapter2.TaskViewHolder> {
    private List<Task2> taskList;
    private boolean isOverdue;
    private Consumer<Task2> onTaskCompleted;
    private Consumer<Task2> onTaskDeleted;

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
        notifyDataSetChanged(); // Làm mới toàn bộ giao diện khi trạng thái thay đổi
    }

    @Override
    public int getItemViewType(int position) {
        // Trả về loại view dựa trên trạng thái isOverdue
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
        String startTime = task.getStartTime().substring(11);
        String endTime = task.getEndTime() != null ? task.getEndTime().substring(11) : "";
        holder.taskTime.setText(startTime + (endTime.isEmpty() ? "" : " - " + endTime));

        holder.checkbox.setOnCheckedChangeListener(null);
        holder.checkbox.setChecked(false);

        holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && onTaskCompleted != null) {
                holder.checkbox.setChecked(true);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        onTaskCompleted.accept(task);
                    }
                }, 300);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                Task2 taskToDelete = taskList.get(pos);
                taskList.remove(pos);
                notifyItemRemoved(pos);
                if (onTaskDeleted != null) {
                    onTaskDeleted.accept(taskToDelete);
                }
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