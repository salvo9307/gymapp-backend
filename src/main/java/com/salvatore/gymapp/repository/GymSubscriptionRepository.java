package com.salvatore.gymapp.repository;

import com.salvatore.gymapp.entity.GymSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface GymSubscriptionRepository extends JpaRepository<GymSubscription, Long> {

    Optional<GymSubscription> findByGym_IdAndActiveTrue(Long gymId);

    boolean existsByGym_IdAndActiveTrueAndEndDateGreaterThanEqual(Long gymId, LocalDate date);
}