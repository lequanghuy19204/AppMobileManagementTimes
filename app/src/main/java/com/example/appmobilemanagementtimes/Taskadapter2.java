package com.example.appmobilemanagementtimes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class Taskadapter2 extends RecyclerView.Adapter<Taskadapter2.TaskViewHolder> {
    private List<Task2> taskList;

    public Taskadapter2(List<Task2> taskList) {
        this.taskList = taskList;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task2 task = taskList.get(position);
        holder.taskName.setText(task.getName());
        holder.taskTime.setText(task.getTime());
    }

    @Override
    public int getItemCount() { return taskList.size(); }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView taskName, taskTime;

        public TaskViewHolder(View itemView) {
            super(itemView);
            taskName = itemView.findViewById(R.id.task_name);
            taskTime = itemView.findViewById(R.id.task_time);
        }
    }
}
