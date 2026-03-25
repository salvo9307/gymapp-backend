package com.salvatore.gymapp.repository;

import com.salvatore.gymapp.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    boolean existsByUserIdAndActiveTrueAndEndDateAfter(Long userId, LocalDate date);

    Optional<Subscription> findByUser_IdAndActiveTrue(Long userId);

    boolean existsByUser_IdAndActiveTrueAndEndDateGreaterThanEqual(Long userId, LocalDate date);

}