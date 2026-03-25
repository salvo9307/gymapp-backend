package com.salvatore.gymapp.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserWorkoutExerciseResponse {
    private Long id;
    private Long exerciseId;
    private String exerciseName;
    private Integer exerciseOrder;
    private Integer sets;
    private String reps;
    private Double weight;
}