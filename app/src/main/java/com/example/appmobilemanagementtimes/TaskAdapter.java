package com.example.appmobilemanagementtimes;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    private List<Task> taskList;
    private Context context;

    public TaskAdapter(List<Task> taskList, Context context) {
        this.taskList = taskList;
        this.context = context;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, @SuppressLint("RecyclerView") int position) {
        Task task = taskList.get(position);
        holder.taskName.setText(task.getName());
        holder.taskDate.setText(task.getDate());
        holder.taskCheckbox.setChecked(task.isCompleted());

        holder.taskCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.setCompleted(isChecked);
        });

        // Xử lý khi bấm nút sửa Task - Chuyển sang MainActivity2


        // Xử lý khi bấm nút xóa Task
        holder.deleteTask.setOnClickListener(v -> {
            taskList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, taskList.size());
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView taskName, taskDate;
        CheckBox taskCheckbox;
        ImageButton editTask, deleteTask;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskName = itemView.findViewById(R.id.task_name);
            taskDate = itemView.findViewById(R.id.task_time);
            taskCheckbox = itemView.findViewById(R.id.checkbox_task);
            editTask = itemView.findViewById(R.id.btnEditTask);
            deleteTask = itemView.findViewById(R.id.btnDeleteTask);
        }
    }

    // Hiển thị dialog sửa Task (nếu vẫn muốn giữ)
    private void showEditTaskDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Edit Task");

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_add_task, null);
        EditText editTaskName = view.findViewById(R.id.edtTaskTitle);
        editTaskName.setText(taskList.get(position).getName());

        builder.setView(view);
        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = editTaskName.getText().toString();
            taskList.get(position).setName(newName);
            notifyItemChanged(position);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}