package com.salvatore.gymapp.service;

import com.salvatore.gymapp.dto.auth.ManagerDashboardResponse;
import com.salvatore.gymapp.entity.User;
import com.salvatore.gymapp.exception.ForbiddenException;
import com.salvatore.gymapp.exception.NotFoundException;
import com.salvatore.gymapp.repository.ExerciseRepository;
import com.salvatore.gymapp.repository.UserRepository;
import com.salvatore.gymapp.repository.WorkoutPlanRepository;
import com.salvatore.gymapp.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManagerDashboardService {

    private final UserRepository userRepository;
    private final WorkoutPlanRepository workoutPlanRepository;
    private final ExerciseRepository exerciseRepository;
    private final SubscriptionService subscriptionService;

    public ManagerDashboardResponse getDashboard(CustomUserPrincipal currentUser) {
        User manager = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Utente non trovato"));

        if (manager.getGym() == null) {
            throw new ForbiddenException("Manager senza palestra associata");
        }

        Long gymId = manager.getGym().getId();

        List<User> gymUsers = userRepository.findAllByGymIdAndRole_Name(gymId, "USER");

        long totalUsers = gymUsers.size();

        long activeSubscriptionsCount = gymUsers.stream()
                .filter(user -> subscriptionService.hasValidSubscription(user.getId()))
                .count();

        long expiredUsersCount = totalUsers - activeSubscriptionsCount;

        long expiringUsersCount = gymUsers.stream()
                .filter(user -> {
                    LocalDate endDate = subscriptionService.getSubscriptionEndDate(user.getId());

                    if (endDate == null) {
                        return false;
                    }

                    LocalDate today = LocalDate.now();
                    return !endDate.isBefore(today) && !endDate.isAfter(today.plusDays(5));
                })
                .count();

        long usersWithActivePlan = workoutPlanRepository.countDistinctUsersWithActivePlanByGymId(gymId);
        long usersWithoutActivePlan = totalUsers - usersWithActivePlan;
        long totalExercises = exerciseRepository.countByGymId(gymId);

        return new ManagerDashboardResponse(
                gymId,
                manager.getGym().getName(),
                totalUsers,
                usersWithActivePlan,
                usersWithoutActivePlan,
                totalExercises,
                activeSubscriptionsCount,
                expiringUsersCount,
                expiredUsersCount
        );
    }
}