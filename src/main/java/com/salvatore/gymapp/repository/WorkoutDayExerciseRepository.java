package com.salvatore.gymapp.repository;

import com.salvatore.gymapp.entity.workout.WorkoutDayExercise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkoutDayExerciseRepository extends JpaRepository<WorkoutDayExercise, Long> {

    List<WorkoutDayExercise> findByWorkoutDayIdOrderByExerciseOrderAsc(Long workoutDayId);

    Optional<WorkoutDayExercise> findById(Long id);
}