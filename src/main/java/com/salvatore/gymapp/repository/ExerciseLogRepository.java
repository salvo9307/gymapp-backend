package com.salvatore.gymapp.repository;

import com.salvatore.gymapp.entity.exercise.ExerciseLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExerciseLogRepository extends JpaRepository<ExerciseLog, Long> {

    Optional<ExerciseLog> findByUserIdAndWorkoutDayExerciseId(Long userId, Long workoutDayExerciseId);

    boolean existsByUserIdAndWorkoutDayExerciseId(Long userId, Long workoutDayExerciseId);

    List<ExerciseLog> findByUserId(Long userId);
}