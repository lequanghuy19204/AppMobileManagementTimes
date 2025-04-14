package com.example.appmobilemanagementtimes;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class Taskadapter3 extends RecyclerView.Adapter<Taskadapter3.TaskViewHolder> {
    private List<Task2> taskList;
    private static final String TAG = "Taskadapter3";

    public Taskadapter3(List<Task2> taskList) {
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

        holder.checkbox.setChecked(true);
        holder.checkbox.setEnabled(false);

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

    public void updateTasks(List<Task2> newTasks) {
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