package com.salvatore.gymapp.controller;

import com.salvatore.gymapp.dto.auth.*;
import com.salvatore.gymapp.entity.workout.WorkoutPlan;
import com.salvatore.gymapp.security.CustomUserPrincipal;
import com.salvatore.gymapp.service.ManagerDashboardService;
import com.salvatore.gymapp.service.SubscriptionService;
import com.salvatore.gymapp.service.UserService;
import com.salvatore.gymapp.service.WorkoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
public class ManagerController {

    private final WorkoutService workoutService;
    private final UserService userService;
    private final ManagerDashboardService managerDashboardService;
    private final SubscriptionService subscriptionService;

    @PostMapping("/workout-plans")
    public Long createWorkoutPlan(@Valid @RequestBody CreateWorkoutPlanRequest request,
                                  @AuthenticationPrincipal CustomUserPrincipal currentUser) {
        WorkoutPlan plan = workoutService.createWorkoutPlan(request, currentUser);
        return plan.getId();
    }

    @GetMapping("/users/{userId}/workout-plan")
    public WorkoutPlanResponse getUserWorkoutPlan(@PathVariable Long userId,
                                                  @AuthenticationPrincipal CustomUserPrincipal currentUser) {
        return workoutService.getWorkoutPlanForUser(userId, currentUser);
    }

    @GetMapping("/users")
    public List<UserSummaryResponse> getMyGymUsers(@AuthenticationPrincipal CustomUserPrincipal currentUser) {
        return userService.getUsersForManager(currentUser);
    }

    @GetMapping("/users/{userId}")
    public UserDetailResponse getUserDetail(@PathVariable Long userId,
                                            @AuthenticationPrincipal CustomUserPrincipal currentUser) {
        return userService.getUserDetailForManager(userId, currentUser);
    }

    @PutMapping("/workout-plans/{workoutPlanId}")
    public Long updateWorkoutPlan(@PathVariable Long workoutPlanId,
                                  @Valid @RequestBody CreateWorkoutPlanRequest request,
                                  @AuthenticationPrincipal CustomUserPrincipal currentUser) {
        WorkoutPlan plan = workoutService.updateWorkoutPlan(workoutPlanId, request, currentUser);
        return plan.getId();
    }

    @GetMapping("/dashboard")
    public ManagerDashboardResponse getDashboard(
            @AuthenticationPrincipal CustomUserPrincipal currentUser) {
        return managerDashboardService.getDashboard(currentUser);
    }

    @GetMapping("/users/search")
    public PagedResponse<UserSummaryResponse> searchUsers(
            @AuthenticationPrincipal CustomUserPrincipal currentUser,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return userService.searchUsersForManager(currentUser, search, page, size);
    }

    @PostMapping("/users")
    public UserDetailResponse createUserForManager(
            @Valid @RequestBody CreateManagedUserRequest request,
            @AuthenticationPrincipal CustomUserPrincipal currentUser) {
        return userService.createUserForManager(request, currentUser);
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId,
                                           @AuthenticationPrincipal CustomUserPrincipal currentUser) {
        userService.deleteUserForManager(userId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/{userId}/reset-password")
    public void resetUserPassword(@PathVariable Long userId,
                                  @Valid @RequestBody ResetUserPasswordRequest request,
                                  @AuthenticationPrincipal CustomUserPrincipal currentUser) {
        userService.resetUserPasswordForManager(userId, request, currentUser);
    }

    @PostMapping("/workout-plans/{workoutPlanId}/duplicate")
    public ResponseEntity<WorkoutPlanResponse> duplicateWorkoutPlan(
            @PathVariable Long workoutPlanId,
            Authentication authentication
    ) {
        CustomUserPrincipal currentUser = (CustomUserPrincipal) authentication.getPrincipal();
        WorkoutPlanResponse response = workoutService.duplicateWorkoutPlan(workoutPlanId, currentUser);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/users/{userId}/status")
    public void updateUserStatus(@PathVariable Long userId,
                                 @Valid @RequestBody UpdateUserStatusRequest request,
                                 @AuthenticationPrincipal CustomUserPrincipal currentUser) {
        userService.updateUserStatusForManager(userId, request, currentUser);
    }

    @PutMapping("/users/{id}/renew-subscription")
    public ResponseEntity<Void> renewSubscription(
            @PathVariable Long id,
            @Valid @RequestBody RenewSubscriptionRequest request) {

        subscriptionService.renewSubscription(id, request.getMonths(), request.getStartDate());
        return ResponseEntity.ok().build();
    }
}