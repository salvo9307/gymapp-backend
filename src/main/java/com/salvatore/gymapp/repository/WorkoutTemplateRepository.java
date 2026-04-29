package com.salvatore.gymapp.repository;

import com.salvatore.gymapp.entity.workout.WorkoutTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkoutTemplateRepository extends JpaRepository<WorkoutTemplate, Long> {
    List<WorkoutTemplate> findByGymId(Long gymId);
    List<WorkoutTemplate> findByGymIdOrderByTitleAsc(Long gymId);
}