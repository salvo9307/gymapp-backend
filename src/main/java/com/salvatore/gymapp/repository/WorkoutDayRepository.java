package com.salvatore.gymapp.repository;

import com.salvatore.gymapp.entity.WorkoutDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkoutDayRepository extends JpaRepository<WorkoutDay, Long> {

    List<WorkoutDay> findByWorkoutPlanIdOrderByDayOrderAsc(Long workoutPlanId);

}