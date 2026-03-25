package com.salvatore.gymapp.dto.app;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AppWorkoutExerciseResponse {
    private Long id;
    private Long exerciseId;
    private String exerciseName;
    private Integer exerciseOrder;
    private Integer sets;
    private String reps;
    private Double lastWeight;
}