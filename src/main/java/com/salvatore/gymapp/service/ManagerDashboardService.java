package com.salvatore.gymapp.service;

import com.salvatore.gymapp.dto.auth.ManagerDashboardResponse;
import com.salvatore.gymapp.entity.gym.User;
import com.salvatore.gymapp.exception.ForbiddenException;
import com.salvatore.gymapp.exception.NotFoundException;
import com.salvatore.gymapp.repository.ExerciseRepository;
import com.salvatore.gymapp.repository.UserRepository;
import com.salvatore.gymapp.repository.WorkoutPlanRepository;
import com.salvatore.gymapp.security.CustomUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManagerDashboardService {

    private final UserRepository userRepository;
    private final WorkoutPlanRepository workoutPlanRepository;
    private final ExerciseRepository exerciseRepository;
    private final SubscriptionService subscriptionService;
    private final GymSubscriptionService gymSubscriptionService;

    public ManagerDashboardResponse getDashboard(CustomUserPrincipal currentUser) {

        log.info("=== START ManagerDashboardService.getDashboard ===");

        User manager = userRepository.findByIdWithGym(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Utente non trovato"));

        if (manager.getGym() == null) {
            throw new ForbiddenException("Manager senza palestra associata");
        }

        Long gymId = manager.getGym().getId();
        String gymName = manager.getGym().getName();

        List<User> gymUsers = userRepository.findAllByGymIdAndRole_Name(gymId, "USER");

        long totalUsers = gymUsers.size();

        long activeUsersCount = 0;
        long activeSubscriptionsCount = 0;
        long expiringUsersCount = 0;

        LocalDate today = LocalDate.now();
        LocalDate expiryThreshold = today.plusDays(5);

        for (User user : gymUsers) {
            boolean hasValidSubscription = subscriptionService.hasValidSubscription(user.getId());
            LocalDate endDate = subscriptionService.getSubscriptionEndDate(user.getId());

            boolean isEffectivelyActive = user.isActive() && hasValidSubscription;

            if (isEffectivelyActive) {
                activeUsersCount++;
            }

            if (hasValidSubscription) {
                activeSubscriptionsCount++;
            }

            if (endDate != null
                    && !endDate.isBefore(today.minusDays(2))
                    && !endDate.isAfter(expiryThreshold)) {
                expiringUsersCount++;
            }

            log.info(
                    "User {} -> isActive={}, validSubscription={}, effectiveActive={}, endDate={}",
                    user.getId(),
                    user.isActive(),
                    hasValidSubscription,
                    isEffectivelyActive,
                    endDate
            );
        }

        long expiredUsersCount = totalUsers - activeSubscriptionsCount;

        long usersWithActivePlan = workoutPlanRepository.countDistinctUsersWithActivePlanByGymId(gymId);
        long usersWithoutActivePlan = totalUsers - usersWithActivePlan;

        long totalExercises = exerciseRepository.countByGymId(gymId);

        LocalDate subscriptionEndDate = gymSubscriptionService.getSubscriptionEndDate(gymId);

        Integer maxUsers = manager.getGym().getMaxUsers();
        Integer availableSlots = maxUsers == null
                ? null
                : Math.max(maxUsers - Math.toIntExact(activeUsersCount), 0);

        log.info("GYM subscriptionEndDate={}", subscriptionEndDate);
        log.info("GYM maxUsers={}, activeUsersCount={}, availableSlots={}", maxUsers, activeUsersCount, availableSlots);

        return new ManagerDashboardResponse(
                gymId,
                gymName,
                totalUsers,
                activeUsersCount,
                usersWithActivePlan,
                usersWithoutActivePlan,
                totalExercises,
                activeSubscriptionsCount,
                expiringUsersCount,
                expiredUsersCount,
                subscriptionEndDate,
                maxUsers,
                availableSlots
        );
    }
}