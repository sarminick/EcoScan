package com.example.todolist;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    public interface OnTaskClickListener {
        void onTaskToggle(int position, boolean isCompleted);
        void onTaskDelete(int position);
    }

    private final List<Task> taskList;
    private final OnTaskClickListener listener;

    public TaskAdapter(List<Task> taskList, OnTaskClickListener listener) {
        this.taskList = taskList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.bind(task, listener);
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCheckBox cbCompleted;
        private final TextView tvTaskTitle;
        private final ImageButton btnDelete;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            cbCompleted = itemView.findViewById(R.id.cbCompleted);
            tvTaskTitle = itemView.findViewById(R.id.tvTaskTitle);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        public void bind(Task task, OnTaskClickListener listener) {
            tvTaskTitle.setText(task.getTitle());

            // Evitar disparar listener al setear el valor programáticamente
            cbCompleted.setOnCheckedChangeListener(null);
            cbCompleted.setChecked(task.isCompleted());

            updateStrikeThrough(task.isCompleted());

            cbCompleted.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onTaskToggle(pos, isChecked);
                    updateStrikeThrough(isChecked);
                }
            });

            btnDelete.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onTaskDelete(pos);
                }
            });
        }

        private void updateStrikeThrough(boolean isCompleted) {
            if (isCompleted) {
                tvTaskTitle.setPaintFlags(tvTaskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                tvTaskTitle.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_secondary));
            } else {
                tvTaskTitle.setPaintFlags(tvTaskTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                tvTaskTitle.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_primary));
            }
        }
    }
}
