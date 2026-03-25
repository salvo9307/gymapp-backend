package com.salvatore.gymapp.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UserWorkoutDayResponse {
    private Long id;
    private Integer dayOrder;
    private String title;
    private List<UserWorkoutExerciseResponse> exercises;
}