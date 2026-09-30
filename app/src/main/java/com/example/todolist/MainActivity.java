package com.example.todolist;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements TaskAdapter.OnTaskClickListener {

    private static final String PREFS_NAME = "todo_prefs";
    private static final String KEY_TASKS = "tasks_list";

    private final List<Task> taskList = new ArrayList<>();
    private TaskAdapter taskAdapter;

    private EditText etTaskInput;
    private Button btnAddTask;
    private RecyclerView rvTasks;
    private View emptyView;
    private TextView tvTaskSummary;

    private final Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        loadTasks();
        setupRecyclerView();
        setupListeners();
        updateUI();
    }

    private void initViews() {
        etTaskInput = findViewById(R.id.etTaskInput);
        btnAddTask = findViewById(R.id.btnAddTask);
        rvTasks = findViewById(R.id.rvTasks);
        emptyView = findViewById(R.id.emptyView);
        tvTaskSummary = findViewById(R.id.tvTaskSummary);
    }

    private void setupRecyclerView() {
        taskAdapter = new TaskAdapter(taskList, this);
        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        rvTasks.setAdapter(taskAdapter);
    }

    private void setupListeners() {
        btnAddTask.setOnClickListener(v -> addNewTask());

        // Permitir agregar tarea presionando enter/done en el teclado
        etTaskInput.setOnEditorActionListener((v, actionId, event) -> {
            addNewTask();
            return true;
        });
    }

    private void addNewTask() {
        String taskTitle = etTaskInput.getText() != null ? etTaskInput.getText().toString().trim() : "";

        if (taskTitle.isEmpty()) {
            Toast.makeText(this, R.string.empty_field_warning, Toast.LENGTH_SHORT).show();
            return;
        }

        Task newTask = new Task(taskTitle);
        taskList.add(0, newTask); // Insertar al inicio de la lista
        taskAdapter.notifyItemInserted(0);
        rvTasks.scrollToPosition(0);

        etTaskInput.setText("");
        hideKeyboard();
        saveTasks();
        updateUI();
    }

    @Override
    public void onTaskToggle(int position, boolean isCompleted) {
        if (position >= 0 && position < taskList.size()) {
            taskList.get(position).setCompleted(isCompleted);
            saveTasks();
            updateUI();
        }
    }

    @Override
    public void onTaskDelete(int position) {
        if (position < 0 || position >= taskList.size()) return;

        new AlertDialog.Builder(this)
                .setTitle(R.string.delete)
                .setMessage(R.string.delete_task_confirm)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    taskList.remove(position);
                    taskAdapter.notifyItemRemoved(position);
                    taskAdapter.notifyItemRangeChanged(position, taskList.size() - position);
                    saveTasks();
                    updateUI();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updateUI() {
        if (taskList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            rvTasks.setVisibility(View.GONE);
            tvTaskSummary.setText(R.string.empty_tasks);
        } else {
            emptyView.setVisibility(View.GONE);
            rvTasks.setVisibility(View.VISIBLE);

            long pendingCount = 0;
            for (Task task : taskList) {
                if (!task.isCompleted()) {
                    pendingCount++;
                }
            }

            if (pendingCount == 1) {
                tvTaskSummary.setText("1 tarea pendiente");
            } else {
                tvTaskSummary.setText(pendingCount + " tareas pendientes");
            }
        }
    }

    private void saveTasks() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String json = gson.toJson(taskList);
        editor.putString(KEY_TASKS, json);
        editor.apply();
    }

    private void loadTasks() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_TASKS, null);

        taskList.clear();
        if (json != null && !json.isEmpty()) {
            Type type = new TypeToken<ArrayList<Task>>() {}.getType();
            List<Task> savedTasks = gson.fromJson(json, type);
            if (savedTasks != null) {
                taskList.addAll(savedTasks);
            }
        }
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}
