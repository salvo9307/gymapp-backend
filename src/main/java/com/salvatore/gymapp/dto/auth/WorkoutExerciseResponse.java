package com.salvatore.gymapp.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class WorkoutExerciseResponse {
    private Long workoutDayExerciseId;
    private String exerciseName;
    private Integer exerciseOrder;
    private Integer sets;
    private String reps;
    private BigDecimal weight;
}