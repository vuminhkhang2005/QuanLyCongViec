package com.todo.todolist.seeder;

import com.todo.todolist.entity.Priority;
import com.todo.todolist.entity.Task;
import com.todo.todolist.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final TaskRepository taskRepository;

    @Override
    public void run(String... args) {
        if (taskRepository.count() == 0) {
            log.info("Database is empty. Seeding initial task data...");

            Task task1 = Task.builder()
                    .title("Học Spring Boot cơ bản")
                    .description("Tìm hiểu về Spring IoC, DI, JPA, Hibernate và Spring Boot Auto Configuration")
                    .completed(true)
                    .priority(Priority.HIGH)
                    .dueDate(LocalDateTime.now().minusDays(2))
                    .build();

            Task task2 = Task.builder()
                    .title("Xây dựng RESTful API Todo List")
                    .description("Thiết kế và cài đặt các endpoint CRUD cho ứng dụng Todo List tuân thủ chuẩn REST")
                    .completed(false)
                    .priority(Priority.HIGH)
                    .dueDate(LocalDateTime.now().plusDays(1))
                    .build();

            Task task3 = Task.builder()
                    .title("Viết Unit Tests cho dự án")
                    .description("Sử dụng Mockito và MockMvc để viết kiểm thử cho Service và Controller")
                    .completed(false)
                    .priority(Priority.MEDIUM)
                    .dueDate(LocalDateTime.now().plusDays(2))
                    .build();

            Task task4 = Task.builder()
                    .title("Tìm hiểu Docker và Containerize")
                    .description("Viết Dockerfile và cấu hình docker-compose.yml kết nối MySQL với Spring Boot")
                    .completed(false)
                    .priority(Priority.LOW)
                    .dueDate(LocalDateTime.now().plusDays(5))
                    .build();

            Task task5 = Task.builder()
                    .title("Mua thực phẩm tuần mới")
                    .description("Mua rau củ quả, sữa, trứng, thịt gà và cá hồi cho tuần này")
                    .completed(true)
                    .priority(Priority.MEDIUM)
                    .dueDate(LocalDateTime.now().minusDays(1))
                    .build();

            taskRepository.saveAll(List.of(task1, task2, task3, task4, task5));
            log.info("Successfully seeded 5 tasks into database.");
        } else {
            log.info("Database already contains task data. Skipping seeder.");
        }
    }
}
