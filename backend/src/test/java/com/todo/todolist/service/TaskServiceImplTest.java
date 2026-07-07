package com.todo.todolist.service;

import com.todo.todolist.dto.CreateTaskRequest;
import com.todo.todolist.dto.TaskResponse;
import com.todo.todolist.dto.UpdateTaskRequest;
import com.todo.todolist.entity.Priority;
import com.todo.todolist.entity.Task;
import com.todo.todolist.exception.ResourceNotFoundException;
import com.todo.todolist.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskServiceImpl taskService;

    private Task sampleTask;

    @BeforeEach
    void setUp() {
        sampleTask = Task.builder()
                .id(1L)
                .title("Sample Task")
                .description("Sample Description")
                .completed(false)
                .priority(Priority.MEDIUM)
                .dueDate(LocalDateTime.now().plusDays(1))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getAllTasks_ShouldReturnPagedTasks() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Task> page = new PageImpl<>(List.of(sampleTask));

        when(taskRepository.findAllWithFilters(any(), any(), any(Pageable.class))).thenReturn(page);

        Page<TaskResponse> result = taskService.getAllTasks(null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Sample Task", result.getContent().get(0).title());
        verify(taskRepository, times(1)).findAllWithFilters(any(), any(), any(Pageable.class));
    }

    @Test
    void getTaskById_Success_ShouldReturnTask() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));

        TaskResponse result = taskService.getTaskById(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Sample Task", result.title());
        verify(taskRepository, times(1)).findById(1L);
    }

    @Test
    void getTaskById_NotFound_ShouldThrowException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.getTaskById(99L));
        verify(taskRepository, times(1)).findById(99L);
    }

    @Test
    void createTask_ShouldSaveAndReturnTask() {
        CreateTaskRequest request = new CreateTaskRequest(
                "New Task",
                "New Description",
                Priority.HIGH,
                LocalDateTime.now().plusDays(2)
        );

        Task savedTask = Task.builder()
                .id(2L)
                .title(request.title())
                .description(request.description())
                .completed(false)
                .priority(request.priority())
                .dueDate(request.dueDate())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        TaskResponse result = taskService.createTask(request);

        assertNotNull(result);
        assertEquals(2L, result.id());
        assertEquals("New Task", result.title());
        assertEquals(Priority.HIGH, result.priority());
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void updateTask_Success_ShouldUpdateAndReturnTask() {
        UpdateTaskRequest request = new UpdateTaskRequest(
                "Updated Task",
                "Updated Description",
                true,
                Priority.LOW,
                LocalDateTime.now().plusDays(3)
        );

        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any(Task.class))).thenReturn(sampleTask);

        TaskResponse result = taskService.updateTask(1L, request);

        assertNotNull(result);
        assertEquals("Updated Task", sampleTask.getTitle());
        assertTrue(sampleTask.isCompleted());
        assertEquals(Priority.LOW, sampleTask.getPriority());
        verify(taskRepository, times(1)).findById(1L);
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void updateTask_NotFound_ShouldThrowException() {
        UpdateTaskRequest request = new UpdateTaskRequest(
                "Updated Task",
                "Updated Description",
                true,
                Priority.LOW,
                null
        );

        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.updateTask(99L, request));
        verify(taskRepository, times(1)).findById(99L);
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void toggleTaskCompletion_Success_ShouldToggleStatus() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
        when(taskRepository.save(any(Task.class))).thenReturn(sampleTask);

        assertFalse(sampleTask.isCompleted());

        TaskResponse result = taskService.toggleTaskCompletion(1L);

        assertNotNull(result);
        assertTrue(sampleTask.isCompleted());
        verify(taskRepository, times(1)).findById(1L);
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void deleteTask_Success_ShouldDeleteTask() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
        doNothing().when(taskRepository).delete(sampleTask);

        taskService.deleteTask(1L);

        verify(taskRepository, times(1)).findById(1L);
        verify(taskRepository, times(1)).delete(sampleTask);
    }

    @Test
    void deleteTask_NotFound_ShouldThrowException() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.deleteTask(99L));
        verify(taskRepository, times(1)).findById(99L);
        verify(taskRepository, never()).delete(any(Task.class));
    }
}
