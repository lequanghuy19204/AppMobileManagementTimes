package com.example.appmobilemanagementtimes;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CalendarDayAdapter extends RecyclerView.Adapter<CalendarDayAdapter.CalendarDayViewHolder> {
    private List<Integer> days;
    private Context context;
    private int selectedDay = -1;
    private int currentMonth;
    private int currentYear;
    private OnDayClickListener listener;
    private Set<Integer> daysWithTasks = new HashSet<>();

    public interface OnDayClickListener {
        void onDayClick(int day);
    }

    public CalendarDayAdapter(Context context, List<Integer> days, int currentMonth, int currentYear) {
        this.context = context;
        this.days = days;
        this.currentMonth = currentMonth;
        this.currentYear = currentYear;
    }

    public void setOnDayClickListener(OnDayClickListener listener) {
        this.listener = listener;
    }

    public void setSelectedDay(int day) {
        this.selectedDay = day;
        notifyDataSetChanged();
    }

    public void setDaysWithTasks(Set<Integer> daysWithTasks) {
        this.daysWithTasks = daysWithTasks;
        Log.d("CalendarDayAdapter", "Ngày có nhiệm vụ: " + daysWithTasks.toString());
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CalendarDayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.calendar_day_item, parent, false);
        return new CalendarDayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarDayViewHolder holder, int position) {
        int day = days.get(position);
        
        if (day > 0) {
            holder.dayText.setText(String.valueOf(day));
            
            // Kiểm tra ngày hiện tại
            Calendar calendar = Calendar.getInstance();
            boolean isToday = day == calendar.get(Calendar.DAY_OF_MONTH) && 
                    currentMonth == calendar.get(Calendar.MONTH) && 
                    currentYear == calendar.get(Calendar.YEAR);
            
            // Kiểm tra ngày được chọn
            boolean isSelected = day == selectedDay;
            
            // Hiển thị chỉ báo cho ngày có nhiệm vụ
            if (daysWithTasks.contains(day)) {
                Log.d("CalendarDayAdapter", "Hiển thị dấu chấm cho ngày: " + day);
                holder.taskIndicator.setVisibility(View.VISIBLE);
            } else {
                holder.taskIndicator.setVisibility(View.INVISIBLE);
            }
            
            if (isSelected) {
                holder.dayText.setBackgroundResource(R.drawable.selected_day_background);
                holder.dayText.setTextColor(Color.WHITE);
            } else if (isToday) {
                holder.dayText.setBackgroundResource(R.drawable.today_background);
                holder.dayText.setTextColor(ContextCompat.getColor(context, R.color.orange_accent));
            } else {
                holder.dayText.setBackgroundResource(0);
                // Sử dụng màu từ thuộc tính theme thay vì màu cứng
                int textColorAttr = android.R.attr.textColorPrimary;
                int textColor = getThemeColor(textColorAttr);
                holder.dayText.setTextColor(textColor);
            }
            
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDayClick(day);
                }
                selectedDay = day;
                notifyDataSetChanged();
            });
        } else {
            holder.dayText.setText("");
            holder.dayText.setBackgroundResource(0);
            holder.taskIndicator.setVisibility(View.INVISIBLE);
            holder.itemView.setOnClickListener(null);
        }
    }

    // Phương thức lấy màu từ theme
    private int getThemeColor(int attr) {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        context.getTheme().resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    static class CalendarDayViewHolder extends RecyclerView.ViewHolder {
        TextView dayText;
        View taskIndicator;

        CalendarDayViewHolder(View itemView) {
            super(itemView);
            dayText = itemView.findViewById(R.id.dayText);
            taskIndicator = itemView.findViewById(R.id.taskIndicator);
        }
    }
} 