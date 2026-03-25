package com.salvatore.gymapp.controller;

import com.salvatore.gymapp.dto.auth.UpdateWeightRequest;
import com.salvatore.gymapp.dto.auth.WorkoutPlanResponse;
import com.salvatore.gymapp.security.CustomUserPrincipal;
import com.salvatore.gymapp.service.WorkoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final WorkoutService workoutService;

    @GetMapping("/me/workout-plan")
    public WorkoutPlanResponse getMyWorkoutPlan(@AuthenticationPrincipal CustomUserPrincipal currentUser) {
        return workoutService.getMyLatestWorkoutPlan(currentUser);
    }

    @PutMapping("/me/exercises/{workoutDayExerciseId}/weight")
    public void updateMyWeight(@PathVariable Long workoutDayExerciseId,
                               @Valid @RequestBody UpdateWeightRequest request,
                               @AuthenticationPrincipal CustomUserPrincipal currentUser) {
        workoutService.updateMyWeight(workoutDayExerciseId, request.getWeight(), currentUser);
    }
}