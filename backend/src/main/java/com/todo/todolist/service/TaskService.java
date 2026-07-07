package com.todo.todolist.service;

import com.todo.todolist.dto.CreateTaskRequest;
import com.todo.todolist.dto.TaskResponse;
import com.todo.todolist.dto.UpdateTaskRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TaskService {
    Page<TaskResponse> getAllTasks(Boolean completed, String search, Pageable pageable);
    TaskResponse getTaskById(Long id);
    TaskResponse createTask(CreateTaskRequest request);
    TaskResponse updateTask(Long id, UpdateTaskRequest request);
    TaskResponse toggleTaskCompletion(Long id);
    void deleteTask(Long id);
}
