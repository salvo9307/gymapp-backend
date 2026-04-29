package com.salvatore.gymapp.service;

import com.salvatore.gymapp.entity.Subscription;
import com.salvatore.gymapp.entity.gym.User;
import com.salvatore.gymapp.repository.SubscriptionRepository;
import com.salvatore.gymapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    private static final int TOLERANCE_DAYS = 2;

    public void renewSubscription(Long userId, int months, LocalDate startDate) {

        if (months <= 0) {
            throw new IllegalArgumentException("Months must be greater than zero");
        }

        if (startDate == null) {
            throw new IllegalArgumentException("Start date is required");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Subscription subscription = subscriptionRepository
                .findByUser_IdAndActiveTrue(userId)
                .orElse(null);

        LocalDate threshold = LocalDate.now().minusDays(TOLERANCE_DAYS);

        if (subscription == null) {
            subscription = new Subscription();
            subscription.setUser(user);
            subscription.setStartDate(startDate);
            subscription.setEndDate(startDate.plusMonths(months));
            subscription.setActive(true);
        } else {
            LocalDate currentEndDate = subscription.getEndDate();

            LocalDate effectiveStartDate;

            if (currentEndDate != null && !currentEndDate.isBefore(threshold)) {
                // abbonamento ancora valido o in tolleranza
                // parte dalla data più avanti tra quella scelta e la scadenza attuale
                effectiveStartDate = startDate.isAfter(currentEndDate) ? startDate : currentEndDate;
            } else {
                // abbonamento scaduto
                effectiveStartDate = startDate;
            }

            subscription.setStartDate(effectiveStartDate);
            subscription.setEndDate(effectiveStartDate.plusMonths(months));
            subscription.setActive(true);
        }

        subscriptionRepository.save(subscription);
        updateUserActiveStatus(user, subscription.getEndDate());
    }

    public boolean hasValidSubscription(Long userId) {
        LocalDate threshold = LocalDate.now().minusDays(TOLERANCE_DAYS);

        return subscriptionRepository
                .existsByUser_IdAndActiveTrueAndEndDateGreaterThanEqual(userId, threshold);
    }

    public LocalDate getSubscriptionEndDate(Long userId) {
        return subscriptionRepository
                .findByUser_IdAndActiveTrue(userId)
                .map(Subscription::getEndDate)
                .orElse(null);
    }

    private void updateUserActiveStatus(User user, LocalDate subscriptionEndDate) {

        LocalDate threshold = LocalDate.now().minusDays(TOLERANCE_DAYS);

        boolean isActive = subscriptionEndDate != null &&
                !subscriptionEndDate.isBefore(threshold);

        user.setActive(isActive);

        userRepository.save(user);
    }
}