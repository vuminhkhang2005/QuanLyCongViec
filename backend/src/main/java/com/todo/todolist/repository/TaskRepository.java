package com.todo.todolist.repository;

import com.todo.todolist.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query("SELECT t FROM Task t WHERE t.user.id = :userId AND " +
            "(:completed IS NULL OR t.completed = :completed) AND " +
            "(:search IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Task> findAllWithFilters(
            @Param("completed") Boolean completed,
            @Param("search") String search,
            @Param("userId") Long userId,
            Pageable pageable
    );

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("UPDATE Task t SET t.user = :user WHERE t.user IS NULL")
    void assignUnownedTasksToUser(@Param("user") com.todo.todolist.entity.User user);
}
