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

public class Taskadapter4 extends RecyclerView.Adapter<Taskadapter4.TaskViewHolder> {
    private List<Task2> taskList; // Dùng Task2 để hiển thị startTime và endTime
    private Consumer<Task2> onTaskCompleted; // Callback để chuyển task sang Done

    public Taskadapter4(List<Task2> taskList, Consumer<Task2> onTaskCompleted) {
        this.taskList = taskList;
        this.onTaskCompleted = onTaskCompleted;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task_overdue, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task2 task = taskList.get(position);
        holder.taskName.setText(task.getName());
        String startTime = task.getStartTime().substring(11); // "HH:mm"
        String endTime = task.getEndTime().substring(11);     // "HH:mm"
        holder.taskTime.setText(startTime + " - " + endTime);

        // Reset trạng thái checkbox
        holder.checkbox.setOnCheckedChangeListener(null);
        holder.checkbox.setChecked(false);

        // Xử lý checkbox để chuyển task sang Done
        holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && onTaskCompleted != null) {
                holder.checkbox.setChecked(true); // Hiển thị dấu tích ngay lập tức
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        onTaskCompleted.accept(task);
                    }
                }, 300); // Delay 300ms để thấy hiệu ứng
            }
        });

        // Xử lý nút xóa
        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                taskList.remove(pos);
                notifyItemRemoved(pos);
            }
        });

        // Hiển thị label (pause3)

    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    // Cập nhật danh sách task
    public void updateTasks(List<Task2> newTasks) {
        this.taskList = newTasks;
        notifyDataSetChanged();
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