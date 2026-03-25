package com.salvatore.gymapp.service;

import com.salvatore.gymapp.entity.Gym;
import com.salvatore.gymapp.entity.GymSubscription;
import com.salvatore.gymapp.repository.GymRepository;
import com.salvatore.gymapp.repository.GymSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class GymSubscriptionService {

    private final GymSubscriptionRepository gymSubscriptionRepository;
    private final GymRepository gymRepository;

    private static final int TOLERANCE_DAYS = 2;

    public void renewSubscription(Long gymId, int months) {

        Gym gym = gymRepository.findById(gymId)
                .orElseThrow(() -> new RuntimeException("Gym not found"));

        GymSubscription subscription = gymSubscriptionRepository
                .findByGym_IdAndActiveTrue(gymId)
                .orElse(null);

        LocalDate today = LocalDate.now();

        if (subscription == null) {
            subscription = new GymSubscription();
            subscription.setGym(gym);
            subscription.setStartDate(today);
            subscription.setEndDate(today.plusMonths(months));
            subscription.setActive(true);
        } else {
            LocalDate endDate = subscription.getEndDate();

            if (endDate != null && !endDate.isBefore(today.minusDays(TOLERANCE_DAYS))) {
                // 🔥 ancora valida → estendi
                subscription.setEndDate(endDate.plusMonths(months));
            } else {
                // 🔥 scaduta → riparti da oggi
                subscription.setStartDate(today);
                subscription.setEndDate(today.plusMonths(months));
            }
        }

        gymSubscriptionRepository.save(subscription);
    }

    public boolean hasValidSubscription(Long gymId) {
        LocalDate threshold = LocalDate.now().minusDays(TOLERANCE_DAYS);

        return gymSubscriptionRepository
                .existsByGym_IdAndActiveTrueAndEndDateGreaterThanEqual(gymId, threshold);
    }

    public LocalDate getSubscriptionEndDate(Long gymId) {
        return gymSubscriptionRepository
                .findByGym_IdAndActiveTrue(gymId)
                .map(GymSubscription::getEndDate)
                .orElse(null);
    }
}