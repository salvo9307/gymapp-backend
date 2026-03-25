package com.salvatore.gymapp.dto.app;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AppWorkoutDayResponse {
    private Long id;
    private Integer dayOrder;
    private String title;
    private List<AppWorkoutExerciseResponse> exercises;
}