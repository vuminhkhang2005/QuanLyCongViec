package com.todo.todolist.repository;

import com.todo.todolist.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByVerificationCode(String verificationCode);
    Optional<User> findByPasswordResetToken(String passwordResetToken);
    Optional<User> findByRefreshToken(String refreshToken);
}
