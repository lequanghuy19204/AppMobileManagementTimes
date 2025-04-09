package com.example.appmobilemanagementtimes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class Taskadapter3 extends RecyclerView.Adapter<Taskadapter3.TaskViewHolder> {
    private List<Task> taskList;

    public Taskadapter3(List<Task> taskList) {
        this.taskList = taskList;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task_done, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.taskName.setText(task.getName());
        String time = task.getTime().substring(11); // Chỉ lấy phần giờ "HH:mm"
        holder.taskTime.setText(time);

        // Đặt checkbox luôn có dấu tích và không thể chỉnh sửa
        holder.checkbox.setChecked(true);
        holder.checkbox.setEnabled(false); // Đảm bảo không thể tương tác

        holder.btnDelete.setOnClickListener(v -> {

            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                taskList.remove(pos);
                notifyItemRemoved(pos);
            }
        });


    }


    @Override
    public int getItemCount() {
        return taskList.size();
    }

    public void updateTasks(List<Task> newTasks) {
        this.taskList = newTasks;
        notifyDataSetChanged();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView taskName, taskTime;
        CheckBox checkbox;
        ImageButton btnEdit, btnDelete;

        public TaskViewHolder(View itemView) {
            super(itemView);
            taskName = itemView.findViewById(R.id.task_name);
            taskTime = itemView.findViewById(R.id.task_time);
            checkbox = itemView.findViewById(R.id.checkbox_task);
            btnDelete = itemView.findViewById(R.id.btnDeleteTask);
        }
    }
}