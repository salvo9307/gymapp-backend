package com.salvatore.gymapp.service;

import com.salvatore.gymapp.dto.auth.AdminDashboardGymResponse;
import com.salvatore.gymapp.dto.auth.AdminDashboardResponse;
import com.salvatore.gymapp.entity.gym.Gym;
import com.salvatore.gymapp.entity.gym.User;
import com.salvatore.gymapp.repository.ExerciseRepository;
import com.salvatore.gymapp.repository.GymRepository;
import com.salvatore.gymapp.repository.UserRepository;
import com.salvatore.gymapp.repository.WorkoutPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final GymRepository gymRepository;
    private final UserRepository userRepository;
    private final ExerciseRepository exerciseRepository;
    private final WorkoutPlanRepository workoutPlanRepository;
    private final GymSubscriptionService gymSubscriptionService;
    private final SubscriptionService subscriptionService;

    public AdminDashboardResponse getDashboard() {
        List<Gym> gyms = gymRepository.findAll();

        List<AdminDashboardGymResponse> gymResponses = gyms.stream()
                .map(this::buildGymResponse)
                .toList();

        long totalGyms = gymRepository.count();
        long totalManagers = userRepository.countByRole_Name("MANAGER");
        long totalUsers = userRepository.countByRole_Name("USER");
        long totalExercises = exerciseRepository.count();

        return new AdminDashboardResponse(
                totalGyms,
                totalManagers,
                totalUsers,
                totalExercises,
                gymResponses
        );
    }

    private AdminDashboardGymResponse buildGymResponse(Gym gym) {
        List<User> gymUsers = userRepository.findAllByGymIdAndRole_Name(gym.getId(), "USER");

        long totalUsers = gymUsers.size();
        long activeUsersCount = 0;

        for (User user : gymUsers) {
            boolean hasValidSubscription = subscriptionService.hasValidSubscription(user.getId());
            boolean isEffectivelyActive = user.isActive() && hasValidSubscription;

            if (isEffectivelyActive) {
                activeUsersCount++;
            }
        }

        long usersWithActivePlan = workoutPlanRepository.countByUserGymIdAndActiveTrue(gym.getId());
        long usersWithoutActivePlan = totalUsers - usersWithActivePlan;
        long totalExercises = exerciseRepository.countByGymId(gym.getId());

        User manager = userRepository.findFirstByGymIdAndRole_Name(gym.getId(), "MANAGER")
                .orElse(null);

        LocalDate subscriptionEndDate = gymSubscriptionService.getSubscriptionEndDate(gym.getId());
        boolean subscriptionActive = gymSubscriptionService.hasValidSubscription(gym.getId());

        Integer maxUsers = gym.getMaxUsers();
        Integer availableSlots = maxUsers == null
                ? null
                : Math.max(maxUsers - Math.toIntExact(activeUsersCount), 0);

        return new AdminDashboardGymResponse(
                gym.getId(),
                gym.getName(),
                gym.getCity(),
                totalUsers,
                usersWithActivePlan,
                usersWithoutActivePlan,
                totalExercises,
                subscriptionActive,
                manager != null ? manager.getId() : null,
                manager != null ? manager.getFirstName() : null,
                manager != null ? manager.getLastName() : null,
                manager != null ? manager.getEmailBackup() : null,
                subscriptionEndDate,
                maxUsers,
                activeUsersCount,
                availableSlots
        );
    }
}