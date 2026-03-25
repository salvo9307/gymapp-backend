package com.salvatore.gymapp.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class UserDetailResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private Long gymId;
    private boolean active;

    private boolean hasWorkoutPlan;
    private Long latestWorkoutPlanId;
    private String latestWorkoutPlanTitle;
    private LocalDate subscriptionEndDate;
}