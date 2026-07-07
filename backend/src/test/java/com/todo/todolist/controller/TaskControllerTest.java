package com.todo.todolist.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todo.todolist.dto.CreateTaskRequest;
import com.todo.todolist.dto.TaskResponse;
import com.todo.todolist.dto.UpdateTaskRequest;
import com.todo.todolist.entity.Priority;
import com.todo.todolist.exception.ResourceNotFoundException;
import com.todo.todolist.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.security.test.context.support.WithMockUser;

import org.springframework.context.annotation.Import;

@WebMvcTest(TaskController.class)
@WithMockUser
@Import(com.todo.todolist.security.SecurityConfig.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;

    @MockBean
    private com.todo.todolist.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.todo.todolist.security.CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private TaskResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = new TaskResponse(
                1L,
                "Sample Task",
                "Sample Description",
                false,
                Priority.MEDIUM,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    void getAllTasks_ShouldReturnPagedTasks() throws Exception {
        Page<TaskResponse> page = new PageImpl<>(List.of(sampleResponse));

        when(taskService.getAllTasks(any(), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/tasks")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "createdAt")
                        .param("sortDir", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[0].title", is("Sample Task")))
                .andExpect(jsonPath("$.content[0].completed", is(false)));

        verify(taskService, times(1)).getAllTasks(any(), any(), any(Pageable.class));
    }

    @Test
    void getTaskById_Success_ShouldReturnTask() throws Exception {
        when(taskService.getTaskById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Sample Task")));

        verify(taskService, times(1)).getTaskById(1L);
    }

    @Test
    void getTaskById_NotFound_ShouldReturn404() throws Exception {
        when(taskService.getTaskById(99L)).thenThrow(new ResourceNotFoundException("Task not found with id: 99"));

        mockMvc.perform(get("/api/tasks/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", is("Task not found with id: 99")));

        verify(taskService, times(1)).getTaskById(99L);
    }

    @Test
    void createTask_Success_ShouldReturn201() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest(
                "New Task",
                "New Description",
                Priority.HIGH,
                null
        );

        TaskResponse createdResponse = new TaskResponse(
                2L,
                "New Task",
                "New Description",
                false,
                Priority.HIGH,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(taskService.createTask(any(CreateTaskRequest.class))).thenReturn(createdResponse);

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(2)))
                .andExpect(jsonPath("$.title", is("New Task")))
                .andExpect(jsonPath("$.priority", is("HIGH")));

        verify(taskService, times(1)).createTask(any(CreateTaskRequest.class));
    }

    @Test
    void createTask_InvalidInput_ShouldReturn400() throws Exception {
        // Gửi request với title trống để kích hoạt validation error
        CreateTaskRequest request = new CreateTaskRequest(
                "",
                "Description",
                Priority.MEDIUM,
                null
        );

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.details.title", is("Title must not be blank")));

        verify(taskService, never()).createTask(any(CreateTaskRequest.class));
    }

    @Test
    void updateTask_Success_ShouldReturn200() throws Exception {
        UpdateTaskRequest request = new UpdateTaskRequest(
                "Updated Task",
                "Updated Description",
                true,
                Priority.LOW,
                null
        );

        TaskResponse updatedResponse = new TaskResponse(
                1L,
                "Updated Task",
                "Updated Description",
                true,
                Priority.LOW,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(taskService.updateTask(eq(1L), any(UpdateTaskRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/api/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.title", is("Updated Task")))
                .andExpect(jsonPath("$.completed", is(true)))
                .andExpect(jsonPath("$.priority", is("LOW")));

        verify(taskService, times(1)).updateTask(eq(1L), any(UpdateTaskRequest.class));
    }

    @Test
    void updateTask_InvalidInput_ShouldReturn400() throws Exception {
        UpdateTaskRequest request = new UpdateTaskRequest(
                "   ", // chỉ có dấu cách (blank)
                "Updated Description",
                true,
                Priority.LOW,
                null
        );

        mockMvc.perform(put("/api/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.details.title", is("Title must not be blank")));

        verify(taskService, never()).updateTask(anyLong(), any(UpdateTaskRequest.class));
    }

    @Test
    void toggleTaskCompletion_ShouldReturn200() throws Exception {
        TaskResponse toggledResponse = new TaskResponse(
                1L,
                "Sample Task",
                "Sample Description",
                true, // toggled from false to true
                Priority.MEDIUM,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(taskService.toggleTaskCompletion(1L)).thenReturn(toggledResponse);

        mockMvc.perform(patch("/api/tasks/1/toggle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.completed", is(true)));

        verify(taskService, times(1)).toggleTaskCompletion(1L);
    }

    @Test
    void deleteTask_ShouldReturn24NoContent() throws Exception {
        doNothing().when(taskService).deleteTask(1L);

        mockMvc.perform(delete("/api/tasks/1"))
                .andExpect(status().isNoContent());

        verify(taskService, times(1)).deleteTask(1L);
    }
}
