package com.salvatore.gymapp.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class UserWorkoutPlanResponse {
    private Long workoutPlanId;
    private String title;
    private List<UserWorkoutDayResponse> days;
}