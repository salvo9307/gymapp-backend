package com.salvatore.gymapp.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class WorkoutTemplateExerciseResponse {
    private Long id;
    private Long exerciseId;
    private String exerciseName;
    private Integer exerciseOrder;
    private Integer sets;
    private String reps;
    private Integer restSeconds;
}