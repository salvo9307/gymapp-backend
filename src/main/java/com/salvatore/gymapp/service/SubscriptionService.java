package com.salvatore.gymapp.service;

import com.salvatore.gymapp.entity.Subscription;
import com.salvatore.gymapp.entity.User;
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

    public void renewSubscription(Long userId, int months) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Subscription subscription = subscriptionRepository
                .findByUser_IdAndActiveTrue(userId)
                .orElse(null);

        LocalDate today = LocalDate.now();

        if (subscription == null) {
            subscription = new Subscription();
            subscription.setUser(user);
            subscription.setStartDate(today);
            subscription.setEndDate(today.plusMonths(months));
            subscription.setActive(true);
        } else {
            LocalDate currentEndDate = subscription.getEndDate();

            if (currentEndDate != null && !currentEndDate.isBefore(today.minusDays(TOLERANCE_DAYS))) {
                // ancora valido → aggiungi mesi
                subscription.setEndDate(currentEndDate.plusMonths(months));
            } else {
                // scaduto → riparti da oggi
                subscription.setStartDate(today);
                subscription.setEndDate(today.plusMonths(months));
            }
        }

        subscriptionRepository.save(subscription);
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
}