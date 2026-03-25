package com.salvatore.gymapp.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class UserSummaryResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Long gymId;
    private boolean active;

    private boolean hasWorkoutPlan;
    private Long latestWorkoutPlanId;
    private String latestWorkoutPlanTitle;
    private LocalDate subscriptionEndDate;
    private String subscriptionStatus;
}