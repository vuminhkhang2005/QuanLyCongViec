package com.todo.todolist.service;

import com.todo.todolist.dto.CreateTaskRequest;
import com.todo.todolist.dto.TaskResponse;
import com.todo.todolist.dto.UpdateTaskRequest;
import com.todo.todolist.entity.Priority;
import com.todo.todolist.entity.Task;
import com.todo.todolist.exception.ResourceNotFoundException;
import com.todo.todolist.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponse> getAllTasks(Boolean completed, String search, Pageable pageable) {
        // Nếu search trống hoặc chỉ có khoảng trắng, chuyển về null để Query nhận diện
        String searchParam = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        return taskRepository.findAllWithFilters(completed, searchParam, pageable)
                .map(TaskResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        return TaskResponse.fromEntity(task);
    }

    @Override
    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        Priority priority = request.priority() != null ? request.priority() : Priority.MEDIUM;
        Task task = Task.builder()
                .title(request.title().trim())
                .description(request.description())
                .completed(false)
                .priority(priority)
                .dueDate(request.dueDate())
                .build();
        Task savedTask = taskRepository.save(task);
        return TaskResponse.fromEntity(savedTask);
    }

    @Override
    @Transactional
    public TaskResponse updateTask(Long id, UpdateTaskRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setCompleted(request.completed());
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());

        Task updatedTask = taskRepository.save(task);
        return TaskResponse.fromEntity(updatedTask);
    }

    @Override
    @Transactional
    public TaskResponse toggleTaskCompletion(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));

        task.setCompleted(!task.isCompleted());
        Task updatedTask = taskRepository.save(task);
        return TaskResponse.fromEntity(updatedTask);
    }

    @Override
    @Transactional
    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
        taskRepository.delete(task);
    }
}
