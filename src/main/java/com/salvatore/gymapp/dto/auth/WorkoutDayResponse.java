package com.salvatore.gymapp.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class WorkoutDayResponse {
    private Long id;
    private Integer dayOrder;
    private String title;
    private List<WorkoutExerciseResponse> exercises;
}