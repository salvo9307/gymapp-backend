package com.salvatore.gymapp.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class WorkoutPlanResponse {
    private Long id;
    private String title;
    private List<WorkoutDayResponse> days;
    private LocalDate subscriptionEndDate;
}