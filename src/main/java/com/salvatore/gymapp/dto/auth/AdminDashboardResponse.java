package com.salvatore.gymapp.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AdminDashboardResponse {
    private long totalGyms;
    private long totalManagers;
    private long totalUsers;
    private long totalExercises;
    private List<AdminDashboardGymResponse> gyms;
}