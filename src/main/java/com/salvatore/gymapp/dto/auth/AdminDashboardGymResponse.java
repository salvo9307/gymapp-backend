package com.salvatore.gymapp.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class AdminDashboardGymResponse {

    private Long id;
    private String name;
    private String city;
    private long totalUsers;
    private long usersWithActivePlan;
    private long usersWithoutActivePlan;
    private long totalExercises;
    private boolean active;
    private Long managerId;
    private String managerFirstName;
    private String managerLastName;
    private String managerEmail;
    private LocalDate subscriptionEndDate;
    private Integer maxUsers;
    private long activeUsersCount;
    private Integer availableSlots;
}