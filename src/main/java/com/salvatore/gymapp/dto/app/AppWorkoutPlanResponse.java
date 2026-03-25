package com.salvatore.gymapp.dto.app;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AppWorkoutPlanResponse {
    private Long id;
    private String title;
    private List<AppWorkoutDayResponse> days;
}