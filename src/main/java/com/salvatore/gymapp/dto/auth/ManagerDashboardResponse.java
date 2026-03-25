package com.salvatore.gymapp.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ManagerDashboardResponse {

    private Long gymId;
    private String gymName;
    private long totalUsers;
    private long usersWithActivePlan;
    private long usersWithoutActivePlan;
    private long totalExercises;
    private long activeSubscriptionsCount;
    private long expiringUsersCount;
    private long expiredUsersCount;
}