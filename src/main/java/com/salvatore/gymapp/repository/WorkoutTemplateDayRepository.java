package com.salvatore.gymapp.repository;

import com.salvatore.gymapp.entity.workout.WorkoutTemplateDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkoutTemplateDayRepository extends JpaRepository<WorkoutTemplateDay, Long> {
    List<WorkoutTemplateDay> findByTemplateIdOrderByDayOrderAsc(Long templateId);
}