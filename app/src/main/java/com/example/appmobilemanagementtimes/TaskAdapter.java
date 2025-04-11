package com.example.appmobilemanagementtimes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private List<Task> taskList;

    public TaskAdapter(List<Task> taskList) {
        this.taskList = taskList;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.task_item, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.titleTextView.setText(task.getTitle());
        holder.timeTextView.setText(task.getTimeRange());
        
        // Thêm sự kiện click cho mainTaskLayout
        holder.mainTaskLayout.setOnClickListener(v -> {
            // Toggle hiển thị của taskActionsLayout
            boolean isVisible = holder.taskActionsLayout.getVisibility() == View.VISIBLE;
            holder.taskActionsLayout.setVisibility(isVisible ? View.GONE : View.VISIBLE);
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView timeTextView;
        View mainTaskLayout;
        View taskActionsLayout;
        ImageButton editButton;
        ImageButton deleteButton;

        TaskViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.taskTitle);
            timeTextView = itemView.findViewById(R.id.taskTime);
            mainTaskLayout = itemView.findViewById(R.id.mainTaskLayout);
            taskActionsLayout = itemView.findViewById(R.id.taskActionsLayout);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
} 