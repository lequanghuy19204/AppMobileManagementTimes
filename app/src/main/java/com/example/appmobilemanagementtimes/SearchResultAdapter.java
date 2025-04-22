package com.example.appmobilemanagementtimes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {

    private List<Task2> tasks = new ArrayList<>();
    private OnTaskClickListener listener;

    public interface OnTaskClickListener {
        void onTaskClick(Task2 task);
    }

    public SearchResultAdapter(List<Task2> tasks, OnTaskClickListener listener) {
        this.tasks = tasks;
        this.listener = listener;
    }

    public void updateTasks(List<Task2> newTasks) {
        this.tasks = newTasks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.search_result_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Task2 task = tasks.get(position);
        holder.bindTask(task);
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView taskNameTextView;
        private TextView taskDateTextView;
        private TextView taskTimeTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            taskNameTextView = itemView.findViewById(R.id.taskNameTextView);
            taskDateTextView = itemView.findViewById(R.id.taskDateTextView);
            taskTimeTextView = itemView.findViewById(R.id.taskTimeTextView);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onTaskClick(tasks.get(position));
                }
            });
        }

        public void bindTask(Task2 task) {
            taskNameTextView.setText(task.getName());
            
            try {
                // Định dạng ngày
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                
                String startTimeStr = task.getStartTime();
                String endTimeStr = task.getEndTime();
                
                if (startTimeStr != null && !startTimeStr.isEmpty()) {
                    Date startDate = inputFormat.parse(startTimeStr);
                    taskDateTextView.setText("Ngày: " + dateFormat.format(startDate));
                    
                    if (endTimeStr != null && !endTimeStr.isEmpty()) {
                        Date endDate = inputFormat.parse(endTimeStr);
                        taskTimeTextView.setText("Thời gian: " + timeFormat.format(startDate) + " - " + timeFormat.format(endDate));
                    } else {
                        taskTimeTextView.setText("Thời gian: " + timeFormat.format(startDate));
                    }
                }
            } catch (ParseException e) {
                taskDateTextView.setText("Ngày: không xác định");
                taskTimeTextView.setText("Thời gian: không xác định");
            }
        }
    }
} 