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

    public ManagerDashboardResponse getDashboard(CustomUserPrincipal currentUser) {
        log.info("=== START ManagerDashboardService.getDashboard ===");
        log.info("currentUser id={}, email={}, role={}",
                currentUser.getId(),
                currentUser.getUsername(),
                currentUser.getRole());

        User manager = userRepository.findByIdWithGym(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Utente non trovato"));

        log.info("Manager trovato: id={}, email={}", manager.getId(), manager.getEmail());

        if (manager.getGym() == null) {
            log.error("Manager senza palestra associata. managerId={}", manager.getId());
            throw new ForbiddenException("Manager senza palestra associata");
        }

        Long gymId = manager.getGym().getId();
        String gymName = manager.getGym().getName();

        log.info("Palestra manager: gymId={}, gymName={}", gymId, gymName);

        List<User> gymUsers = userRepository.findAllByGymIdAndRole_Name(gymId, "USER");
        log.info("Utenti palestra trovati: {}", gymUsers.size());

        long totalUsers = gymUsers.size();

        long activeSubscriptionsCount = 0;
        long expiringUsersCount = 0;

        LocalDate today = LocalDate.now();
        LocalDate expiryThreshold = today.plusDays(5);

        for (User user : gymUsers) {
            try {
                boolean hasValidSubscription = subscriptionService.hasValidSubscription(user.getId());

                if (hasValidSubscription) {
                    activeSubscriptionsCount++;
                }

                LocalDate endDate = subscriptionService.getSubscriptionEndDate(user.getId());

                if (endDate != null && !endDate.isBefore(today) && !endDate.isAfter(expiryThreshold)) {
                    expiringUsersCount++;
                }

                log.info("Utente id={} | validSubscription={} | endDate={}",
                        user.getId(),
                        hasValidSubscription,
                        endDate);

            } catch (Exception e) {
                log.error("Errore durante il calcolo subscription per userId={}", user.getId(), e);
                throw e;
            }
        }

        long expiredUsersCount = totalUsers - activeSubscriptionsCount;
        long usersWithActivePlan = workoutPlanRepository.countDistinctUsersWithActivePlanByGymId(gymId);
        long usersWithoutActivePlan = totalUsers - usersWithActivePlan;
        long totalExercises = exerciseRepository.countByGymId(gymId);

        log.info("activeSubscriptionsCount={}", activeSubscriptionsCount);
        log.info("expiringUsersCount={}", expiringUsersCount);
        log.info("expiredUsersCount={}", expiredUsersCount);
        log.info("usersWithActivePlan={}", usersWithActivePlan);
        log.info("usersWithoutActivePlan={}", usersWithoutActivePlan);
        log.info("totalExercises={}", totalExercises);

        ManagerDashboardResponse response = new ManagerDashboardResponse(
                gymId,
                gymName,
                totalUsers,
                usersWithActivePlan,
                usersWithoutActivePlan,
                totalExercises,
                activeSubscriptionsCount,
                expiringUsersCount,
                expiredUsersCount
        );

        log.info("Response dashboard costruita correttamente per gymId={}", gymId);
        log.info("=== END ManagerDashboardService.getDashboard ===");

        return response;
    }
}