package com.salvatore.gymapp.repository;

import com.salvatore.gymapp.entity.workout.WorkoutTemplateExercise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkoutTemplateExerciseRepository extends JpaRepository<WorkoutTemplateExercise, Long> {

    List<WorkoutTemplateExercise> findByTemplateDayIdOrderByExerciseOrderAsc(Long templateDayId);

    void deleteByTemplateDayId(Long templateDayId);
}