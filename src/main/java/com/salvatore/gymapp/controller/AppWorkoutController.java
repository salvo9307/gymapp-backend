package com.salvatore.gymapp.controller;

import com.salvatore.gymapp.dto.app.AppUpdateWeightRequest;
import com.salvatore.gymapp.dto.app.AppWorkoutPlanResponse;
import com.salvatore.gymapp.security.CustomUserPrincipal;
import com.salvatore.gymapp.service.AppWorkoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/app/me")
@RequiredArgsConstructor
public class AppWorkoutController {

    private final AppWorkoutService appWorkoutService;

    @GetMapping("/workout-plan")
    public ResponseEntity<AppWorkoutPlanResponse> getMyWorkoutPlan(
            @AuthenticationPrincipal CustomUserPrincipal currentUser
    ) {
        return ResponseEntity.ok(appWorkoutService.getMyWorkoutPlan(currentUser));
    }

    @PutMapping("/exercises/{workoutDayExerciseId}/weight")
    public ResponseEntity<Void> updateMyWeight(
            @PathVariable Long workoutDayExerciseId,
            @RequestBody AppUpdateWeightRequest request,
            @AuthenticationPrincipal CustomUserPrincipal currentUser
    ) {
        appWorkoutService.updateMyWeight(workoutDayExerciseId, request, currentUser);
        return ResponseEntity.ok().build();
    }
}